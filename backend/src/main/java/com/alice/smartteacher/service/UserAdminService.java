package com.alice.smartteacher.service;

import com.alice.smartteacher.model.Enums;
import com.alice.smartteacher.model.NotificationPreference;
import com.alice.smartteacher.model.SchoolDocument;
import com.alice.smartteacher.model.UserAccount;
import com.alice.smartteacher.repo.DocumentRepository;
import com.alice.smartteacher.repo.NotificationPreferenceRepository;
import com.alice.smartteacher.repo.UserRepository;
import com.alice.smartteacher.web.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class UserAdminService {

    private final UserRepository users;
    private final DocumentRepository documents;
    private final NotificationPreferenceRepository preferences;
    private final PasswordEncoder encoder;
    private final CurrentUserService current;
    private final AttendanceService attendance;
    private final AuditService audit;

    public UserAdminService(UserRepository users, DocumentRepository documents, NotificationPreferenceRepository preferences,
                            PasswordEncoder encoder, CurrentUserService current, AttendanceService attendance, AuditService audit) {
        this.users = users;
        this.documents = documents;
        this.preferences = preferences;
        this.encoder = encoder;
        this.current = current;
        this.attendance = attendance;
        this.audit = audit;
    }

    public List<Map<String, Object>> list() {
        Access.requireAdmin(current.get());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (UserAccount user : users.findAllByOrderByFullNameAsc()) {
            rows.add(Views.user(user));
        }
        return rows;
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body, HttpServletRequest request) {
        UserAccount me = current.get();
        Access.requireAdmin(me);
        String username = text(body.get("username"));
        String email = text(body.get("email"));
        String fullName = text(body.get("fullName"));
        if (username.isBlank() || email.isBlank() || fullName.isBlank()) {
            throw new ApiException(400, "Name, username, and email are required.");
        }
        if (users.existsByUsername(username)) {
            throw new ApiException(400, "That username is already used.");
        }
        if (users.existsByEmail(email)) {
            throw new ApiException(400, "That email is already used.");
        }
        UserAccount user = new UserAccount();
        user.setFullName(fullName);
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(text(body.get("phone")));
        user.setDepartment(text(body.get("department")));
        user.setSubject(text(body.get("subject")));
        user.setStaffCode(text(body.get("staffCode")));
        user.setRole(parseRole(text(body.get("role"))));
        user.setActive(true);
        String password = text(body.get("password"));
        if (password.isBlank()) {
            password = "password123";
        }
        user.setPasswordHash(encoder.encode(password));
        users.save(user);
        NotificationPreference pref = new NotificationPreference();
        pref.setUser(user);
        pref.setInAppOn(true);
        preferences.save(pref);
        audit.record(me, "USER_CREATE", user.getUsername(), request);
        return Views.user(user);
    }

    @Transactional
    public Map<String, Object> update(Long id, Map<String, Object> body, HttpServletRequest request) {
        UserAccount me = current.get();
        Access.requireAdmin(me);
        UserAccount user = users.findById(id).orElseThrow(() -> new ApiException(404, "User not found."));
        if (body.get("fullName") != null) user.setFullName(text(body.get("fullName")));
        if (body.get("email") != null) user.setEmail(text(body.get("email")));
        if (body.get("phone") != null) user.setPhone(text(body.get("phone")));
        if (body.get("department") != null) user.setDepartment(text(body.get("department")));
        if (body.get("subject") != null) user.setSubject(text(body.get("subject")));
        if (body.get("staffCode") != null) user.setStaffCode(text(body.get("staffCode")));
        if (body.get("role") != null) user.setRole(parseRole(text(body.get("role"))));
        if (body.get("active") != null) {
            boolean active = Boolean.parseBoolean(String.valueOf(body.get("active")));
            if (!active && user.getId().equals(me.getId())) {
                throw new ApiException(400, "You cannot deactivate your own account.");
            }
            user.setActive(active);
        }
        user.setUpdatedAt(Instant.now());
        users.save(user);
        audit.record(me, "USER_UPDATE", user.getUsername(), request);
        return Views.user(user);
    }

    @Transactional
    public Map<String, Object> resetPassword(Long id, HttpServletRequest request) {
        UserAccount me = current.get();
        Access.requireAdmin(me);
        UserAccount user = users.findById(id).orElseThrow(() -> new ApiException(404, "User not found."));
        user.setPasswordHash(encoder.encode("password123"));
        users.save(user);
        audit.record(me, "PASSWORD_RESET", user.getUsername(), request);
        return Maps.of("username", user.getUsername(), "password", "password123");
    }

    public Map<String, Object> profile(Long id) {
        UserAccount me = current.get();
        UserAccount teacher = users.findById(id).orElseThrow(() -> new ApiException(404, "Teacher not found."));
        if (me.getRole() == Enums.Role.TEACHER && !me.getId().equals(teacher.getId())) {
            throw new ApiException(403, "You can only open your own profile.");
        }
        List<Map<String, Object>> history = attendance.history(id);
        int present = 0, absent = 0, late = 0, leave = 0;
        for (Map<String, Object> row : history) {
            switch (String.valueOf(row.get("status"))) {
                case "PRESENT": present++; break;
                case "ABSENT": absent++; break;
                case "LATE": late++; break;
                case "ON_LEAVE": leave++; break;
                default: break;
            }
        }
        List<Map<String, Object>> docs = new ArrayList<>();
        for (SchoolDocument doc : documents.findByOwnerIdOrderByUpdatedAtDesc(teacher.getId())) {
            docs.add(Views.document(doc));
        }
        List<Map<String, Object>> recent = history.size() > 12 ? history.subList(0, 12) : history;
        return Maps.of(
                "teacher", Views.user(teacher),
                "summary", Maps.of("present", present, "absent", absent, "late", late, "onLeave", leave, "total", history.size()),
                "attendance", recent,
                "documents", docs
        );
    }

    private Enums.Role parseRole(String role) {
        try {
            return Enums.Role.valueOf(role.toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (Exception ex) {
            throw new ApiException(400, "Role must be ADMIN, HEAD_OF_STUDIES, or TEACHER.");
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
