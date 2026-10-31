package com.alice.smartteacher.service;

import com.alice.smartteacher.model.AuditLog;
import com.alice.smartteacher.model.LoginSession;
import com.alice.smartteacher.model.UserAccount;
import com.alice.smartteacher.repo.AuditLogRepository;
import com.alice.smartteacher.repo.LoginSessionRepository;
import com.alice.smartteacher.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class SecurityDeskService {

    private static final Set<String> KEYS = Set.of("teacher.upload", "teacher.biometric", "hos.approve");

    private final CurrentUserService current;
    private final LoginSessionRepository sessions;
    private final AuditLogRepository audits;
    private final SettingService settings;
    private final AuditService audit;

    public SecurityDeskService(CurrentUserService current, LoginSessionRepository sessions, AuditLogRepository audits,
                               SettingService settings, AuditService audit) {
        this.current = current;
        this.sessions = sessions;
        this.audits = audits;
        this.settings = settings;
        this.audit = audit;
    }

    public Map<String, Object> overview() {
        UserAccount me = current.get();
        Access.requireAdmin(me);
        List<Map<String, Object>> active = new ArrayList<>();
        for (LoginSession session : sessions.findByActiveTrue()) {
            active.add(Views.loginSession(session));
        }
        return Maps.of(
                "passwordStorage", "BCrypt (one-way hash)",
                "token", "Signed JWT, checked on every request",
                "files", "Saved on this computer in the uploads folder",
                "database", "MariaDB on localhost, not exposed by this student setup",
                "https", "Use localhost in class. A public server should add HTTPS.",
                "permissions", Maps.of(
                        "teacher.upload", settings.enabled("teacher.upload"),
                        "teacher.biometric", settings.enabled("teacher.biometric"),
                        "hos.approve", settings.enabled("hos.approve")
                ),
                "activeSessions", active.size(),
                "sessions", sessionList(""),
                "audit", auditList("")
        );
    }

    public List<Map<String, Object>> sessionList(String q) {
        Access.requireAdmin(current.get());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LoginSession session : sessions.findTop100ByOrderByCreatedAtDesc()) {
            String hay = (session.getUser().getFullName() + " " + session.getIpAddress()).toLowerCase(Locale.ROOT);
            if (q != null && !q.isBlank() && !hay.contains(q.toLowerCase(Locale.ROOT))) continue;
            rows.add(Views.loginSession(session));
        }
        return rows;
    }

    public List<Map<String, Object>> auditList(String q) {
        UserAccount me = current.get();
        Access.requireAdmin(me);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AuditLog log : audits.findTop200ByOrderByCreatedAtDesc()) {
            String detail = log.getDetail() == null ? "" : log.getDetail();
            String hay = (log.getAction() + " " + detail).toLowerCase(Locale.ROOT);
            if (q != null && !q.isBlank() && !hay.contains(q.toLowerCase(Locale.ROOT))) continue;
            rows.add(Maps.of(
                    "id", log.getId(),
                    "action", log.getAction(),
                    "detail", detail,
                    "user", log.getUser() == null ? "System" : log.getUser().getFullName(),
                    "ipAddress", log.getIpAddress(),
                    "createdAt", log.getCreatedAt()
            ));
        }
        return rows;
    }

    @Transactional
    public void endSession(Long id) {
        UserAccount me = current.get();
        Access.requireAdmin(me);
        LoginSession session = sessions.findById(id).orElseThrow(() -> new ApiException(404, "Session not found."));
        session.setActive(false);
        session.setEndedAt(Instant.now());
        sessions.save(session);
    }

    @Transactional
    public void putPermission(String key, boolean enabled, javax.servlet.http.HttpServletRequest request) {
        UserAccount me = current.get();
        Access.requireAdmin(me);
        if (!KEYS.contains(key)) {
            throw new ApiException(400, "Unknown permission.");
        }
        settings.put(key, enabled ? "true" : "false");
        audit.record(me, "PERMISSION", key + "=" + enabled, request);
    }
}
