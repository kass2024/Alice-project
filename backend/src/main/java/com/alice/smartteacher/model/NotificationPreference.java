package com.alice.smartteacher.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification_preferences")
public class NotificationPreference extends BaseEntity {

    @OneToOne(optional = false)
    private UserAccount user;

    private boolean emailOn = false;
    private boolean smsOn = false;
    private boolean inAppOn = true;
}
