package com.alice.smartteacher.service;

import com.alice.smartteacher.model.ApprovalLog;
import com.alice.smartteacher.model.DocumentVersion;
import com.alice.smartteacher.model.Enums;
import com.alice.smartteacher.model.SchoolDocument;
import com.alice.smartteacher.model.UserAccount;
import com.alice.smartteacher.repo.ApprovalLogRepository;
import com.alice.smartteacher.repo.DocumentRepository;
import com.alice.smartteacher.repo.DocumentVersionRepository;
import com.alice.smartteacher.web.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DocumentService {

    private final DocumentRepository documents;
    private final DocumentVersionRepository versions;
    private final ApprovalLogRepository approvals;
    private final CurrentUserService current;
    private final FileStore files;
    private final TextExtractor text;
    private final GeminiService gemini;
    private final NotificationService notifications;
    private final AuditService audit;
    private final SettingService settings;
    private final ObjectMapper mapper = new ObjectMapper();

    public DocumentService(DocumentRepository documents, DocumentVersionRepository versions, ApprovalLogRepository approvals,
                           CurrentUserService current, FileStore files, TextExtractor text, GeminiService gemini,
                           NotificationService notifications, AuditService audit, SettingService settings) {
        this.documents = documents;
        this.versions = versions;
        this.approvals = approvals;
        this.current = current;
        this.files = files;
        this.text = text;
        this.gemini = gemini;
        this.notifications = notifications;
        this.audit = audit;
        this.settings = settings;
    }

    public List<Map<String, Object>> list(String q, String category, String status, String track) {
        UserAccount me = current.get();
        List<SchoolDocument> source = me.getRole() == Enums.Role.TEACHER
                ? documents.findByOwnerIdOrderByUpdatedAtDesc(me.getId())
                : documents.findAllByOrderByUpdatedAtDesc();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SchoolDocument doc : source) {
            if (!match(doc, q, category, status, track)) {
                continue;
            }
            rows.add(Views.document(doc));
        }
        return rows;
    }

    public Map<String, Object> get(Long id) {
        SchoolDocument doc = loadVisible(id);
        Map<String, Object> view = Views.document(doc);
        List<Map<String, Object>> history = new ArrayList<>();
        for (DocumentVersion version : versions.findByDocumentIdOrderByVersionNumberDesc(doc.getId())) {
            history.add(Views.version(version));
        }
        List<Map<String, Object>> timeline = new ArrayList<>();
        for (ApprovalLog log : approvals.findByDocumentIdOrderByCreatedAtDesc(doc.getId())) {
            timeline.add(Views.approval(log));
        }
        view.put("versions", history);
        view.put("approvals", timeline);
        view.put("analysis", parseAnalysis(doc.getAnalysisJson()));
        return view;
    }

    @Transactional
    public Map<String, Object> create(String title, String category, String track, MultipartFile file, HttpServletRequest request) {
        UserAccount me = current.get();
        requireUpload(me);
        if (title == null || title.isBlank()) {
            throw new ApiException(400, "Give the document a title.");
        }
        FileStore.Stored stored = files.save(file);
        SchoolDocument doc = new SchoolDocument();
        doc.setTitle(title.trim());
        doc.setCategory(parseCategory(category));
        doc.setTrack(parseTrack(track));
        doc.setStatus(Enums.DocStatus.DRAFT);
        doc.setOwner(me);
        doc.setFilePath(stored.storedName);
        doc.setOriginalName(stored.originalName);
        doc.setContentType(stored.contentType);
        doc.setVersion(1);
        doc.setRoutedTo("Head of Studies");
        doc.setUpdatedAt(Instant.now());
        documents.save(doc);
        addVersion(doc, "Uploaded");
        audit.record(me, "DOCUMENT_UPLOAD", doc.getTitle(), request);
        return Views.document(doc);
    }

    @Transactional
    public Map<String, Object> replace(Long id, MultipartFile file, String note, HttpServletRequest request) {
        UserAccount me = current.get();
        SchoolDocument doc = loadVisible(id);
        if (me.getRole() == Enums.Role.TEACHER && !doc.getOwner().getId().equals(me.getId())) {
            throw new ApiException(403, "You can only replace your own file.");
        }
        if (doc.getStatus() == Enums.DocStatus.APPROVED || doc.getStatus() == Enums.DocStatus.UNDER_REVIEW) {
            throw new ApiException(400, "This document is locked while it is approved or under review.");
        }
        FileStore.Stored stored = files.save(file);
        doc.setVersion(doc.getVersion() + 1);
        doc.setFilePath(stored.storedName);
        doc.setOriginalName(stored.originalName);
        doc.setContentType(stored.contentType);
        doc.setStatus(Enums.DocStatus.DRAFT);
        doc.setUpdatedAt(Instant.now());
        documents.save(doc);
        addVersion(doc, note == null || note.isBlank() ? "New version" : note);
        audit.record(me, "DOCUMENT_VERSION", doc.getTitle() + " v" + doc.getVersion(), request);
        return get(id);
    }

    @Transactional
    public Map<String, Object> update(Long id, String title, String category, String track) {
        UserAccount me = current.get();
        SchoolDocument doc = loadVisible(id);
        if (me.getRole() == Enums.Role.TEACHER && !doc.getOwner().getId().equals(me.getId())) {
            throw new ApiException(403, "You can only edit your own document.");
        }
        if (title != null && !title.isBlank()) {
            doc.setTitle(title.trim());
        }
        if (category != null && !category.isBlank()) {
            doc.setCategory(parseCategory(category));
        }
        if (track != null && !track.isBlank()) {
            doc.setTrack(parseTrack(track));
        }
        doc.setUpdatedAt(Instant.now());
        documents.save(doc);
        return Views.document(doc);
    }

    @Transactional
    public Map<String, Object> submit(Long id, HttpServletRequest request) {
        UserAccount me = current.get();
        SchoolDocument doc = loadVisible(id);
        if (me.getRole() == Enums.Role.TEACHER && !doc.getOwner().getId().equals(me.getId())) {
            throw new ApiException(403, "You can only submit your own document.");
        }
        if (doc.getStatus() != Enums.DocStatus.DRAFT && doc.getStatus() != Enums.DocStatus.RETURNED && doc.getStatus() != Enums.DocStatus.REJECTED) {
            throw new ApiException(400, "Only a draft, returned, or rejected document can be submitted.");
        }
        doc.setStatus(Enums.DocStatus.SUBMITTED);
        doc.setRoutedTo("Head of Studies");
        doc.setUpdatedAt(Instant.now());
        documents.save(doc);
        notifications.notifyRole(Enums.Role.HEAD_OF_STUDIES, "Document to review",
                doc.getOwner().getFullName() + " submitted " + doc.getTitle() + ".", "APPROVAL", Enums.Priority.HIGH);
        notifications.notifyRole(Enums.Role.ADMIN, "Document submitted",
                doc.getTitle() + " is waiting for the Head of Studies.", "DOCUMENT", Enums.Priority.NORMAL);
        audit.record(me, "DOCUMENT_SUBMIT", doc.getTitle(), request);
        return Views.document(doc);
    }

    @Transactional
    public int bulk(List<Long> ids, String action, HttpServletRequest request) {
        if (ids == null) {
            return 0;
        }
        int count = 0;
        for (Long id : ids) {
            if ("delete".equalsIgnoreCase(action)) {
                delete(id, request);
                count++;
            } else if ("submit".equalsIgnoreCase(action)) {
                submit(id, request);
                count++;
            }
        }
        return count;
    }

    @Transactional
    public void delete(Long id, HttpServletRequest request) {
        UserAccount me = current.get();
        SchoolDocument doc = loadVisible(id);
        boolean owner = doc.getOwner().getId().equals(me.getId());
        if (me.getRole() != Enums.Role.ADMIN && !(owner && doc.getStatus() == Enums.DocStatus.DRAFT)) {
            throw new ApiException(403, "Only an admin can delete this document. Teachers can delete their own drafts.");
        }
        for (DocumentVersion version : versions.findByDocumentIdOrderByVersionNumberDesc(id)) {
            versions.delete(version);
        }
        for (ApprovalLog log : approvals.findByDocumentIdOrderByCreatedAtDesc(id)) {
            approvals.delete(log);
        }
        documents.delete(doc);
        audit.record(me, "DOCUMENT_DELETE", doc.getTitle(), request);
    }

    public Path file(Long id, Integer version) {
        SchoolDocument doc = loadVisible(id);
        if (version == null || version == doc.getVersion()) {
            return files.resolve(doc.getFilePath());
        }
        DocumentVersion row = versions.findByDocumentIdAndVersionNumber(id, version)
                .orElseThrow(() -> new ApiException(404, "That version was not found."));
        return files.resolve(row.getFilePath());
    }

    public SchoolDocument visible(Long id) {
        return loadVisible(id);
    }

    public Map<String, Object> analyze(String trackText, String subject, String className,
                                        MultipartFile primary, MultipartFile secondary) {
        current.get();
        Enums.Track track = parseTrack(trackText);
        if (track == Enums.Track.GENERAL) {
            throw new ApiException(400, "Choose REB or TVET before analysing.");
        }
        boolean reb = track == Enums.Track.REB;
        String primaryLabel = reb ? "Syllabus" : "Curriculum";
        String secondaryLabel = reb ? "Weeks breakdown" : "Chronogram";
        String primaryText = text.clip(text.read(primary), 14000);
        String secondaryText = secondary == null || secondary.isEmpty() ? "" : text.clip(text.read(secondary), 14000);
        if (primaryText.isBlank() && secondaryText.isBlank()) {
            throw new ApiException(400, "The file has no readable text. Use a PDF with text, a DOCX, or a TXT file.");
        }
        Map<String, Object> analysis = gemini.analyze(track.name(), subject, className,
                primaryLabel, primaryText, secondaryLabel, secondaryText);
        analysis.putIfAbsent("summary", "");
        analysis.put("track", track.name());
        analysis.put("subject", subject);
        analysis.put("className", className);
        analysis.put("primaryLabel", primaryLabel);
        analysis.put("secondaryLabel", secondaryLabel);
        analysis.put("primaryChars", primaryText.length());
        analysis.put("secondaryChars", secondaryText.length());
        return analysis;
    }

    @Transactional
    public Map<String, Object> saveScheme(String title, String trackText, String subject, String className,
                                           Map<String, Object> analysis, HttpServletRequest request) {
        UserAccount me = current.get();
        requireUpload(me);
        if (analysis == null) {
            throw new ApiException(400, "Analyse the documents before saving.");
        }
        Enums.Track track = parseTrack(trackText);
        String safeTitle = title == null || title.isBlank()
                ? ((track == Enums.Track.REB ? "Scheme of work" : "Scheme of work") + " - " + blank(subject))
                : title.trim();
        byte[] bytes = workbook(safeTitle, subject, className, analysis);
        String stored = files.saveBytes(safeTitle.replaceAll("[^A-Za-z0-9._-]+", "_") + ".xlsx", bytes);
        SchoolDocument doc = new SchoolDocument();
        doc.setTitle(safeTitle);
        doc.setCategory(Enums.DocCategory.SCHEME_OF_WORK);
        doc.setTrack(track);
        doc.setStatus(Enums.DocStatus.DRAFT);
        doc.setOwner(me);
        doc.setFilePath(stored);
        doc.setOriginalName(safeTitle + ".xlsx");
        doc.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        doc.setVersion(1);
        doc.setRoutedTo("Head of Studies");
        doc.setUpdatedAt(Instant.now());
        try {
            doc.setAnalysisJson(mapper.writeValueAsString(analysis));
        } catch (Exception ex) {
            throw new ApiException(500, "Could not store the analysis.");
        }
        documents.save(doc);
        addVersion(doc, "Generated from " + (track == Enums.Track.REB ? "Syllabus and Weeks breakdown" : "Curriculum and Chronogram"));
        audit.record(me, "SCHEME_SAVED", doc.getTitle(), request);
        return get(doc.getId());
    }

    public List<Map<String, Object>> queue(String status) {
        UserAccount me = current.get();
        Access.requireManager(me);
        List<Enums.DocStatus> wanted = new ArrayList<>();
        if (status == null || status.isBlank() || "QUEUE".equalsIgnoreCase(status)) {
            wanted.add(Enums.DocStatus.SUBMITTED);
            wanted.add(Enums.DocStatus.UNDER_REVIEW);
        } else {
            wanted.add(parseDocStatus(status));
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SchoolDocument doc : documents.findByStatusInOrderByUpdatedAtDesc(wanted)) {
            rows.add(Views.document(doc));
        }
        return rows;
    }

    @Transactional
    public Map<String, Object> decide(Long id, String action, String comment, HttpServletRequest request) {
        UserAccount me = current.get();
        Access.requireManager(me);
        if (me.getRole() == Enums.Role.HEAD_OF_STUDIES && !settings.enabled("hos.approve")) {
            throw new ApiException(403, "Approval is turned off for the Head of Studies.");
        }
        SchoolDocument doc = documents.findById(id).orElseThrow(() -> new ApiException(404, "Document not found."));
        Enums.DocStatus next = parseDocStatus(action);
        if (next != Enums.DocStatus.UNDER_REVIEW && next != Enums.DocStatus.APPROVED
                && next != Enums.DocStatus.REJECTED && next != Enums.DocStatus.RETURNED) {
            throw new ApiException(400, "Choose under review, approved, rejected, or returned.");
        }
        doc.setStatus(next);
        doc.setReviewerComment(comment);
        doc.setUpdatedAt(Instant.now());
        documents.save(doc);
        ApprovalLog log = new ApprovalLog();
        log.setDocument(doc);
        log.setReviewer(me);
        log.setAction(next.name());
        log.setComment(comment);
        approvals.save(log);
        Enums.Priority priority = next == Enums.DocStatus.REJECTED || next == Enums.DocStatus.RETURNED
                ? Enums.Priority.HIGH : Enums.Priority.NORMAL;
        notifications.notify(doc.getOwner(), "Document " + next.name().toLowerCase(Locale.ROOT).replace('_', ' '),
                doc.getTitle() + (comment == null || comment.isBlank() ? "" : ": " + comment),
                "APPROVAL", priority);
        audit.record(me, "DOCUMENT_" + next.name(), doc.getTitle(), request);
        return get(id);
    }

    @Transactional
    public int decideMany(List<Long> ids, String action, String comment, HttpServletRequest request) {
        if (ids == null) {
            return 0;
        }
        for (Long id : ids) {
            decide(id, action, comment, request);
        }
        return ids.size();
    }

    public boolean geminiReady() {
        return gemini.ready();
    }

    @Transactional
    public int scanMissing(HttpServletRequest request) {
        UserAccount me = current.get();
        Access.requireManager(me);
        int count = 0;
        for (UserAccount teacher : notifications.activeRole(Enums.Role.TEACHER)) {
            boolean hasPlan = false;
            for (SchoolDocument doc : documents.findByOwnerIdOrderByUpdatedAtDesc(teacher.getId())) {
                if (doc.getCategory() == Enums.DocCategory.LESSON_PLAN && doc.getStatus() != Enums.DocStatus.DRAFT) {
                    hasPlan = true;
                    break;
                }
            }
            if (!hasPlan) {
                notifications.notify(teacher, "Lesson plan missing",
                        "Please upload and submit your lesson plan.", "DOCUMENT", Enums.Priority.HIGH);
                count++;
            }
        }
        if (count > 0) {
            notifications.notifyRole(Enums.Role.HEAD_OF_STUDIES, "Missing lesson plans",
                    count + " teachers have no submitted lesson plan.", "DOCUMENT", Enums.Priority.HIGH);
        }
        audit.record(me, "MISSING_SCAN", count + " teachers", request);
        return count;
    }

    private void requireUpload(UserAccount me) {
        if (me.getRole() == Enums.Role.TEACHER && !settings.enabled("teacher.upload")) {
            throw new ApiException(403, "Document upload is turned off for teachers.");
        }
    }

    private void addVersion(SchoolDocument doc, String note) {
        DocumentVersion version = new DocumentVersion();
        version.setDocument(doc);
        version.setVersionNumber(doc.getVersion());
        version.setFilePath(doc.getFilePath());
        version.setOriginalName(doc.getOriginalName());
        version.setNote(note);
        versions.save(version);
    }

    private SchoolDocument loadVisible(Long id) {
        UserAccount me = current.get();
        SchoolDocument doc = documents.findById(id).orElseThrow(() -> new ApiException(404, "Document not found."));
        if (me.getRole() == Enums.Role.TEACHER && !doc.getOwner().getId().equals(me.getId())) {
            throw new ApiException(403, "You can only open your own documents.");
        }
        return doc;
    }

    private boolean match(SchoolDocument doc, String q, String category, String status, String track) {
        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)
                && !doc.getCategory().name().equalsIgnoreCase(category)) {
            return false;
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)
                && !doc.getStatus().name().equalsIgnoreCase(status)) {
            return false;
        }
        if (track != null && !track.isBlank() && !"ALL".equalsIgnoreCase(track)
                && !doc.getTrack().name().equalsIgnoreCase(track)) {
            return false;
        }
        if (q != null && !q.isBlank()) {
            String hay = (doc.getTitle() + " " + doc.getOwner().getFullName() + " " + doc.getOriginalName()).toLowerCase(Locale.ROOT);
            if (!hay.contains(q.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    private Enums.DocCategory parseCategory(String value) {
        try {
            return Enums.DocCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new ApiException(400, "Unknown document type.");
        }
    }

    private Enums.Track parseTrack(String value) {
        if (value == null || value.isBlank()) {
            return Enums.Track.GENERAL;
        }
        try {
            return Enums.Track.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new ApiException(400, "Track must be REB or TVET.");
        }
    }

    private Enums.DocStatus parseDocStatus(String value) {
        try {
            return Enums.DocStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new ApiException(400, "Unknown document status.");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAnalysis(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(json, Map.class);
        } catch (Exception ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private byte[] workbook(String title, String subject, String className, Map<String, Object> analysis) {
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet scheme = book.createSheet("Scheme of work");
            String[] headers = {"Week", "Unit", "Lesson", "Objectives", "Activities", "Resources", "Assessment", "Hours"};
            Row head = scheme.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                head.createCell(i).setCellValue(headers[i]);
            }
            List<Map<String, Object>> rows = (List<Map<String, Object>>) analysis.getOrDefault("scheme", List.of());
            int r = 1;
            for (Object item : rows) {
                if (!(item instanceof Map)) {
                    continue;
                }
                Map<String, Object> row = (Map<String, Object>) item;
                Row excel = scheme.createRow(r++);
                excel.createCell(0).setCellValue(str(row.get("week")));
                excel.createCell(1).setCellValue(str(row.get("unit")));
                excel.createCell(2).setCellValue(str(row.get("lesson")));
                excel.createCell(3).setCellValue(str(row.get("objectives")));
                excel.createCell(4).setCellValue(str(row.get("activities")));
                excel.createCell(5).setCellValue(str(row.get("resources")));
                excel.createCell(6).setCellValue(str(row.get("assessment")));
                excel.createCell(7).setCellValue(str(row.get("hours")));
            }
            for (int i = 0; i < headers.length; i++) {
                scheme.setColumnWidth(i, 5000);
            }
            Sheet info = book.createSheet("Summary");
            info.createRow(0).createCell(0).setCellValue(title);
            info.createRow(1).createCell(0).setCellValue("Subject: " + blank(subject));
            info.createRow(2).createCell(0).setCellValue("Class: " + blank(className));
            info.createRow(3).createCell(0).setCellValue(str(analysis.get("summary")));
            info.setColumnWidth(0, 18000);
            book.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new ApiException(500, "Could not build the scheme of work file.");
        }
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String blank(String value) {
        return value == null ? "" : value;
    }
}
