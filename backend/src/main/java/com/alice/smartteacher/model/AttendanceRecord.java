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
import javax.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "attendance_records", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"teacher_id", "attend_date"})
})
public class AttendanceRecord extends BaseEntity {

    @ManyToOne(optional = false)
    private UserAccount teacher;

    @Column(name = "attend_date", nullable = false)
    private LocalDate attendDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.AttendanceStatus status;

    private LocalDateTime checkIn;
    private LocalDateTime checkOut;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Enums.MarkMethod method;

    @Column(length = 1000)
    private String note;

    @ManyToOne
    private UserAccount markedBy;

    private Double faceDistance;
}
