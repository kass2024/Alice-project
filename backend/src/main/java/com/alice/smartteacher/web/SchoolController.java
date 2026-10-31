package com.alice.smartteacher.web;

import com.alice.smartteacher.service.ActivityService;
import com.alice.smartteacher.service.NotificationService;
import com.alice.smartteacher.service.ReportService;
import com.alice.smartteacher.service.SecurityDeskService;
import com.alice.smartteacher.service.StorageService;
import com.alice.smartteacher.service.UserAdminService;
import com.alice.smartteacher.service.Maps;
import com.alice.smartteacher.service.Views;
import com.alice.smartteacher.model.AcademicRecord;
import com.alice.smartteacher.model.Notification;
import com.alice.smartteacher.model.NotificationPreference;
import com.alice.smartteacher.repo.NotificationPreferenceRepository;
import com.alice.smartteacher.repo.NotificationRepository;
import com.alice.smartteacher.service.CurrentUserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class SchoolController {

    private final ActivityService activities;
    private final ReportService reports;
    private final UserAdminService people;
    private final StorageService storage;
    private final SecurityDeskService security;
    private final NotificationRepository notifications;
    private final NotificationPreferenceRepository preferences;
    private final CurrentUserService current;
    private final NotificationService notificationService;

    public SchoolController(ActivityService activities, ReportService reports, UserAdminService people,
                            StorageService storage, SecurityDeskService security,
                            NotificationRepository notifications, NotificationPreferenceRepository preferences,
                            CurrentUserService current, NotificationService notificationService) {
        this.activities = activities;
        this.reports = reports;
        this.people = people;
        this.storage = storage;
        this.security = security;
        this.notifications = notifications;
        this.preferences = preferences;
        this.current = current;
        this.notificationService = notificationService;
    }

    @GetMapping("/api/activities")
    public Map<String, Object> activities(@RequestParam(required = false) String from,
                                          @RequestParam(required = false) String to,
                                          @RequestParam(required = false) Long teacherId,
                                          @RequestParam(required = false) String status) {
        return activities.list(from, to, teacherId, status);
    }

    @PostMapping("/api/activities")
    public Map<String, Object> createActivity(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return activities.create(body, request);
    }

    @PostMapping("/api/activities/{id}/start")
    public Map<String, Object> start(@PathVariable Long id) {
        return activities.start(id);
    }

    @PostMapping("/api/activities/{id}/end")
    public Map<String, Object> end(@PathVariable Long id) {
        return activities.end(id);
    }

    @GetMapping("/api/dashboard")
    public Map<String, Object> dashboard(@RequestParam(required = false) String from,
                                         @RequestParam(required = false) String to,
                                         @RequestParam(required = false) String department) {
        return reports.dashboard(from, to, department);
    }

    @GetMapping("/api/reports")
    public Map<String, Object> report(@RequestParam(required = false) String from,
                                      @RequestParam(required = false) String to,
                                      @RequestParam(required = false) String department) {
        return reports.report(from, to, department);
    }

    @PutMapping("/api/reports/schedule")
    public Map<String, Boolean> schedule(@RequestBody Map<String, String> body) {
        reports.saveSchedule(body.get("time"));
        return Map.of("ok", true);
    }

    @GetMapping("/api/reports/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "attendance") String type,
                                         @RequestParam(defaultValue = "csv") String format,
                                         @RequestParam(required = false) String from,
                                         @RequestParam(required = false) String to) {
        ReportService.Download file = reports.export(type, format, from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename + "\"")
                .contentType(MediaType.parseMediaType(file.contentType))
                .body(file.bytes);
    }

    @GetMapping("/api/users")
    public List<Map<String, Object>> users() {
        return people.list();
    }

    @PostMapping("/api/users")
    public Map<String, Object> createUser(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return people.create(body, request);
    }

    @PutMapping("/api/users/{id}")
    public Map<String, Object> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        return people.update(id, body, request);
    }

    @PostMapping("/api/users/{id}/reset-password")
    public Map<String, Object> reset(@PathVariable Long id, HttpServletRequest request) {
        return people.resetPassword(id, request);
    }

    @GetMapping("/api/teachers/{id}/profile")
    public Map<String, Object> profile(@PathVariable Long id) {
        return people.profile(id);
    }

    @GetMapping("/api/notifications")
    public List<Map<String, Object>> notes() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Notification note : notificationService.mine(current.get())) {
            rows.add(Views.notification(note));
        }
        return rows;
    }

    @PostMapping("/api/notifications/{id}/read")
    public Map<String, Boolean> read(@PathVariable Long id) {
        Notification note = notifications.findById(id).orElseThrow();
        var me = current.get();
        if (me.getRole() != com.alice.smartteacher.model.Enums.Role.ADMIN && !note.getUser().getId().equals(me.getId())) {
            throw new ApiException(403, "That notification is not yours.");
        }
        note.setReadFlag(true);
        notifications.save(note);
        return Map.of("ok", true);
    }

    @PostMapping("/api/notifications/read-all")
    public Map<String, Boolean> readAll() {
        var me = current.get();
        boolean admin = me.getRole() == com.alice.smartteacher.model.Enums.Role.ADMIN;
        Iterable<Notification> notes = admin
                ? notifications.findAll()
                : notifications.findByUserIdOrderByCreatedAtDesc(me.getId());
        for (Notification note : notes) {
            note.setReadFlag(true);
            notifications.save(note);
        }
        return Map.of("ok", true);
    }

    @GetMapping("/api/notifications/preferences")
    public Map<String, Boolean> prefs() {
        NotificationPreference pref = preferences.findByUserId(current.get().getId()).orElseGet(NotificationPreference::new);
        return Map.of("email", pref.isEmailOn(), "sms", pref.isSmsOn(), "inApp", pref.isInAppOn());
    }

    @PutMapping("/api/notifications/preferences")
    public Map<String, Boolean> savePrefs(@RequestBody Map<String, Boolean> body) {
        var me = current.get();
        NotificationPreference pref = preferences.findByUserId(me.getId()).orElseGet(NotificationPreference::new);
        pref.setUser(me);
        if (body.containsKey("email")) pref.setEmailOn(Boolean.TRUE.equals(body.get("email")));
        if (body.containsKey("sms")) pref.setSmsOn(Boolean.TRUE.equals(body.get("sms")));
        if (body.containsKey("inApp")) pref.setInAppOn(Boolean.TRUE.equals(body.get("inApp")));
        preferences.save(pref);
        return Map.of("ok", true);
    }

    @GetMapping("/api/storage")
    public Map<String, Object> storage() {
        return storage.overview();
    }

    @GetMapping("/api/academic")
    public List<Map<String, Object>> academic() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AcademicRecord row : storage.academicRows()) {
            rows.add(Maps.of(
                    "id", row.getId(),
                    "studentName", row.getStudentName(),
                    "className", row.getClassName(),
                    "subject", row.getSubject(),
                    "termName", row.getTermName() == null ? "" : row.getTermName(),
                    "score", row.getScore() == null ? "" : row.getScore(),
                    "teacherName", row.getTeacherName() == null ? "" : row.getTeacherName()
            ));
        }
        return rows;
    }

    @GetMapping("/api/academic/export")
    public ResponseEntity<byte[]> exportAcademic() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"academic-records.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(storage.exportAcademic());
    }

    @PostMapping("/api/academic/import")
    public Map<String, Object> importAcademic(@RequestParam MultipartFile file, HttpServletRequest request) {
        return Map.of("imported", storage.importAcademic(file, request));
    }

    @PutMapping("/api/storage/sync")
    public Map<String, Boolean> sync(@RequestBody Map<String, Boolean> body) {
        storage.setSync(Boolean.TRUE.equals(body.get("enabled")));
        return Map.of("ok", true);
    }

    @GetMapping("/api/storage/backup")
    public ResponseEntity<byte[]> backup(HttpServletRequest request) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"smart-teacher-backup.sql\"")
                .contentType(MediaType.parseMediaType("application/sql"))
                .body(storage.backup(request));
    }

    @PostMapping("/api/storage/restore")
    public Map<String, String> restore(@RequestParam MultipartFile file, HttpServletRequest request) {
        return Map.of("message", storage.restore(file, request));
    }

    @GetMapping("/api/security")
    public Map<String, Object> security(@RequestParam(required = false) String q) {
        Map<String, Object> data = security.overview();
        if (q != null && !q.isBlank()) {
            data.put("audit", security.auditList(q));
            data.put("sessions", security.sessionList(q));
        }
        return data;
    }

    @PostMapping("/api/security/sessions/{id}/end")
    public Map<String, Boolean> endSession(@PathVariable Long id) {
        security.endSession(id);
        return Map.of("ok", true);
    }

    @PutMapping("/api/security/permissions")
    public Map<String, Boolean> permission(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        security.putPermission(String.valueOf(body.get("key")), Boolean.parseBoolean(String.valueOf(body.get("enabled"))), request);
        return Map.of("ok", true);
    }
}
