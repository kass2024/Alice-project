package com.alice.smartteacher.service;

import com.alice.smartteacher.model.LoginSession;
import com.alice.smartteacher.model.NotificationPreference;
import com.alice.smartteacher.model.UserAccount;
import com.alice.smartteacher.repo.LoginSessionRepository;
import com.alice.smartteacher.repo.NotificationPreferenceRepository;
import com.alice.smartteacher.repo.UserRepository;
import com.alice.smartteacher.security.JwtService;
import com.alice.smartteacher.web.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository users;
    private final LoginSessionRepository sessions;
    private final NotificationPreferenceRepository preferences;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final CurrentUserService current;
    private final AuditService audit;

    public AuthService(UserRepository users, LoginSessionRepository sessions, NotificationPreferenceRepository preferences,
                       PasswordEncoder encoder, JwtService jwt, CurrentUserService current, AuditService audit) {
        this.users = users;
        this.sessions = sessions;
        this.preferences = preferences;
        this.encoder = encoder;
        this.jwt = jwt;
        this.current = current;
        this.audit = audit;
    }

    @Transactional
    public Map<String, Object> login(String username, String password, HttpServletRequest request) {
        if (username == null || password == null) {
            throw new ApiException(400, "Enter your username and password.");
        }
        UserAccount user = users.findByUsername(username.trim()).orElseThrow(() -> new ApiException(401, "Wrong username or password."));
        if (!user.isActive()) {
            throw new ApiException(403, "This account is deactivated. Ask the admin.");
        }
        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(401, "Wrong username or password.");
        }
        LoginSession session = new LoginSession();
        session.setUser(user);
        session.setActive(true);
        if (request != null) {
            session.setIpAddress(request.getRemoteAddr());
            String agent = request.getHeader("User-Agent");
            session.setUserAgent(agent == null ? "" : agent.substring(0, Math.min(agent.length(), 280)));
        }
        sessions.save(session);
        preferences.findByUserId(user.getId()).orElseGet(() -> {
            NotificationPreference pref = new NotificationPreference();
            pref.setUser(user);
            pref.setInAppOn(true);
            return preferences.save(pref);
        });
        String token = jwt.create(user.getId(), session.getId(), user.getRole().name());
        audit.record(user, "LOGIN", "Signed in", request);
        return Maps.of("token", token, "user", Views.user(user));
    }

    public Map<String, Object> me() {
        return Views.user(current.get());
    }

    @Transactional
    public void logout(HttpServletRequest request) {
        UserAccount user = current.get();
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Long sid = jwt.read(header.substring(7).trim()).get("sid", Number.class).longValue();
                sessions.findById(sid).ifPresent(session -> {
                    session.setActive(false);
                    session.setEndedAt(Instant.now());
                    sessions.save(session);
                });
            } catch (Exception ignored) {
                // The token is already unusable. Logging out should still succeed.
            }
        }
        audit.record(user, "LOGOUT", "Signed out", request);
    }

    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        UserAccount user = current.get();
        if (newPassword == null || newPassword.length() < 6) {
            throw new ApiException(400, "Use at least 6 characters.");
        }
        if (!encoder.matches(currentPassword == null ? "" : currentPassword, user.getPasswordHash())) {
            throw new ApiException(400, "The current password is wrong.");
        }
        user.setPasswordHash(encoder.encode(newPassword));
        user.setUpdatedAt(Instant.now());
        users.save(user);
    }
}
