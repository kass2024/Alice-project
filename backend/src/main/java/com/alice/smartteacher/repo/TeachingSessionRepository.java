package com.alice.smartteacher.repo;

import com.alice.smartteacher.model.TeachingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TeachingSessionRepository extends JpaRepository<TeachingSession, Long> {
    List<TeachingSession> findBySessionDateBetweenOrderBySessionDateAscStartTimeAsc(LocalDate from, LocalDate to);
    List<TeachingSession> findByTeacherIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(Long teacherId, LocalDate from, LocalDate to);
    List<TeachingSession> findBySessionDateOrderByStartTimeAsc(LocalDate date);
}
