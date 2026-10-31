package com.alice.smartteacher.service;

import com.alice.smartteacher.model.AuditLog;
import com.alice.smartteacher.model.UserAccount;
import com.alice.smartteacher.repo.AuditLogRepository;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
public class AuditService {
    private final AuditLogRepository logs;

    public AuditService(AuditLogRepository logs) {
        this.logs = logs;
    }

    public void record(UserAccount user, String action, String detail, HttpServletRequest request) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setDetail(detail);
        if (request != null) {
            log.setIpAddress(request.getRemoteAddr());
        }
        logs.save(log);
    }
}
