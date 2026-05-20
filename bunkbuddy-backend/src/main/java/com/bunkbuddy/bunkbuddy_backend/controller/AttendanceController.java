package com.bunkbuddy.bunkbuddy_backend.controller;

import com.bunkbuddy.bunkbuddy_backend.entity.AttendanceRecord;
import com.bunkbuddy.bunkbuddy_backend.entity.Schedule;
import com.bunkbuddy.bunkbuddy_backend.entity.Subject;
import com.bunkbuddy.bunkbuddy_backend.repository.AttendanceRecordRepository;
import com.bunkbuddy.bunkbuddy_backend.repository.ScheduleRepository;
import com.bunkbuddy.bunkbuddy_backend.repository.SubjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final SubjectRepository subjectRepository;
    private final ScheduleRepository scheduleRepository;

    public AttendanceController(AttendanceRecordRepository attendanceRecordRepository, SubjectRepository subjectRepository, ScheduleRepository scheduleRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.subjectRepository = subjectRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @PostMapping("/mark")
    public ResponseEntity<?> markAttendance(@RequestBody Map<String, Object> request) {
        try {
            Long subjectId = Long.parseLong(request.get("subjectId").toString());
            String status = request.get("status").toString(); // "PRESENT" or "ABSENT"
            
            Long scheduleId = null;
            if (request.containsKey("scheduleId") && request.get("scheduleId") != null) {
                scheduleId = Long.parseLong(request.get("scheduleId").toString());
            }

            if (scheduleId != null) {
                boolean alreadyMarked = attendanceRecordRepository.existsByScheduleIdAndDate(scheduleId, LocalDate.now());
                if (alreadyMarked) {
                    return ResponseEntity.badRequest().body("Attendance already marked for this class today.");
                }
            }

            Subject subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new RuntimeException("Subject not found"));

            AttendanceRecord record = new AttendanceRecord();
            record.setSubject(subject);
            record.setDate(LocalDate.now());
            record.setStatus(status);
            
            if (scheduleId != null) {
                Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
                record.setSchedule(schedule);
            }

            subject.setTotalClasses(subject.getTotalClasses() + 1);
            if ("PRESENT".equalsIgnoreCase(status)) {
                subject.setAttendedClasses(subject.getAttendedClasses() + 1);
            }

            subjectRepository.save(subject);
            attendanceRecordRepository.save(record);

            return ResponseEntity.ok(Map.of("message", "Attendance marked successfully", "subjectId", subject.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error marking attendance: " + e.getMessage());
        }
    }
}
