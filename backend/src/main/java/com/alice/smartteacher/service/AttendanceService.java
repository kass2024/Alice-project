package com.alice.smartteacher.service;

import com.alice.smartteacher.model.AttendanceRecord;
import com.alice.smartteacher.model.Enums;
import com.alice.smartteacher.model.UserAccount;
import com.alice.smartteacher.repo.AttendanceRepository;
import com.alice.smartteacher.repo.UserRepository;
import com.alice.smartteacher.web.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AttendanceService {

    private final AttendanceRepository attendance;
    private final UserRepository users;
    private final CurrentUserService current;
    private final NotificationService notifications;
    private final AuditService audit;
    private final FileStore files;
    private final SettingService settings;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.late-after:08:15}")
    private String lateAfter;

    @Value("${app.face-threshold:0.50}")
    private double faceThreshold;

    public AttendanceService(AttendanceRepository attendance, UserRepository users, CurrentUserService current,
                             NotificationService notifications, AuditService audit, FileStore files, SettingService settings) {
        this.attendance = attendance;
        this.users = users;
        this.current = current;
        this.notifications = notifications;
        this.audit = audit;
        this.files = files;
        this.settings = settings;
    }

    public List<UserAccount> teachers(String department) {
        List<UserAccount> list = users.findByRoleAndActiveTrueOrderByFullNameAsc(Enums.Role.TEACHER);
        if (department == null || department.isBlank() || "ALL".equalsIgnoreCase(department)) {
            return list;
        }
        List<UserAccount> filtered = new ArrayList<>();
        for (UserAccount teacher : list) {
            if (department.equalsIgnoreCase(teacher.getDepartment())) {
                filtered.add(teacher);
            }
        }
        return filtered;
    }

    public Map<String, Object> sheet(String dateText, String department) {
        UserAccount me = current.get();
        LocalDate date = parseDate(dateText);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (UserAccount teacher : visibleTeachers(me, department)) {
            AttendanceRecord record = attendance.findByTeacherIdAndAttendDate(teacher.getId(), date).orElse(null);
            rows.add(Maps.of("teacher", Views.user(teacher), "record", record == null ? null : Views.attendance(record)));
        }
        return Maps.of("date", date, "rows", rows, "summary", summary(date, date, department, me));
    }

    public Map<String, Object> range(String fromText, String toText, String department, String status, String q, String method) {
        UserAccount me = current.get();
        LocalDate from = fromText == null || fromText.isBlank() ? SchoolClock.today().minusDays(14) : LocalDate.parse(fromText);
        LocalDate to = toText == null || toText.isBlank() ? SchoolClock.today() : LocalDate.parse(toText);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AttendanceRecord record : attendance.findByAttendDateBetweenOrderByAttendDateDesc(from, to)) {
            if (!canSee(me, record.getTeacher())) {
                continue;
            }
            if (!match(record, department, status, q, method)) {
                continue;
            }
            rows.add(Views.attendance(record));
        }
        return Maps.of("from", from, "to", to, "records", rows, "summary", summary(from, to, department, me));
    }

    public List<Map<String, Object>> history(Long teacherId) {
        UserAccount me = current.get();
        UserAccount teacher = users.findById(teacherId).orElseThrow(() -> new ApiException(404, "Teacher not found."));
        if (!canSee(me, teacher)) {
            throw new ApiException(403, "You can only open your own attendance.");
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AttendanceRecord record : attendance.findByTeacherIdOrderByAttendDateDesc(teacherId)) {
            rows.add(Views.attendance(record));
        }
        return rows;
    }

    public List<Map<String, Object>> biometricLogs() {
        UserAccount me = current.get();
        List<Map<String, Object>> rows = new ArrayList<>();
        List<Enums.MarkMethod> methods = java.util.List.of(Enums.MarkMethod.FACE, Enums.MarkMethod.OVERRIDE);
        for (AttendanceRecord record : attendance.findTop40ByMethodInOrderByAttendDateDesc(methods)) {
            if (!canSee(me, record.getTeacher())) {
                continue;
            }
            rows.add(Views.attendance(record));
        }
        return rows;
    }

    @Transactional
    public Map<String, Object> markOne(Long teacherId, String dateText, String status, String note, HttpServletRequest request) {
        UserAccount me = current.get();
        Access.requireManager(me);
        AttendanceRecord saved = upsert(teacher(teacherId), parseDate(dateText), parseStatus(status), note, Enums.MarkMethod.MANUAL, me, null);
        audit.record(me, "ATTENDANCE_MARK", saved.getTeacher().getFullName() + " " + saved.getStatus(), request);
        return Views.attendance(saved);
    }

    @Transactional
    public int bulk(String dateText, List<Map<String, Object>> rows, HttpServletRequest request) {
        UserAccount me = current.get();
        Access.requireManager(me);
        LocalDate date = parseDate(dateText);
        int count = 0;
        if (rows == null) {
            return 0;
        }
        for (Map<String, Object> row : rows) {
            if (row.get("teacherId") == null || row.get("status") == null) {
                continue;
            }
            String status = String.valueOf(row.get("status"));
            if (status.isBlank()) {
                continue;
            }
            Long teacherId = Long.valueOf(String.valueOf(row.get("teacherId")));
            Enums.AttendanceStatus next = parseStatus(status);
            AttendanceRecord existing = attendance.findByTeacherIdAndAttendDate(teacherId, date).orElse(null);
            if (existing != null && existing.getMethod() == Enums.MarkMethod.FACE && existing.getStatus() == next) {
                count++;
                continue;
            }
            String note = row.get("note") == null ? null : String.valueOf(row.get("note"));
            upsert(teacher(teacherId), date, next, note, Enums.MarkMethod.BULK, me, null);
            count++;
        }
        audit.record(me, "ATTENDANCE_BULK", count + " teachers on " + date, request);
        return count;
    }

    @Transactional
    public Map<String, Object> override(Long teacherId, String dateText, String status, String note, HttpServletRequest request) {
        UserAccount me = current.get();
        Access.requireManager(me);
        String fullNote = note == null || note.isBlank() ? "Manual override" : "Manual override: " + note;
        AttendanceRecord saved = upsert(teacher(teacherId), parseDate(dateText), parseStatus(status), fullNote, Enums.MarkMethod.OVERRIDE, me, null);
        audit.record(me, "ATTENDANCE_OVERRIDE", saved.getTeacher().getFullName() + " " + saved.getStatus(), request);
        return Views.attendance(saved);
    }

    @Transactional
    public Map<String, Object> face(List<?> descriptor, String action) {
        UserAccount me = current.get();
        if (me.getRole() == Enums.Role.TEACHER && !settings.enabled("teacher.biometric")) {
            throw new ApiException(403, "Face check-in is turned off in security settings.");
        }
        double[] probe = toArray(descriptor);
        UserAccount best = null;
        double bestDistance = 99;
        for (UserAccount teacher : users.findByRoleAndActiveTrueOrderByFullNameAsc(Enums.Role.TEACHER)) {
            double[] known = readDescriptor(teacher.getFaceDescriptor());
            if (known == null) {
                continue;
            }
            if (known.length != probe.length) {
                continue;
            }
            double distance = distance(probe, known);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = teacher;
            }
        }
        if (best == null) {
            return Maps.of("matched", false, "message", "No teacher has enrolled a face yet.");
        }
        boolean samePerson = bestDistance <= faceThreshold;
        if (me.getRole() == Enums.Role.TEACHER && !me.getId().equals(best.getId())) {
            samePerson = false;
        }
        if (!samePerson) {
            return Maps.of(
                    "matched", false,
                    "distance", round(bestDistance),
                    "message", "Face not recognised. Move closer to the camera, or ask an admin to use manual override."
            );
        }
        LocalDate today = SchoolClock.today();
        LocalDateTime now = SchoolClock.now();
        AttendanceRecord record = attendance.findByTeacherIdAndAttendDate(best.getId(), today).orElseGet(AttendanceRecord::new);
        if (record.getStatus() == Enums.AttendanceStatus.ON_LEAVE) {
            return Maps.of("matched", true, "action", "ON_LEAVE", "distance", round(bestDistance), "teacher", Views.user(best),
                    "record", record.getId() == null ? null : Views.attendance(record),
                    "message", best.getFullName() + " is on leave today, so the face scan did not change the register.");
        }
        record.setTeacher(best);
        record.setAttendDate(today);
        record.setMethod(Enums.MarkMethod.FACE);
        record.setFaceDistance(round(bestDistance));
        record.setMarkedBy(me);
        boolean auto = action == null || action.isBlank() || "AUTO".equalsIgnoreCase(action);
        if (auto && record.getCheckIn() != null && record.getCheckOut() != null) {
            return Maps.of("matched", true, "action", "DONE", "distance", round(bestDistance), "teacher", Views.user(best),
                    "record", Views.attendance(record),
                    "message", best.getFullName() + " already checked in and out today.");
        }
        boolean leaving = "CHECK_OUT".equalsIgnoreCase(action) || (auto && record.getCheckIn() != null);
        String message;
        String actionName;
        String clock = now.toLocalTime().withNano(0).toString();
        if (clock.length() >= 5) {
            clock = clock.substring(0, 5);
        }
        if (leaving) {
            if (record.getCheckIn() == null) {
                throw new ApiException(400, "Check in first.");
            }
            record.setCheckOut(now);
            if (record.getStatus() == null) {
                record.setStatus(Enums.AttendanceStatus.PRESENT);
            }
            actionName = "CHECK_OUT";
            message = best.getFullName() + " checked out at " + clock + ".";
        } else {
            if (record.getCheckIn() != null) {
                return Maps.of("matched", true, "action", "DONE", "distance", round(bestDistance), "teacher", Views.user(best),
                        "record", Views.attendance(record), "message", "Already checked in at " + record.getCheckIn().toLocalTime());
            }
            record.setCheckIn(now);
            record.setStatus(now.toLocalTime().isAfter(LocalTime.parse(lateAfter))
                    ? Enums.AttendanceStatus.LATE : Enums.AttendanceStatus.PRESENT);
            actionName = "CHECK_IN";
            message = best.getFullName() + " checked in at " + clock + " as " + record.getStatus().name().toLowerCase(Locale.ROOT).replace('_', ' ') + ".";
            if (record.getStatus() == Enums.AttendanceStatus.LATE) {
                notifications.notify(best, "You arrived late", "Face check-in at " + clock + " is after " + lateAfter + ".",
                        "ATTENDANCE", Enums.Priority.NORMAL);
            }
        }
        AttendanceRecord saved = attendance.save(record);
        return Maps.of("matched", true, "action", actionName, "distance", round(bestDistance), "teacher", Views.user(best),
                "record", Views.attendance(saved), "message", message);
    }

    @Transactional
    public void enroll(Long teacherId, String descriptorJson, MultipartFile photo, HttpServletRequest request) {
        UserAccount me = current.get();
        UserAccount teacher = teacher(teacherId);
        if (me.getRole() == Enums.Role.TEACHER && !me.getId().equals(teacher.getId())) {
            throw new ApiException(403, "You can only enroll your own face.");
        }
        if (me.getRole() == Enums.Role.HEAD_OF_STUDIES) {
            throw new ApiException(403, "A teacher or an admin enrolls the face.");
        }
        double[] values = readDescriptor(descriptorJson);
        if (values == null) {
            throw new ApiException(400, "The face photo could not be read. Enroll again from the Android attendance app.");
        }
        if (photo != null && !photo.isEmpty()) {
            String type = photo.getContentType() == null ? "" : photo.getContentType();
            if (!type.startsWith("image/")) {
                throw new ApiException(400, "The face photo must be an image.");
            }
            teacher.setPhotoPath(files.save(photo).storedName);
        }
        teacher.setFaceDescriptor(descriptorJson);
        teacher.setUpdatedAt(SchoolClock.now().atZone(SchoolClock.ZONE).toInstant());
        users.save(teacher);
        audit.record(me, "FACE_ENROLL", teacher.getFullName(), request);
    }

    @Transactional
    public void clearFace(Long teacherId, HttpServletRequest request) {
        UserAccount me = current.get();
        Access.requireAdmin(me);
        UserAccount teacher = teacher(teacherId);
        teacher.setFaceDescriptor(null);
        teacher.setPhotoPath(null);
        users.save(teacher);
        audit.record(me, "FACE_CLEAR", teacher.getFullName(), request);
    }

    public UserAccount photoOwner(Long teacherId) {
        current.get();
        return teacher(teacherId);
    }

    private AttendanceRecord upsert(UserAccount teacher, LocalDate date, Enums.AttendanceStatus status, String note,
                                    Enums.MarkMethod method, UserAccount actor, Double distance) {
        AttendanceRecord record = attendance.findByTeacherIdAndAttendDate(teacher.getId(), date).orElseGet(AttendanceRecord::new);
        boolean wasAbsent = record.getStatus() == Enums.AttendanceStatus.ABSENT;
        record.setTeacher(teacher);
        record.setAttendDate(date);
        record.setStatus(status);
        record.setMethod(method);
        record.setNote(note);
        record.setMarkedBy(actor);
        record.setFaceDistance(distance);
        if (status == Enums.AttendanceStatus.PRESENT || status == Enums.AttendanceStatus.LATE) {
            if (record.getCheckIn() == null && date.equals(SchoolClock.today())) {
                record.setCheckIn(SchoolClock.now());
            }
        }
        AttendanceRecord saved = attendance.save(record);
        if (status == Enums.AttendanceStatus.ABSENT && !wasAbsent) {
            notifications.notify(teacher, "Marked absent", "You are marked absent on " + date + ".", "ATTENDANCE", Enums.Priority.HIGH);
        }
        return saved;
    }

    private Map<String, Object> summary(LocalDate from, LocalDate to, String department, UserAccount me) {
        int present = 0, absent = 0, late = 0, leave = 0;
        for (AttendanceRecord record : attendance.findByAttendDateBetweenOrderByAttendDateDesc(from, to)) {
            if (!canSee(me, record.getTeacher())) {
                continue;
            }
            if (department != null && !department.isBlank() && !"ALL".equalsIgnoreCase(department)
                    && !department.equalsIgnoreCase(record.getTeacher().getDepartment())) {
                continue;
            }
            switch (record.getStatus()) {
                case PRESENT: present++; break;
                case ABSENT: absent++; break;
                case LATE: late++; break;
                case ON_LEAVE: leave++; break;
                default: break;
            }
        }
        return Maps.of("present", present, "absent", absent, "late", late, "onLeave", leave);
    }

    private List<UserAccount> visibleTeachers(UserAccount me, String department) {
        if (me.getRole() == Enums.Role.TEACHER) {
            return List.of(me);
        }
        return teachers(department);
    }

    private boolean canSee(UserAccount me, UserAccount teacher) {
        return me.getRole() != Enums.Role.TEACHER || me.getId().equals(teacher.getId());
    }

    private boolean match(AttendanceRecord record, String department, String status, String q, String method) {
        if (department != null && !department.isBlank() && !"ALL".equalsIgnoreCase(department)
                && !department.equalsIgnoreCase(record.getTeacher().getDepartment())) {
            return false;
        }
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)
                && !record.getStatus().name().equalsIgnoreCase(status)) {
            return false;
        }
        if (method != null && !method.isBlank() && !"ALL".equalsIgnoreCase(method)
                && !record.getMethod().name().equalsIgnoreCase(method)) {
            return false;
        }
        if (q != null && !q.isBlank()) {
            String hay = (record.getTeacher().getFullName() + " " + record.getTeacher().getDepartment()
                    + " " + record.getNote()).toLowerCase(Locale.ROOT);
            if (!hay.contains(q.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    private UserAccount teacher(Long id) {
        UserAccount user = users.findById(id).orElseThrow(() -> new ApiException(404, "Teacher not found."));
        if (user.getRole() != Enums.Role.TEACHER) {
            throw new ApiException(400, "That account is not a teacher.");
        }
        return user;
    }

    private LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return SchoolClock.today();
        }
        return LocalDate.parse(text);
    }

    private Enums.AttendanceStatus parseStatus(String status) {
        try {
            return Enums.AttendanceStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new ApiException(400, "Status must be PRESENT, ABSENT, LATE, or ON_LEAVE.");
        }
    }

    private double[] toArray(List<?> values) {
        if (values == null || values.size() < 64 || values.size() > 512) {
            throw new ApiException(400, "Face data is incomplete. Take the photo again in the Android app.");
        }
        double[] out = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Object item = values.get(i);
            out[i] = item instanceof Number ? ((Number) item).doubleValue() : 0;
        }
        return out;
    }

    private double[] readDescriptor(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            double[] values = mapper.readValue(json, double[].class);
            return values.length >= 64 && values.length <= 512 ? values : null;
        } catch (Exception ex) {
            return null;
        }
    }

    /** Euclidean distance between two face signatures. Lower means more similar. */
    private double distance(double[] a, double[] b) {
        double sum = 0;
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            double d = a[i] - b[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    public List<AttendanceRecord> between(LocalDate from, LocalDate to) {
        List<AttendanceRecord> rows = new ArrayList<>(attendance.findByAttendDateBetweenOrderByAttendDateDesc(from, to));
        rows.sort(Comparator.comparing(AttendanceRecord::getAttendDate));
        return rows;
    }
}
