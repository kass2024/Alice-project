package com.alice.smartteacher.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "teaching_sessions")
public class TeachingSession extends BaseEntity {

    @ManyToOne(optional = false)
    private UserAccount teacher;

    @Column(nullable = false)
    private String className;

    @Column(nullable = false)
    private String subject;

    private String room;

    @Column(nullable = false)
    private LocalDate sessionDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.ActivityStatus status = Enums.ActivityStatus.SCHEDULED;

    @Column(length = 1000)
    private String note;
}
