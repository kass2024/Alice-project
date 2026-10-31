package com.alice.smartteacher.service;

import com.alice.smartteacher.model.AttendanceRecord;
import com.alice.smartteacher.model.Enums;
import com.alice.smartteacher.repo.AttendanceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReportScheduler {

    private final SettingService settings;
    private final NotificationService notifications;
    private final AttendanceRepository attendance;

    public ReportScheduler(SettingService settings, NotificationService notifications, AttendanceRepository attendance) {
        this.settings = settings;
        this.notifications = notifications;
        this.attendance = attendance;
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 15000)
    public void dailySummary() {
        String configured = settings.get("report.schedule", "18:00");
        java.time.LocalTime now = java.time.LocalTime.now(SchoolClock.ZONE).withSecond(0).withNano(0);
        if (!now.equals(java.time.LocalTime.parse(configured))) {
            return;
        }
        String today = SchoolClock.today().toString();
        if (today.equals(settings.get("report.lastRun", ""))) {
            return;
        }
        int present = 0;
        int absent = 0;
        for (AttendanceRecord record : attendance.findByAttendDate(SchoolClock.today())) {
            if (record.getStatus() == Enums.AttendanceStatus.PRESENT || record.getStatus() == Enums.AttendanceStatus.LATE) {
                present++;
            }
            if (record.getStatus() == Enums.AttendanceStatus.ABSENT) {
                absent++;
            }
        }
        String message = present + " teachers are present or late, and " + absent + " are absent.";
        notifications.notifyRole(Enums.Role.ADMIN, "Daily attendance summary", message, "SYSTEM", Enums.Priority.NORMAL);
        notifications.notifyRole(Enums.Role.HEAD_OF_STUDIES, "Daily attendance summary", message, "SYSTEM", Enums.Priority.NORMAL);
        settings.put("report.lastRun", today);
    }
}
