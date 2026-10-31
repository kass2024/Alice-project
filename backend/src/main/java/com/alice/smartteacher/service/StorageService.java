package com.alice.smartteacher.service;

import com.alice.smartteacher.model.AcademicRecord;
import com.alice.smartteacher.model.UserAccount;
import com.alice.smartteacher.repo.AcademicRecordRepository;
import com.alice.smartteacher.repo.AttendanceRepository;
import com.alice.smartteacher.repo.AuditLogRepository;
import com.alice.smartteacher.repo.DocumentRepository;
import com.alice.smartteacher.repo.LoginSessionRepository;
import com.alice.smartteacher.repo.NotificationRepository;
import com.alice.smartteacher.repo.TeachingSessionRepository;
import com.alice.smartteacher.repo.UserRepository;
import com.alice.smartteacher.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class StorageService {

    private final CurrentUserService current;
    private final UserRepository users;
    private final AttendanceRepository attendance;
    private final DocumentRepository documents;
    private final TeachingSessionRepository sessions;
    private final NotificationRepository notifications;
    private final AuditLogRepository audits;
    private final AcademicRecordRepository academic;
    private final LoginSessionRepository loginSessions;
    private final FileStore files;
    private final SettingService settings;
    private final AuditService audit;

    @Value("${app.mysql-bin:C:/xampp7/mysql/bin}")
    private String mysqlBin;
    @Value("${app.db-name:smart_teacher}")
    private String dbName;
    @Value("${app.db-port:3307}")
    private String dbPort;
    @Value("${app.db-user:root}")
    private String dbUser;
    @Value("${app.db-password:}")
    private String dbPassword;

    public StorageService(CurrentUserService current, UserRepository users, AttendanceRepository attendance,
                          DocumentRepository documents, TeachingSessionRepository sessions,
                          NotificationRepository notifications, AuditLogRepository audits,
                          AcademicRecordRepository academic, LoginSessionRepository loginSessions,
                          FileStore files, SettingService settings, AuditService audit) {
        this.current = current;
        this.users = users;
        this.attendance = attendance;
        this.documents = documents;
        this.sessions = sessions;
        this.notifications = notifications;
        this.audits = audits;
        this.academic = academic;
        this.loginSessions = loginSessions;
        this.files = files;
        this.settings = settings;
        this.audit = audit;
    }

    public Map<String, Object> overview() {
        Access.requireAdmin(current.get());
        List<Map<String, Object>> tables = List.of(
                row("Users", users.count()),
                row("Attendance", attendance.count()),
                row("Documents", documents.count()),
                row("Teaching sessions", sessions.count()),
                row("Notifications", notifications.count()),
                row("Audit log", audits.count()),
                row("Academic records", academic.count()),
                row("Login sessions", loginSessions.count())
        );
        return Maps.of(
                "tables", tables,
                "uploadBytes", files.folderBytes(),
                "database", dbName,
                "engine", "MariaDB (XAMPP)",
                "syncEnabled", settings.enabled("academic.syncEnabled"),
                "lastImport", settings.get("academic.lastImport", "Never")
        );
    }

    public List<AcademicRecord> academicRows() {
        Access.requireAdmin(current.get());
        return academic.findAllByOrderByClassNameAscStudentNameAsc();
    }

    public byte[] exportAcademic() {
        Access.requireAdmin(current.get());
        StringBuilder csv = new StringBuilder("\uFEFFstudent,class,subject,term,score,teacher\n");
        for (AcademicRecord row : academic.findAllByOrderByClassNameAscStudentNameAsc()) {
            csv.append(esc(row.getStudentName())).append(',')
                    .append(esc(row.getClassName())).append(',')
                    .append(esc(row.getSubject())).append(',')
                    .append(esc(row.getTermName())).append(',')
                    .append(row.getScore() == null ? "" : row.getScore()).append(',')
                    .append(esc(row.getTeacherName())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public int importAcademic(MultipartFile file, HttpServletRequest request) {
        UserAccount me = current.get();
        Access.requireAdmin(me);
        if (!settings.enabled("academic.syncEnabled")) {
            throw new ApiException(400, "Academic sync is turned off. Turn it on first.");
        }
        String raw;
        try {
            raw = new String(file.getBytes(), StandardCharsets.UTF_8).replace("\uFEFF", "");
        } catch (Exception ex) {
            throw new ApiException(400, "Could not read the CSV file.");
        }
        String[] lines = raw.split("\\r?\\n");
        int count = 0;
        for (String line : lines) {
            if (line.isBlank() || line.toLowerCase().startsWith("student")) continue;
            String[] cells = line.split(",", -1);
            if (cells.length < 4) continue;
            AcademicRecord row = new AcademicRecord();
            row.setStudentName(cells[0].trim());
            row.setClassName(cells[1].trim());
            row.setSubject(cells[2].trim());
            row.setTermName(cells.length > 3 ? cells[3].trim() : "");
            if (cells.length > 4 && !cells[4].isBlank()) {
                try {
                    row.setScore(Double.valueOf(cells[4].trim()));
                } catch (NumberFormatException ignored) {
                    row.setScore(null);
                }
            }
            row.setTeacherName(cells.length > 5 ? cells[5].trim() : "");
            academic.save(row);
            count++;
        }
        settings.put("academic.lastImport", SchoolClock.now().toString());
        audit.record(me, "ACADEMIC_IMPORT", count + " rows", request);
        return count;
    }

    public void setSync(boolean enabled) {
        Access.requireAdmin(current.get());
        settings.put("academic.syncEnabled", enabled ? "true" : "false");
    }

    public byte[] backup(HttpServletRequest request) {
        UserAccount me = current.get();
        Access.requireAdmin(me);
        Path dump = Path.of(mysqlBin, "mysqldump.exe");
        if (!Files.exists(dump)) {
            throw new ApiException(500, "mysqldump was not found at " + dump + ". Check app.mysql-bin.");
        }
        List<String> cmd = new ArrayList<>();
        cmd.add(dump.toString());
        cmd.add("-u");
        cmd.add(dbUser);
        cmd.add("-P");
        cmd.add(dbPort);
        cmd.add("--single-transaction");
        if (dbPassword != null && !dbPassword.isBlank()) {
            cmd.add("--password=" + dbPassword);
        }
        cmd.add(dbName);
        try {
            Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            byte[] bytes = process.getInputStream().readAllBytes();
            int code = process.waitFor();
            if (code != 0) {
                String err = new String(bytes, StandardCharsets.UTF_8);
                throw new ApiException(500, "Backup failed. " + err.substring(0, Math.min(err.length(), 240)));
            }
            audit.record(me, "BACKUP", "Downloaded SQL backup", request);
            settings.put("storage.lastBackup", SchoolClock.now().toString());
            return bytes;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(500, "Backup failed. " + ex.getMessage());
        }
    }

    public String restore(MultipartFile file, HttpServletRequest request) {
        UserAccount me = current.get();
        Access.requireAdmin(me);
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".sql")) {
            throw new ApiException(400, "Choose a .sql backup file.");
        }
        Path mysql = Path.of(mysqlBin, "mysql.exe");
        if (!Files.exists(mysql)) {
            throw new ApiException(500, "mysql.exe was not found at " + mysql);
        }
        try {
            Path temp = Files.createTempFile("smart-teacher-", ".sql");
            file.transferTo(temp);
            List<String> cmd = new ArrayList<>();
            cmd.add(mysql.toString());
            cmd.add("-u");
            cmd.add(dbUser);
            cmd.add("-P");
            cmd.add(dbPort);
            if (dbPassword != null && !dbPassword.isBlank()) cmd.add("--password=" + dbPassword);
            cmd.add(dbName);
            Process process = new ProcessBuilder(cmd).redirectInput(temp.toFile()).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int code = process.waitFor();
            Files.deleteIfExists(temp);
            if (code != 0) {
                throw new ApiException(500, "Restore failed. " + output.substring(0, Math.min(output.length(), 240)));
            }
            audit.record(me, "RESTORE", file.getOriginalFilename(), request);
            return "Restore finished. Sign in again if your session was replaced.";
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(500, "Restore failed. " + ex.getMessage());
        }
    }

    private Map<String, Object> row(String name, long count) {
        return Maps.of("name", name, "rows", count);
    }

    private String esc(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
