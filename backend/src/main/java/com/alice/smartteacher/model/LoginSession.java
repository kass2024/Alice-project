package com.alice.smartteacher.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "login_sessions")
public class LoginSession extends BaseEntity {

    @ManyToOne(optional = false)
    private UserAccount user;

    private boolean active = true;
    private String ipAddress;

    @Column(length = 300)
    private String userAgent;

    private Instant endedAt;
}
