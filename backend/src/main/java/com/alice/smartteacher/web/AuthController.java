package com.alice.smartteacher.web;

import com.alice.smartteacher.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @GetMapping("/")
    public String home() {
        return "Smart Teacher API is running. Open http://localhost:5173";
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @PostMapping("/api/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        return auth.login(body.get("username"), body.get("password"), request);
    }

    @GetMapping("/api/auth/me")
    public Map<String, Object> me() {
        return auth.me();
    }

    @PostMapping("/api/auth/logout")
    public Map<String, Boolean> logout(HttpServletRequest request) {
        auth.logout(request);
        return Map.of("ok", true);
    }

    @PostMapping("/api/auth/password")
    public Map<String, Boolean> password(@RequestBody Map<String, String> body) {
        auth.changePassword(body.get("currentPassword"), body.get("newPassword"));
        return Map.of("ok", true);
    }
}
