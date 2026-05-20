package com.bunkbuddy.bunkbuddy_backend.service;

import com.bunkbuddy.bunkbuddy_backend.entity.Subject;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PredictionService {

    private static final double REQUIRED_ATTENDANCE_PERCENTAGE = 75.0;

    public Map<String, Object> calculateSubjectStats(Subject subject) {
        Map<String, Object> stats = new HashMap<>();
        int total = subject.getTotalClasses();
        int attended = subject.getAttendedClasses();

        double currentPercentage = total == 0 ? 100.0 : ((double) attended / total) * 100;

        int safeBunks = 0;
        int classesNeeded = 0;
        String recommendation = "";

        if (currentPercentage >= REQUIRED_ATTENDANCE_PERCENTAGE) {
            safeBunks = (int) Math.floor((attended - 0.75 * total) / 0.75);
            if (safeBunks > 0) {
                recommendation = "You can safely skip " + safeBunks + " more classes.";
            } else {
                recommendation = "You are on the edge. Attend the next class.";
            }
        } else {
            classesNeeded = (int) Math.ceil((0.75 * total - attended) / 0.25);
            recommendation = "Attend next " + classesNeeded + " classes to stay above 75%.";
        }

        stats.put("subjectId", subject.getId());
        stats.put("subjectName", subject.getName());
        stats.put("totalClasses", total);
        stats.put("attendedClasses", attended);
        stats.put("currentPercentage", currentPercentage);
        stats.put("safeBunks", Math.max(0, safeBunks));
        stats.put("classesNeeded", Math.max(0, classesNeeded));
        stats.put("recommendation", recommendation);

        return stats;
    }
}
