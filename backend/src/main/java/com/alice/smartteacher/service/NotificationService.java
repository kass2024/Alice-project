package com.alice.smartteacher.service;

import com.alice.smartteacher.model.Enums;
import com.alice.smartteacher.model.Notification;
import com.alice.smartteacher.model.NotificationPreference;
import com.alice.smartteacher.model.UserAccount;
import com.alice.smartteacher.repo.NotificationPreferenceRepository;
import com.alice.smartteacher.repo.NotificationRepository;
import com.alice.smartteacher.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final NotificationPreferenceRepository preferences;
    private final UserRepository users;

    public NotificationService(NotificationRepository notifications,
                               NotificationPreferenceRepository preferences,
                               UserRepository users) {
        this.notifications = notifications;
        this.preferences = preferences;
        this.users = users;
    }

    @Transactional
    public void notify(UserAccount user, String title, String message, String type, Enums.Priority priority) {
        if (user == null) {
            return;
        }
        NotificationPreference pref = preferences.findByUserId(user.getId()).orElse(null);
        if (pref != null && !pref.isInAppOn()) {
            return;
        }
        Notification row = new Notification();
        row.setUser(user);
        row.setTitle(title);
        row.setMessage(message);
        row.setType(type);
        row.setPriority(priority == null ? Enums.Priority.NORMAL : priority);
        row.setChannel("IN_APP");
        notifications.save(row);
    }

    @Transactional
    public void notifyRole(Enums.Role role, String title, String message, String type, Enums.Priority priority) {
        for (UserAccount user : users.findByRoleAndActiveTrueOrderByFullNameAsc(role)) {
            notify(user, title, message, type, priority);
        }
    }

    public List<Notification> mine(UserAccount user) {
        if (user.getRole() == Enums.Role.ADMIN) {
            return notifications.findTop100ByOrderByCreatedAtDesc();
        }
        return notifications.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public List<UserAccount> activeRole(Enums.Role role) {
        return new ArrayList<>(users.findByRoleAndActiveTrueOrderByFullNameAsc(role));
    }
}
