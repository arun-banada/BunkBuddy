package com.bunkbuddy.bunkbuddy_backend.repository;

import com.bunkbuddy.bunkbuddy_backend.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findBySubjectId(Long subjectId);
    boolean existsByScheduleIdAndDate(Long scheduleId, java.time.LocalDate date);
    AttendanceRecord findByScheduleIdAndDate(Long scheduleId, java.time.LocalDate date);
}
