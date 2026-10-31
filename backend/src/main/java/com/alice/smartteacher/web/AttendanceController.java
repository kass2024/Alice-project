package com.alice.smartteacher.web;

import com.alice.smartteacher.model.UserAccount;
import com.alice.smartteacher.service.AttendanceService;
import com.alice.smartteacher.service.FileStore;
import com.alice.smartteacher.service.ReportService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

@RestController
public class AttendanceController {

    private final AttendanceService attendance;
    private final ReportService reports;
    private final FileStore files;

    public AttendanceController(AttendanceService attendance, ReportService reports, FileStore files) {
        this.attendance = attendance;
        this.reports = reports;
        this.files = files;
    }

    @GetMapping("/api/attendance/sheet")
    public Map<String, Object> sheet(@RequestParam(required = false) String date,
                                     @RequestParam(required = false) String department) {
        return attendance.sheet(date, department);
    }

    @GetMapping("/api/attendance")
    public Map<String, Object> range(@RequestParam(required = false) String from,
                                     @RequestParam(required = false) String to,
                                     @RequestParam(required = false) String department,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) String q,
                                     @RequestParam(required = false) String method) {
        return attendance.range(from, to, department, status, q, method);
    }

    @GetMapping("/api/attendance/history/{teacherId}")
    public List<Map<String, Object>> history(@PathVariable Long teacherId) {
        return attendance.history(teacherId);
    }

    @GetMapping("/api/attendance/biometric")
    public List<Map<String, Object>> biometric() {
        return attendance.biometricLogs();
    }

    @PostMapping("/api/attendance/mark")
    public Map<String, Object> mark(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return attendance.markOne(longVal(body.get("teacherId")), str(body.get("date")), str(body.get("status")), str(body.get("note")), request);
    }

    @PostMapping("/api/attendance/bulk")
    @SuppressWarnings("unchecked")
    public Map<String, Object> bulk(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        int count = attendance.bulk(str(body.get("date")), (List<Map<String, Object>>) body.get("rows"), request);
        return Map.of("saved", count);
    }

    @PostMapping("/api/attendance/override")
    public Map<String, Object> override(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return attendance.override(longVal(body.get("teacherId")), str(body.get("date")), str(body.get("status")), str(body.get("note")), request);
    }

    @PostMapping("/api/attendance/face")
    @SuppressWarnings("unchecked")
    public Map<String, Object> face(@RequestBody Map<String, Object> body) {
        return attendance.face((List<?>) body.get("descriptor"), str(body.get("action")));
    }

    @PostMapping(value = "/api/teachers/{id}/face", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Boolean> enroll(@PathVariable Long id,
                                       @RequestParam String descriptor,
                                       @RequestParam(required = false) MultipartFile photo,
                                       HttpServletRequest request) {
        attendance.enroll(id, descriptor, photo, request);
        return Map.of("ok", true);
    }

    @PostMapping(value = "/api/teachers/{id}/face", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Boolean> enrollJson(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body,
                                           HttpServletRequest request) throws Exception {
        attendance.enroll(id, new ObjectMapper().writeValueAsString(body.get("descriptor")), null, request);
        return Map.of("ok", true);
    }

    @DeleteMapping("/api/teachers/{id}/face")
    public Map<String, Boolean> clearFace(@PathVariable Long id, HttpServletRequest request) {
        attendance.clearFace(id, request);
        return Map.of("ok", true);
    }

    @GetMapping("/api/teachers/{id}/photo")
    public ResponseEntity<Resource> photo(@PathVariable Long id) throws Exception {
        UserAccount teacher = attendance.photoOwner(id);
        if (teacher.getPhotoPath() == null || teacher.getPhotoPath().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        java.nio.file.Path path = files.resolve(teacher.getPhotoPath());
        String type = Files.probeContentType(path);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(type == null ? "image/jpeg" : type))
                .body(new FileSystemResource(path));
    }

    @GetMapping("/api/attendance/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "csv") String format,
                                         @RequestParam(required = false) String from,
                                         @RequestParam(required = false) String to) {
        ReportService.Download file = reports.export("attendance", format, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename + "\"")
                .contentType(MediaType.parseMediaType(file.contentType))
                .body(file.bytes);
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long longVal(Object value) {
        return Long.valueOf(String.valueOf(value));
    }
}
