package com.alice.smartteacher.config;

import com.alice.smartteacher.model.AcademicRecord;
import com.alice.smartteacher.model.ApprovalLog;
import com.alice.smartteacher.model.AttendanceRecord;
import com.alice.smartteacher.model.DocumentVersion;
import com.alice.smartteacher.model.Enums;
import com.alice.smartteacher.model.Notification;
import com.alice.smartteacher.model.NotificationPreference;
import com.alice.smartteacher.model.SchoolDocument;
import com.alice.smartteacher.model.TeachingSession;
import com.alice.smartteacher.model.UserAccount;
import com.alice.smartteacher.repo.AcademicRecordRepository;
import com.alice.smartteacher.repo.ApprovalLogRepository;
import com.alice.smartteacher.repo.AttendanceRepository;
import com.alice.smartteacher.repo.DocumentRepository;
import com.alice.smartteacher.repo.DocumentVersionRepository;
import com.alice.smartteacher.repo.NotificationPreferenceRepository;
import com.alice.smartteacher.repo.NotificationRepository;
import com.alice.smartteacher.repo.TeachingSessionRepository;
import com.alice.smartteacher.repo.UserRepository;
import com.alice.smartteacher.service.FileStore;
import com.alice.smartteacher.service.SchoolClock;
import com.alice.smartteacher.service.SettingService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
public class DataSeeder implements ApplicationRunner {

    private final UserRepository users;
    private final AttendanceRepository attendance;
    private final DocumentRepository documents;
    private final DocumentVersionRepository versions;
    private final ApprovalLogRepository approvals;
    private final TeachingSessionRepository sessions;
    private final NotificationRepository notifications;
    private final NotificationPreferenceRepository preferences;
    private final AcademicRecordRepository academic;
    private final PasswordEncoder encoder;
    private final FileStore files;
    private final SettingService settings;

    public DataSeeder(UserRepository users, AttendanceRepository attendance, DocumentRepository documents,
                      DocumentVersionRepository versions, ApprovalLogRepository approvals,
                      TeachingSessionRepository sessions, NotificationRepository notifications,
                      NotificationPreferenceRepository preferences, AcademicRecordRepository academic,
                      PasswordEncoder encoder, FileStore files, SettingService settings) {
        this.users = users;
        this.attendance = attendance;
        this.documents = documents;
        this.versions = versions;
        this.approvals = approvals;
        this.sessions = sessions;
        this.notifications = notifications;
        this.preferences = preferences;
        this.academic = academic;
        this.encoder = encoder;
        this.files = files;
        this.settings = settings;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            ready();
            return;
        }
        String hash = encoder.encode("password123");
        UserAccount admin = account("admin", "School Admin", "admin@school.rw", hash, Enums.Role.ADMIN, "Administration", "", "A-001", "0788000001");
        UserAccount hos = account("patrick", "Patrick Ndayisaba", "patrick@school.rw", hash, Enums.Role.HEAD_OF_STUDIES, "Head of Studies", "", "H-001", "0788000002");
        UserAccount alice = account("alice", "Alice Uwimana", "alice@school.rw", hash, Enums.Role.TEACHER, "Sciences", "Mathematics", "T-001", "0788111001");
        UserAccount jean = account("jean", "Jean Bosco Hakizimana", "jean@school.rw", hash, Enums.Role.TEACHER, "Languages", "English", "T-002", "0788111002");
        UserAccount marie = account("marie", "Marie Claire Ingabire", "marie@school.rw", hash, Enums.Role.TEACHER, "Sciences", "Biology", "T-003", "0788111003");
        UserAccount eric = account("eric", "Eric Habimana", "eric@school.rw", hash, Enums.Role.TEACHER, "Humanities", "History", "T-004", "0788111004");
        UserAccount divine = account("divine", "Divine Mukamana", "divine@school.rw", hash, Enums.Role.TEACHER, "ICT", "Computer Science", "T-005", "0788111005");
        UserAccount grace = account("grace", "Grace Uwase", "grace@school.rw", hash, Enums.Role.TEACHER, "Sciences", "Physics", "T-006", "0788111006");
        List<UserAccount> teachers = List.of(alice, jean, marie, eric, divine, grace);

        LocalDate today = SchoolClock.today();
        mark(alice, today, Enums.AttendanceStatus.PRESENT, LocalTime.of(7, 55), Enums.MarkMethod.MANUAL, "On time", admin);
        mark(jean, today, Enums.AttendanceStatus.LATE, LocalTime.of(8, 27), Enums.MarkMethod.MANUAL, "Arrived after assembly", admin);
        mark(marie, today, Enums.AttendanceStatus.ABSENT, null, Enums.MarkMethod.MANUAL, "No check-in", admin);
        mark(eric, today, Enums.AttendanceStatus.ON_LEAVE, null, Enums.MarkMethod.OVERRIDE, "Sick leave approved by the Head of Studies", hos);
        mark(divine, today, Enums.AttendanceStatus.PRESENT, LocalTime.of(8, 5), Enums.MarkMethod.MANUAL, "", admin);
        mark(grace, today, Enums.AttendanceStatus.PRESENT, LocalTime.of(7, 50), Enums.MarkMethod.MANUAL, "", admin);

        LocalDate day = today.minusDays(1);
        int made = 0;
        while (made < 9) {
            if (day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY) {
                for (int i = 0; i < teachers.size(); i++) {
                    int n = (i + day.getDayOfMonth()) % 11;
                    Enums.AttendanceStatus status = Enums.AttendanceStatus.PRESENT;
                    LocalTime in = LocalTime.of(7, 50);
                    if (n == 0) {
                        status = Enums.AttendanceStatus.LATE;
                        in = LocalTime.of(8, 22);
                    } else if (n == 1) {
                        status = Enums.AttendanceStatus.ABSENT;
                        in = null;
                    }
                    mark(teachers.get(i), day, status, in, Enums.MarkMethod.BULK, "", admin);
                }
                made++;
            }
            day = day.minusDays(1);
        }

        doc(alice, "S2 Mathematics lesson plan", Enums.DocCategory.LESSON_PLAN, Enums.DocStatus.APPROVED, Enums.Track.REB,
                "Linear equations lesson plan.\nObjectives: solve simple equations.\nActivities: board examples and pair work.", hos, "Clear objectives.");
        doc(jean, "S2 English lesson plan", Enums.DocCategory.LESSON_PLAN, Enums.DocStatus.SUBMITTED, Enums.Track.REB,
                "Reading comprehension lesson.\nText: a short school story.\nAssessment: five questions.", null, null);
        doc(marie, "S3 Biology class register", Enums.DocCategory.CLASS_REGISTER, Enums.DocStatus.RETURNED, Enums.Track.REB,
                "Class register for S3 A.\nWeek 4 needs the absent names filled in.", hos, "Add the names of absent students.");
        doc(eric, "S1 History scheme of work", Enums.DocCategory.SCHEME_OF_WORK, Enums.DocStatus.SUBMITTED, Enums.Track.REB,
                "Term scheme: kingdoms, colonial period, independence.", null, null);
        doc(divine, "ICT monthly report", Enums.DocCategory.REPORT, Enums.DocStatus.DRAFT, Enums.Track.TVET,
                "Lab report: 18 computers working, 2 waiting for a mouse.", null, null);
        doc(grace, "S2 Physics lesson plan", Enums.DocCategory.LESSON_PLAN, Enums.DocStatus.REJECTED, Enums.Track.REB,
                "Forces lesson. Needs a practical activity.", hos, "Add one experiment the class can do.");

        LocalDate monday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        String[] classes = {"S2 A", "S2 B", "S3 A", "S1 A", "S3 B", "S2 C"};
        String[] rooms = {"B12", "B04", "Lab 1", "B09", "ICT Lab", "Lab 2"};
        LocalTime[] starts = {LocalTime.of(8, 0), LocalTime.of(8, 40), LocalTime.of(9, 20), LocalTime.of(10, 20), LocalTime.of(11, 0), LocalTime.of(11, 40)};
        for (int i = 0; i < teachers.size(); i++) {
            for (int d = 0; d < 5; d++) {
                LocalDate date = monday.plusDays(d);
                if (date.isAfter(today.plusDays(5))) continue;
                TeachingSession session = new TeachingSession();
                session.setTeacher(teachers.get(i));
                session.setClassName(classes[i]);
                session.setSubject(teachers.get(i).getSubject());
                session.setRoom(rooms[i]);
                session.setSessionDate(date);
                session.setStartTime(starts[i]);
                session.setEndTime(starts[i].plusMinutes(40));
                if (date.isBefore(today)) {
                    session.setStatus(Enums.ActivityStatus.COMPLETED);
                    session.setActualStart(LocalDateTime.of(date, starts[i]));
                    session.setActualEnd(LocalDateTime.of(date, starts[i].plusMinutes(40)));
                } else if (date.equals(today)) {
                    fillToday(session, teachers.get(i), date);
                } else {
                    session.setStatus(Enums.ActivityStatus.SCHEDULED);
                }
                sessions.save(session);
            }
        }
        TeachingSession remedial = new TeachingSession();
        remedial.setTeacher(alice);
        remedial.setClassName("S2 A");
        remedial.setSubject("Mathematics");
        remedial.setRoom("B12");
        remedial.setSessionDate(today);
        remedial.setStartTime(LocalTime.of(16, 0));
        remedial.setEndTime(LocalTime.of(16, 40));
        remedial.setStatus(Enums.ActivityStatus.SCHEDULED);
        remedial.setNote("Remedial for linear equations");
        sessions.save(remedial);

        note(marie, "Marked absent", "You are marked absent today. Contact the Head of Studies if this is a mistake.", "ATTENDANCE", Enums.Priority.HIGH);
        note(jean, "Late arrival", "You were marked late at 08:27.", "ATTENDANCE", Enums.Priority.NORMAL);
        note(alice, "Lesson plan approved", "S2 Mathematics lesson plan was approved.", "APPROVAL", Enums.Priority.NORMAL);
        note(hos, "Documents waiting", "English lesson plan and History scheme of work are in the approval queue.", "APPROVAL", Enums.Priority.HIGH);
        note(admin, "Welcome", "Sample school data is ready. Password for every demo account is password123.", "SYSTEM", Enums.Priority.LOW);
        note(grace, "Lesson plan returned", "S2 Physics lesson plan was rejected. Add a practical activity.", "APPROVAL", Enums.Priority.HIGH);

        String[][] marks = {
                {"Aline Keza", "S2 A", "Mathematics", "Term 1", "78", "Alice Uwimana"},
                {"Brian Mugisha", "S2 A", "Mathematics", "Term 1", "64", "Alice Uwimana"},
                {"Chloe Uwase", "S2 B", "English", "Term 1", "81", "Jean Bosco Hakizimana"},
                {"David Niyonsenga", "S3 A", "Biology", "Term 1", "57", "Marie Claire Ingabire"},
                {"Elise Iradukunda", "S1 A", "History", "Term 1", "73", "Eric Habimana"},
                {"Felix Habineza", "S3 B", "Computer Science", "Term 1", "88", "Divine Mukamana"},
                {"Gina Mutesi", "S2 C", "Physics", "Term 1", "69", "Grace Uwase"}
        };
        for (String[] mark : marks) {
            AcademicRecord row = new AcademicRecord();
            row.setStudentName(mark[0]);
            row.setClassName(mark[1]);
            row.setSubject(mark[2]);
            row.setTermName(mark[3]);
            row.setScore(Double.valueOf(mark[4]));
            row.setTeacherName(mark[5]);
            academic.save(row);
        }

        settings.put("teacher.upload", "true");
        settings.put("teacher.biometric", "true");
        settings.put("hos.approve", "true");
        settings.put("report.schedule", "18:00");
        settings.put("academic.syncEnabled", "true");
        settings.put("academic.lastImport", "Sample data");
        ready();
    }

    private void fillToday(TeachingSession session, UserAccount teacher, LocalDate date) {
        LocalTime start = session.getStartTime();
        if ("alice".equals(teacher.getUsername())) {
            session.setStatus(Enums.ActivityStatus.COMPLETED);
            session.setActualStart(LocalDateTime.of(date, start));
            session.setActualEnd(LocalDateTime.of(date, start.plusMinutes(40)));
        } else if ("jean".equals(teacher.getUsername())) {
            session.setStatus(Enums.ActivityStatus.COMPLETED);
            session.setActualStart(LocalDateTime.of(date, start.plusMinutes(14)));
            session.setActualEnd(LocalDateTime.of(date, start.plusMinutes(50)));
            session.setNote("Started late");
        } else if ("marie".equals(teacher.getUsername()) || "eric".equals(teacher.getUsername())) {
            session.setStatus(Enums.ActivityStatus.MISSED);
            session.setNote("marie".equals(teacher.getUsername()) ? "Teacher absent" : "Teacher on leave");
        } else {
            session.setStatus(Enums.ActivityStatus.COMPLETED);
            session.setActualStart(LocalDateTime.of(date, start));
            session.setActualEnd(LocalDateTime.of(date, start.plusMinutes(40)));
        }
    }

    private UserAccount account(String username, String name, String email, String hash, Enums.Role role,
                                String department, String subject, String code, String phone) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setFullName(name);
        user.setEmail(email);
        user.setPasswordHash(hash);
        user.setRole(role);
        user.setDepartment(department);
        user.setSubject(subject);
        user.setStaffCode(code);
        user.setPhone(phone);
        user.setActive(true);
        users.save(user);
        NotificationPreference pref = new NotificationPreference();
        pref.setUser(user);
        pref.setInAppOn(true);
        preferences.save(pref);
        return user;
    }

    private void mark(UserAccount teacher, LocalDate date, Enums.AttendanceStatus status, LocalTime in,
                      Enums.MarkMethod method, String note, UserAccount actor) {
        AttendanceRecord record = new AttendanceRecord();
        record.setTeacher(teacher);
        record.setAttendDate(date);
        record.setStatus(status);
        record.setMethod(method);
        record.setNote(note);
        record.setMarkedBy(actor);
        if (in != null) {
            record.setCheckIn(LocalDateTime.of(date, in));
        }
        attendance.save(record);
    }

    private void doc(UserAccount owner, String title, Enums.DocCategory category, Enums.DocStatus status,
                     Enums.Track track, String body, UserAccount reviewer, String comment) {
        String stored = files.saveText(title.replace(' ', '-') + ".txt", body);
        SchoolDocument doc = new SchoolDocument();
        doc.setTitle(title);
        doc.setCategory(category);
        doc.setTrack(track);
        doc.setStatus(status);
        doc.setOwner(owner);
        doc.setFilePath(stored);
        doc.setOriginalName(title + ".txt");
        doc.setContentType("text/plain");
        doc.setVersion(1);
        doc.setRoutedTo("Head of Studies");
        doc.setReviewerComment(comment);
        doc.setUpdatedAt(Instant.now());
        documents.save(doc);
        DocumentVersion version = new DocumentVersion();
        version.setDocument(doc);
        version.setVersionNumber(1);
        version.setFilePath(stored);
        version.setOriginalName(doc.getOriginalName());
        version.setNote("Seeded sample");
        versions.save(version);
        if (reviewer != null && comment != null) {
            ApprovalLog log = new ApprovalLog();
            log.setDocument(doc);
            log.setReviewer(reviewer);
            log.setAction(status.name());
            log.setComment(comment);
            approvals.save(log);
        }
    }

    private void note(UserAccount user, String title, String message, String type, Enums.Priority priority) {
        Notification row = new Notification();
        row.setUser(user);
        row.setTitle(title);
        row.setMessage(message);
        row.setType(type);
        row.setPriority(priority);
        row.setChannel("IN_APP");
        notifications.save(row);
    }

    private void ready() {
        System.out.println();
        System.out.println("Smart Teacher API is ready.");
        System.out.println("Website:  http://localhost:5173");
        System.out.println("Login:    admin / password123");
        System.out.println();
    }
}
