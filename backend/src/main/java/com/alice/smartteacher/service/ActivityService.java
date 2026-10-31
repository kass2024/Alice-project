package com.alice.smartteacher.service;

import com.alice.smartteacher.model.Enums;
import com.alice.smartteacher.model.TeachingSession;
import com.alice.smartteacher.model.UserAccount;
import com.alice.smartteacher.repo.TeachingSessionRepository;
import com.alice.smartteacher.repo.UserRepository;
import com.alice.smartteacher.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ActivityService {

    private final TeachingSessionRepository sessions;
    private final UserRepository users;
    private final CurrentUserService current;
    private final AuditService audit;

    public ActivityService(TeachingSessionRepository sessions, UserRepository users,
                           CurrentUserService current, AuditService audit) {
        this.sessions = sessions;
        this.users = users;
        this.current = current;
        this.audit = audit;
    }

    @Transactional
    public Map<String, Object> list(String fromText, String toText, Long teacherId, String status) {
        UserAccount me = current.get();
        LocalDate from = fromText == null || fromText.isBlank() ? SchoolClock.today().minusDays(7) : LocalDate.parse(fromText);
        LocalDate to = toText == null || toText.isBlank() ? SchoolClock.today().plusDays(7) : LocalDate.parse(toText);
        List<TeachingSession> source = me.getRole() == Enums.Role.TEACHER
                ? sessions.findByTeacherIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(me.getId(), from, to)
                : sessions.findBySessionDateBetweenOrderBySessionDateAscStartTimeAsc(from, to);
        refresh(source);
        List<Map<String, Object>> rows = new ArrayList<>();
        int onSchedule = 0, delayed = 0, missed = 0, completed = 0, scheduled = 0;
        for (TeachingSession session : source) {
            if (teacherId != null && !teacherId.equals(session.getTeacher().getId())) {
                continue;
            }
            if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)
                    && !session.getStatus().name().equalsIgnoreCase(status)) {
                continue;
            }
            switch (session.getStatus()) {
                case ON_SCHEDULE: onSchedule++; break;
                case DELAYED: delayed++; break;
                case MISSED: missed++; break;
                case COMPLETED: completed++; break;
                default: scheduled++; break;
            }
            rows.add(Views.session(session));
        }
        return Maps.of("from", from, "to", to, "sessions", rows, "stats", Maps.of(
                "onSchedule", onSchedule, "delayed", delayed, "missed", missed,
                "completed", completed, "scheduled", scheduled, "total", rows.size()
        ));
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body, HttpServletRequest request) {
        UserAccount me = current.get();
        Access.requireManager(me);
        Long teacherId = Long.valueOf(String.valueOf(body.get("teacherId")));
        UserAccount teacher = users.findById(teacherId).orElseThrow(() -> new ApiException(404, "Teacher not found."));
        TeachingSession session = new TeachingSession();
        session.setTeacher(teacher);
        session.setClassName(required(body.get("className"), "Class name"));
        session.setSubject(required(body.get("subject"), "Subject"));
        session.setRoom(body.get("room") == null ? "" : String.valueOf(body.get("room")));
        session.setSessionDate(LocalDate.parse(required(body.get("date"), "Date")));
        session.setStartTime(LocalTime.parse(required(body.get("startTime"), "Start time")));
        session.setEndTime(LocalTime.parse(required(body.get("endTime"), "End time")));
        session.setStatus(Enums.ActivityStatus.SCHEDULED);
        session.setNote(body.get("note") == null ? null : String.valueOf(body.get("note")));
        sessions.save(session);
        audit.record(me, "SESSION_CREATE", teacher.getFullName() + " " + session.getClassName(), request);
        return Views.session(session);
    }

    @Transactional
    public Map<String, Object> start(Long id) {
        TeachingSession session = ownOrManager(id);
        if (session.getActualStart() != null) {
            throw new ApiException(400, "This class has already started.");
        }
        LocalDateTime now = SchoolClock.now();
        session.setActualStart(now);
        LocalDateTime planned = LocalDateTime.of(session.getSessionDate(), session.getStartTime()).plusMinutes(10);
        session.setStatus(now.isAfter(planned) ? Enums.ActivityStatus.DELAYED : Enums.ActivityStatus.ON_SCHEDULE);
        sessions.save(session);
        return Views.session(session);
    }

    @Transactional
    public Map<String, Object> end(Long id) {
        TeachingSession session = ownOrManager(id);
        if (session.getActualStart() == null) {
            throw new ApiException(400, "Start the class first.");
        }
        session.setActualEnd(SchoolClock.now());
        session.setStatus(Enums.ActivityStatus.COMPLETED);
        sessions.save(session);
        return Views.session(session);
    }

    public List<TeachingSession> between(LocalDate from, LocalDate to) {
        return sessions.findBySessionDateBetweenOrderBySessionDateAscStartTimeAsc(from, to);
    }

    private void refresh(List<TeachingSession> source) {
        LocalDateTime now = SchoolClock.now();
        for (TeachingSession session : source) {
            if (session.getStatus() != Enums.ActivityStatus.SCHEDULED) {
                continue;
            }
            LocalDateTime end = LocalDateTime.of(session.getSessionDate(), session.getEndTime());
            if (session.getActualStart() == null && end.isBefore(now)) {
                session.setStatus(Enums.ActivityStatus.MISSED);
                sessions.save(session);
            }
        }
    }

    private TeachingSession ownOrManager(Long id) {
        UserAccount me = current.get();
        TeachingSession session = sessions.findById(id).orElseThrow(() -> new ApiException(404, "Class session not found."));
        if (me.getRole() == Enums.Role.TEACHER && !session.getTeacher().getId().equals(me.getId())) {
            throw new ApiException(403, "You can only update your own class.");
        }
        return session;
    }

    private String required(Object value, String label) {
        if (value == null || String.valueOf(value).isBlank()) {
            throw new ApiException(400, label + " is required.");
        }
        return String.valueOf(value).trim();
    }

    public String statusLabel(Enums.ActivityStatus status) {
        return status.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
