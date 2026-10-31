package com.alice.smartteacher.web;

import com.alice.smartteacher.model.SchoolDocument;
import com.alice.smartteacher.service.DocumentService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
public class DocumentController {

    private final DocumentService documents;

    public DocumentController(DocumentService documents) {
        this.documents = documents;
    }

    @GetMapping("/api/documents")
    public List<Map<String, Object>> list(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) String category,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String track) {
        return documents.list(q, category, status, track);
    }

    @GetMapping("/api/documents/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        return documents.get(id);
    }

    @PostMapping("/api/documents")
    public Map<String, Object> create(@RequestParam String title,
                                      @RequestParam String category,
                                      @RequestParam(required = false) String track,
                                      @RequestParam MultipartFile file,
                                      HttpServletRequest request) {
        return documents.create(title, category, track, file, request);
    }

    @PostMapping("/api/documents/{id}/file")
    public Map<String, Object> replace(@PathVariable Long id,
                                       @RequestParam MultipartFile file,
                                       @RequestParam(required = false) String note,
                                       HttpServletRequest request) {
        return documents.replace(id, file, note, request);
    }

    @PutMapping("/api/documents/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return documents.update(id, body.get("title"), body.get("category"), body.get("track"));
    }

    @PostMapping("/api/documents/{id}/submit")
    public Map<String, Object> submit(@PathVariable Long id, HttpServletRequest request) {
        return documents.submit(id, request);
    }

    @PostMapping("/api/documents/bulk")
    public Map<String, Object> bulk(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        List<Number> raw = (List<Number>) body.get("ids");
        return Map.of("count", documents.bulk(ids(raw), String.valueOf(body.get("action")), request));
    }

    @DeleteMapping("/api/documents/{id}")
    public Map<String, Boolean> delete(@PathVariable Long id, HttpServletRequest request) {
        documents.delete(id, request);
        return Map.of("ok", true);
    }

    @GetMapping("/api/documents/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id, @RequestParam(required = false) Integer version) {
        SchoolDocument doc = documents.visible(id);
        java.nio.file.Path path = documents.file(id, version);
        String type = doc.getContentType() == null ? "application/octet-stream" : doc.getContentType();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(type))
                .body(new FileSystemResource(path));
    }

    @GetMapping("/api/documents/gemini")
    public Map<String, Boolean> gemini() {
        return Map.of("ready", documents.geminiReady());
    }

    @PostMapping("/api/documents/analyze")
    public Map<String, Object> analyze(@RequestParam String track,
                                       @RequestParam(required = false) String subject,
                                       @RequestParam(required = false) String className,
                                       @RequestParam MultipartFile primary,
                                       @RequestParam(required = false) MultipartFile secondary) {
        return documents.analyze(track, subject, className, primary, secondary);
    }

    @PostMapping("/api/documents/scheme")
    @SuppressWarnings("unchecked")
    public Map<String, Object> scheme(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return documents.saveScheme(
                str(body.get("title")), str(body.get("track")), str(body.get("subject")), str(body.get("className")),
                (Map<String, Object>) body.get("analysis"), request);
    }

    @GetMapping("/api/approvals")
    public List<Map<String, Object>> queue(@RequestParam(required = false) String status) {
        return documents.queue(status);
    }

    @PostMapping("/api/approvals/{id}")
    public Map<String, Object> decide(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        return documents.decide(id, body.get("action"), body.get("comment"), request);
    }

    @PostMapping("/api/approvals/bulk")
    public Map<String, Object> decideMany(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        List<Number> raw = (List<Number>) body.get("ids");
        return Map.of("count", documents.decideMany(ids(raw), str(body.get("action")), str(body.get("comment")), request));
    }

    @PostMapping("/api/documents/scan-missing")
    public Map<String, Object> scan(HttpServletRequest request) {
        return Map.of("count", documents.scanMissing(request));
    }

    private List<Long> ids(List<Number> raw) {
        java.util.ArrayList<Long> ids = new java.util.ArrayList<>();
        if (raw != null) {
            for (Number number : raw) ids.add(number.longValue());
        }
        return ids;
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
