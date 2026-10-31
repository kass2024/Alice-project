package com.alice.smartteacher.service;

import com.alice.smartteacher.model.ApprovalLog;
import com.alice.smartteacher.model.AttendanceRecord;
import com.alice.smartteacher.model.DocumentVersion;
import com.alice.smartteacher.model.LoginSession;
import com.alice.smartteacher.model.Notification;
import com.alice.smartteacher.model.SchoolDocument;
import com.alice.smartteacher.model.TeachingSession;
import com.alice.smartteacher.model.UserAccount;

public final class Views {
    private Views() {}

    public static java.util.Map<String, Object> user(UserAccount u) {
        return Maps.of(
                "id", u.getId(),
                "fullName", u.getFullName(),
                "username", u.getUsername(),
                "email", u.getEmail(),
                "role", u.getRole().name(),
                "active", u.isActive(),
                "phone", u.getPhone(),
                "department", u.getDepartment(),
                "subject", u.getSubject(),
                "staffCode", u.getStaffCode(),
                "hasFace", u.getFaceDescriptor() != null && !u.getFaceDescriptor().isBlank(),
                "createdAt", u.getCreatedAt()
        );
    }

    public static java.util.Map<String, Object> attendance(AttendanceRecord r) {
        UserAccount t = r.getTeacher();
        return Maps.of(
                "id", r.getId(),
                "teacherId", t.getId(),
                "teacherName", t.getFullName(),
                "department", t.getDepartment(),
                "subject", t.getSubject(),
                "staffCode", t.getStaffCode(),
                "date", r.getAttendDate(),
                "status", r.getStatus().name(),
                "checkIn", r.getCheckIn(),
                "checkOut", r.getCheckOut(),
                "method", r.getMethod().name(),
                "note", r.getNote(),
                "markedBy", r.getMarkedBy() == null ? null : r.getMarkedBy().getFullName(),
                "faceDistance", r.getFaceDistance()
        );
    }

    public static java.util.Map<String, Object> document(SchoolDocument d) {
        return Maps.of(
                "id", d.getId(),
                "title", d.getTitle(),
                "category", d.getCategory().name(),
                "track", d.getTrack().name(),
                "status", d.getStatus().name(),
                "originalName", d.getOriginalName(),
                "contentType", d.getContentType(),
                "version", d.getVersion(),
                "ownerId", d.getOwner().getId(),
                "ownerName", d.getOwner().getFullName(),
                "reviewerComment", d.getReviewerComment(),
                "routedTo", d.getRoutedTo(),
                "hasAnalysis", d.getAnalysisJson() != null && !d.getAnalysisJson().isBlank(),
                "createdAt", d.getCreatedAt(),
                "updatedAt", d.getUpdatedAt()
        );
    }

    public static java.util.Map<String, Object> version(DocumentVersion v) {
        return Maps.of(
                "id", v.getId(),
                "version", v.getVersionNumber(),
                "originalName", v.getOriginalName(),
                "note", v.getNote(),
                "createdAt", v.getCreatedAt()
        );
    }

    public static java.util.Map<String, Object> approval(ApprovalLog log) {
        return Maps.of(
                "id", log.getId(),
                "action", log.getAction(),
                "comment", log.getComment(),
                "reviewer", log.getReviewer().getFullName(),
                "createdAt", log.getCreatedAt()
        );
    }

    public static java.util.Map<String, Object> session(TeachingSession s) {
        return Maps.of(
                "id", s.getId(),
                "teacherId", s.getTeacher().getId(),
                "teacherName", s.getTeacher().getFullName(),
                "department", s.getTeacher().getDepartment(),
                "className", s.getClassName(),
                "subject", s.getSubject(),
                "room", s.getRoom(),
                "date", s.getSessionDate(),
                "startTime", s.getStartTime(),
                "endTime", s.getEndTime(),
                "actualStart", s.getActualStart(),
                "actualEnd", s.getActualEnd(),
                "status", s.getStatus().name(),
                "note", s.getNote()
        );
    }

    public static java.util.Map<String, Object> notification(Notification n) {
        return Maps.of(
                "id", n.getId(),
                "userId", n.getUser().getId(),
                "userName", n.getUser().getFullName(),
                "title", n.getTitle(),
                "message", n.getMessage(),
                "type", n.getType(),
                "priority", n.getPriority().name(),
                "read", n.isReadFlag(),
                "channel", n.getChannel(),
                "createdAt", n.getCreatedAt()
        );
    }

    public static java.util.Map<String, Object> loginSession(LoginSession s) {
        return Maps.of(
                "id", s.getId(),
                "userId", s.getUser().getId(),
                "userName", s.getUser().getFullName(),
                "username", s.getUser().getUsername(),
                "active", s.isActive(),
                "ipAddress", s.getIpAddress(),
                "userAgent", s.getUserAgent(),
                "createdAt", s.getCreatedAt(),
                "endedAt", s.getEndedAt()
        );
    }
}
