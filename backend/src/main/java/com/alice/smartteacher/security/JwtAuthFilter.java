package com.alice.smartteacher.security;

import com.alice.smartteacher.model.LoginSession;
import com.alice.smartteacher.model.UserAccount;
import com.alice.smartteacher.repo.LoginSessionRepository;
import com.alice.smartteacher.repo.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository users;
    private final LoginSessionRepository sessions;

    public JwtAuthFilter(JwtService jwtService, UserRepository users, LoginSessionRepository sessions) {
        this.jwtService = jwtService;
        this.users = users;
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        try {
            Claims claims = jwtService.read(header.substring(7).trim());
            Long userId = Long.valueOf(claims.getSubject());
            Long sessionId = claims.get("sid", Number.class).longValue();
            Optional<UserAccount> found = users.findById(userId);
            Optional<LoginSession> session = sessions.findById(sessionId);
            if (found.isEmpty() || !found.get().isActive() || session.isEmpty() || !session.get().isActive()) {
                reject(response);
                return;
            }
            AppUserDetails details = new AppUserDetails(found.get());
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(request, response);
        } catch (Exception ex) {
            reject(response);
        }
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Please log in again.\"}");
    }
}
