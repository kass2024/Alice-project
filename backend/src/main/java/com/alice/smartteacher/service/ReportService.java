package com.alice.smartteacher.service;

import com.alice.smartteacher.model.AttendanceRecord;
import com.alice.smartteacher.model.AuditLog;
import com.alice.smartteacher.model.Enums;
import com.alice.smartteacher.model.SchoolDocument;
import com.alice.smartteacher.model.TeachingSession;
import com.alice.smartteacher.model.UserAccount;
import com.alice.smartteacher.repo.AuditLogRepository;
import com.alice.smartteacher.repo.DocumentRepository;
import com.alice.smartteacher.repo.NotificationRepository;
import com.alice.smartteacher.repo.UserRepository;
import com.alice.smartteacher.web.ApiException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final CurrentUserService current;
    private final AttendanceService attendance;
    private final ActivityService activities;
    private final DocumentRepository documents;
    private final UserRepository users;
    private final AuditLogRepository audits;
    private final NotificationRepository notifications;
    private final SettingService settings;

    public ReportService(CurrentUserService current, AttendanceService attendance, ActivityService activities,
                         DocumentRepository documents, UserRepository users, AuditLogRepository audits,
                         NotificationRepository notifications, SettingService settings) {
        this.current = current;
        this.attendance = attendance;
        this.activities = activities;
        this.documents = documents;
        this.users = users;
        this.audits = audits;
        this.notifications = notifications;
        this.settings = settings;
    }

    public Map<String, Object> dashboard(String fromText, String toText, String department) {
        UserAccount me = current.get();
        LocalDate from = fromText == null || fromText.isBlank() ? SchoolClock.today().minusDays(13) : LocalDate.parse(fromText);
        LocalDate to = toText == null || toText.isBlank() ? SchoolClock.today() : LocalDate.parse(toText);
        LocalDate today = SchoolClock.today();
        int present = 0, absent = 0, late = 0, leave = 0;
        for (AttendanceRecord record : attendance.between(today, today)) {
            if (!dept(record.getTeacher(), department) || hidden(me, record.getTeacher())) continue;
            switch (record.getStatus()) {
                case PRESENT: present++; break;
                case ABSENT: absent++; break;
                case LATE: late++; break;
                case ON_LEAVE: leave++; break;
                default: break;
            }
        }
        Map<LocalDate, int[]> byDay = new LinkedHashMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            byDay.put(day, new int[3]);
        }
        for (AttendanceRecord record : attendance.between(from, to)) {
            if (!dept(record.getTeacher(), department) || hidden(me, record.getTeacher())) continue;
            int[] bucket = byDay.get(record.getAttendDate());
            if (bucket == null) continue;
            if (record.getStatus() == Enums.AttendanceStatus.PRESENT) bucket[0]++;
            else if (record.getStatus() == Enums.AttendanceStatus.ABSENT) bucket[1]++;
            else if (record.getStatus() == Enums.AttendanceStatus.LATE) bucket[2]++;
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        for (Map.Entry<LocalDate, int[]> entry : byDay.entrySet()) {
            trend.add(Maps.of("date", entry.getKey().toString(), "present", entry.getValue()[0],
                    "absent", entry.getValue()[1], "late", entry.getValue()[2]));
        }
        Map<String, Integer> docStatus = new LinkedHashMap<>();
        for (Enums.DocStatus status : Enums.DocStatus.values()) docStatus.put(status.name(), 0);
        Instant monthStart = today.withDayOfMonth(1).atStartOfDay(SchoolClock.ZONE).toInstant();
        int docsThisMonth = 0;
        List<SchoolDocument> allDocs = me.getRole() == Enums.Role.TEACHER
                ? documents.findByOwnerIdOrderByUpdatedAtDesc(me.getId())
                : documents.findAllByOrderByUpdatedAtDesc();
        for (SchoolDocument doc : allDocs) {
            docStatus.put(doc.getStatus().name(), docStatus.get(doc.getStatus().name()) + 1);
            if (doc.getCreatedAt() != null && !doc.getCreatedAt().isBefore(monthStart)) docsThisMonth++;
        }
        List<Map<String, Object>> docPie = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : docStatus.entrySet()) {
            if (entry.getValue() > 0) docPie.add(Maps.of("name", entry.getKey(), "value", entry.getValue()));
        }
        List<Map<String, Object>> tracker = new ArrayList<>();
        List<Map<String, Object>> compliance = new ArrayList<>();
        for (UserAccount teacher : attendance.teachers(department)) {
            if (hidden(me, teacher)) continue;
            boolean lesson = false, scheme = false, register = false;
            for (SchoolDocument doc : documents.findByOwnerIdOrderByUpdatedAtDesc(teacher.getId())) {
                if (doc.getCategory() == Enums.DocCategory.LESSON_PLAN) lesson = true;
                if (doc.getCategory() == Enums.DocCategory.SCHEME_OF_WORK) scheme = true;
                if (doc.getCategory() == Enums.DocCategory.CLASS_REGISTER) register = true;
            }
            tracker.add(Maps.of("teacherId", teacher.getId(), "teacher", teacher.getFullName(),
                    "lessonPlan", lesson, "scheme", scheme, "register", register));
            if (!lesson) compliance.add(Maps.of("teacher", teacher.getFullName(), "issue", "No lesson plan in the repository"));
        }
        for (AttendanceRecord record : attendance.between(today, today)) {
            if (record.getStatus() == Enums.AttendanceStatus.ABSENT && dept(record.getTeacher(), department)) {
                compliance.add(Maps.of("teacher", record.getTeacher().getFullName(), "issue", "Absent today"));
            }
        }
        List<Map<String, Object>> queue = new ArrayList<>();
        if (Access.managesSchool(me)) {
            for (SchoolDocument doc : documents.findByStatusInOrderByUpdatedAtDesc(List.of(Enums.DocStatus.SUBMITTED, Enums.DocStatus.UNDER_REVIEW))) {
                if (queue.size() >= 6) break;
                queue.add(Views.document(doc));
            }
        }
        List<Map<String, Object>> schedule = new ArrayList<>();
        int missed = 0;
        int classes = 0;
        for (TeachingSession session : activities.between(today, today)) {
            if (hidden(me, session.getTeacher()) || !dept(session.getTeacher(), department)) continue;
            classes++;
            if (session.getStatus() == Enums.ActivityStatus.MISSED) missed++;
            schedule.add(Views.session(session));
        }
        List<Map<String, Object>> recent = new ArrayList<>();
        for (AuditLog log : audits.findTop200ByOrderByCreatedAtDesc()) {
            if (recent.size() >= 8) break;
            if (me.getRole() == Enums.Role.TEACHER && (log.getUser() == null || !log.getUser().getId().equals(me.getId()))) {
                continue;
            }
            recent.add(Maps.of("action", log.getAction(), "detail", log.getDetail(),
                    "user", log.getUser() == null ? "System" : log.getUser().getFullName(), "createdAt", log.getCreatedAt()));
        }
        return Maps.of(
                "kpis", Maps.of(
                        "presentToday", present, "absentToday", absent, "lateToday", late, "onLeaveToday", leave,
                        "pendingApprovals", documents.countByStatus(Enums.DocStatus.SUBMITTED) + documents.countByStatus(Enums.DocStatus.UNDER_REVIEW),
                        "documentsThisMonth", docsThisMonth, "classesToday", classes, "missedClasses", missed,
                        "unread", notifications.countByUserIdAndReadFlagFalse(me.getId())
                ),
                "trend", trend,
                "documentsByStatus", docPie,
                "tracker", tracker,
                "queue", queue,
                "schedule", schedule,
                "compliance", compliance,
                "recent", recent,
                "from", from, "to", to
        );
    }

    public Map<String, Object> report(String fromText, String toText, String department) {
        Access.requireManager(current.get());
        LocalDate from = fromText == null || fromText.isBlank() ? SchoolClock.today().minusDays(30) : LocalDate.parse(fromText);
        LocalDate to = toText == null || toText.isBlank() ? SchoolClock.today() : LocalDate.parse(toText);
        List<Map<String, Object>> teacherRates = new ArrayList<>();
        for (UserAccount teacher : attendance.teachers(department)) {
            int total = 0, attended = 0;
            for (AttendanceRecord record : attendance.between(from, to)) {
                if (!record.getTeacher().getId().equals(teacher.getId())) continue;
                total++;
                if (record.getStatus() == Enums.AttendanceStatus.PRESENT || record.getStatus() == Enums.AttendanceStatus.LATE) {
                    attended++;
                }
            }
            int rate = total == 0 ? 0 : (int) Math.round(attended * 100.0 / total);
            teacherRates.add(Maps.of("teacher", teacher.getFullName(), "rate", rate, "attended", attended, "total", total));
        }
        Map<String, Integer> pie = new LinkedHashMap<>();
        pie.put("PRESENT", 0); pie.put("ABSENT", 0); pie.put("LATE", 0); pie.put("ON_LEAVE", 0);
        List<Map<String, Object>> daily = new ArrayList<>();
        Map<LocalDate, int[]> days = new LinkedHashMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) days.put(day, new int[2]);
        for (AttendanceRecord record : attendance.between(from, to)) {
            if (!dept(record.getTeacher(), department)) continue;
            pie.put(record.getStatus().name(), pie.get(record.getStatus().name()) + 1);
            int[] bucket = days.get(record.getAttendDate());
            if (bucket != null && (record.getStatus() == Enums.AttendanceStatus.PRESENT || record.getStatus() == Enums.AttendanceStatus.LATE)) {
                bucket[0]++;
            } else if (bucket != null) {
                bucket[1]++;
            }
        }
        for (Map.Entry<LocalDate, int[]> entry : days.entrySet()) {
            daily.add(Maps.of("date", entry.getKey().toString(), "attended", entry.getValue()[0], "away", entry.getValue()[1]));
        }
        List<Map<String, Object>> statusPie = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : pie.entrySet()) statusPie.add(Maps.of("name", entry.getKey(), "value", entry.getValue()));
        Map<String, Object> activity = activities.list(from.toString(), to.toString(), null, null);
        Map<String, Integer> categories = new LinkedHashMap<>();
        for (SchoolDocument doc : documents.findAllByOrderByUpdatedAtDesc()) {
            if (doc.getCreatedAt() == null) continue;
            LocalDate created = doc.getCreatedAt().atZone(SchoolClock.ZONE).toLocalDate();
            if (created.isBefore(from) || created.isAfter(to)) continue;
            categories.put(doc.getCategory().name(), categories.getOrDefault(doc.getCategory().name(), 0) + 1);
        }
        List<Map<String, Object>> docBars = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : categories.entrySet()) {
            docBars.add(Maps.of("name", entry.getKey(), "value", entry.getValue()));
        }
        return Maps.of(
                "from", from, "to", to,
                "teacherRates", teacherRates,
                "daily", daily,
                "statusPie", statusPie,
                "activity", activity.get("stats"),
                "documentBars", docBars,
                "schedule", settings.get("report.schedule", "18:00"),
                "lastRun", settings.get("report.lastRun", "")
        );
    }

    public void saveSchedule(String time) {
        Access.requireAdmin(current.get());
        if (time == null || !time.matches("\\d{2}:\\d{2}")) {
            throw new ApiException(400, "Use a time like 18:00.");
        }
        settings.put("report.schedule", time);
    }

    @Transactional
    public Download export(String type, String format, String fromText, String toText) {
        Access.requireManager(current.get());
        LocalDate from = fromText == null || fromText.isBlank() ? SchoolClock.today().minusDays(30) : LocalDate.parse(fromText);
        LocalDate to = toText == null || toText.isBlank() ? SchoolClock.today() : LocalDate.parse(toText);
        List<String[]> table = table(type, from, to);
        String base = (type == null ? "report" : type) + "-" + from + "-to-" + to;
        if ("xlsx".equalsIgnoreCase(format) || "excel".equalsIgnoreCase(format)) {
            return new Download(xlsx(table), base + ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        }
        if ("pdf".equalsIgnoreCase(format)) {
            return new Download(pdf(title(type), table), base + ".pdf", "application/pdf");
        }
        return new Download(csv(table), base + ".csv", "text/csv");
    }

    private List<String[]> table(String type, LocalDate from, LocalDate to) {
        List<String[]> rows = new ArrayList<>();
        if ("documents".equalsIgnoreCase(type)) {
            rows.add(new String[]{"Title", "Type", "Track", "Status", "Owner", "Version"});
            for (SchoolDocument doc : documents.findAllByOrderByUpdatedAtDesc()) {
                rows.add(new String[]{doc.getTitle(), doc.getCategory().name(), doc.getTrack().name(),
                        doc.getStatus().name(), doc.getOwner().getFullName(), String.valueOf(doc.getVersion())});
            }
            return rows;
        }
        if ("activity".equalsIgnoreCase(type)) {
            rows.add(new String[]{"Date", "Teacher", "Class", "Subject", "Start", "End", "Status"});
            for (TeachingSession session : activities.between(from, to)) {
                rows.add(new String[]{session.getSessionDate().toString(), session.getTeacher().getFullName(),
                        session.getClassName(), session.getSubject(), session.getStartTime().toString(),
                        session.getEndTime().toString(), session.getStatus().name()});
            }
            return rows;
        }
        rows.add(new String[]{"Date", "Teacher", "Department", "Status", "Check in", "Check out", "Method"});
        for (AttendanceRecord record : attendance.between(from, to)) {
            rows.add(new String[]{
                    String.valueOf(record.getAttendDate()), record.getTeacher().getFullName(),
                    String.valueOf(record.getTeacher().getDepartment()), record.getStatus().name(),
                    record.getCheckIn() == null ? "" : record.getCheckIn().toString(),
                    record.getCheckOut() == null ? "" : record.getCheckOut().toString(),
                    record.getMethod().name()
            });
        }
        return rows;
    }

    private byte[] csv(List<String[]> rows) {
        StringBuilder out = new StringBuilder("\uFEFF");
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) out.append(',');
                String cell = row[i] == null ? "" : row[i].replace("\"", "\"\"");
                out.append('"').append(cell).append('"');
            }
            out.append("\n");
        }
        return out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] xlsx(List<String[]> rows) {
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = book.createSheet("Report");
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < rows.get(r).length; c++) row.createCell(c).setCellValue(rows.get(r)[c]);
            }
            book.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new ApiException(500, "Could not build the Excel file.");
        }
    }

    private byte[] pdf(String title, List<String[]> rows) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(doc, page);
            stream.beginText();
            stream.setFont(PDType1Font.HELVETICA_BOLD, 14);
            stream.newLineAtOffset(48, 760);
            stream.showText(cut(title, 70));
            stream.setFont(PDType1Font.HELVETICA, 9);
            stream.newLineAtOffset(0, -22);
            int shown = 0;
            for (String[] row : rows) {
                if (shown > 32) break;
                stream.showText(cut(String.join("  |  ", row), 110));
                stream.newLineAtOffset(0, -16);
                shown++;
            }
            if (rows.size() > 33) {
                stream.showText("Open the Excel export for the full list.");
            }
            stream.endText();
            stream.close();
            doc.save(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new ApiException(500, "Could not build the PDF.");
        }
    }

    private String title(String type) {
        if ("documents".equalsIgnoreCase(type)) return "Document submission report";
        if ("activity".equalsIgnoreCase(type)) return "Teaching activity report";
        return "Teacher attendance report";
    }

    private String cut(String text, int max) {
        String clean = text == null ? "" : text.replaceAll("[^\\x20-\\x7E]", " ");
        return clean.length() <= max ? clean : clean.substring(0, max);
    }

    private boolean dept(UserAccount teacher, String department) {
        return department == null || department.isBlank() || "ALL".equalsIgnoreCase(department)
                || department.equalsIgnoreCase(teacher.getDepartment());
    }

    private boolean hidden(UserAccount me, UserAccount teacher) {
        return me.getRole() == Enums.Role.TEACHER && !me.getId().equals(teacher.getId());
    }

    public static class Download {
        public final byte[] bytes;
        public final String filename;
        public final String contentType;

        public Download(byte[] bytes, String filename, String contentType) {
            this.bytes = bytes;
            this.filename = filename;
            this.contentType = contentType;
        }
    }

    public long days(LocalDate from, LocalDate to) {
        return ChronoUnit.DAYS.between(from, to);
    }
}
