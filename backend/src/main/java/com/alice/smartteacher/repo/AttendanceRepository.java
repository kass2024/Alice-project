package com.alice.smartteacher.repo;

import com.alice.smartteacher.model.AttendanceRecord;
import com.alice.smartteacher.model.Enums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findByTeacherIdAndAttendDate(Long teacherId, LocalDate date);
    List<AttendanceRecord> findByAttendDate(LocalDate date);
    List<AttendanceRecord> findByAttendDateBetweenOrderByAttendDateDesc(LocalDate from, LocalDate to);
    List<AttendanceRecord> findByTeacherIdOrderByAttendDateDesc(Long teacherId);
    List<AttendanceRecord> findTop40ByMethodInOrderByAttendDateDesc(Collection<Enums.MarkMethod> methods);
}
