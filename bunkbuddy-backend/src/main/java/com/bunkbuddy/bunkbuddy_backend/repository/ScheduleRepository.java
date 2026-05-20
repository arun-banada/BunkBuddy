package com.bunkbuddy.bunkbuddy_backend.repository;

import com.bunkbuddy.bunkbuddy_backend.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByUserId(Long userId);
    List<Schedule> findByUserIdAndDayOfWeek(Long userId, String dayOfWeek);
    void deleteByUserId(Long userId);
}
