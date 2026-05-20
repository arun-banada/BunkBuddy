package com.bunkbuddy.bunkbuddy_backend.controller;

import com.bunkbuddy.bunkbuddy_backend.entity.AttendanceRecord;
import com.bunkbuddy.bunkbuddy_backend.entity.Schedule;
import com.bunkbuddy.bunkbuddy_backend.entity.Subject;
import com.bunkbuddy.bunkbuddy_backend.entity.User;
import com.bunkbuddy.bunkbuddy_backend.repository.ScheduleRepository;
import com.bunkbuddy.bunkbuddy_backend.repository.SubjectRepository;
import com.bunkbuddy.bunkbuddy_backend.repository.UserRepository;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.bytedeco.opencv.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/timetable")
public class TimetableController {

    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final com.bunkbuddy.bunkbuddy_backend.repository.AttendanceRecordRepository attendanceRecordRepository;

    public TimetableController(SubjectRepository subjectRepository, UserRepository userRepository, ScheduleRepository scheduleRepository, com.bunkbuddy.bunkbuddy_backend.repository.AttendanceRecordRepository attendanceRecordRepository) {
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
        this.scheduleRepository = scheduleRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow();
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadTimetable(@RequestParam("file") MultipartFile file) {
        try {
            File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename());
            try (FileOutputStream fos = new FileOutputStream(convFile)) {
                fos.write(file.getBytes());
            }

            String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
            Map<String, List<Map<String, String>>> parsedSchedule = new LinkedHashMap<>();

            if (fileName.endsWith(".pdf")) {
                try (PDDocument document = PDDocument.load(convFile)) {
                    PDFTextStripper pdfStripper = new PDFTextStripper();
                    String result = pdfStripper.getText(document);
                    parsedSchedule = parseTextToMatrixFallback(result);
                }
            } else {
                try {
                    parsedSchedule = processImageWithOpenCV(convFile);
                } catch (Throwable e) {
                    // Fallback if OpenCV fails
                    System.err.println("OpenCV failed, using fallback OCR: " + e.getMessage());
                    ITesseract tesseract = new Tesseract();
                    tesseract.setDatapath("tessdata");
                    String fallbackText = tesseract.doOCR(convFile);
                    parsedSchedule = parseTextToMatrixFallback(fallbackText);
                }
            }

            // Extract unique subjects for UI reference
            Set<String> uniqueSubjects = new HashSet<>();
            for (List<Map<String, String>> dayEntries : parsedSchedule.values()) {
                for (Map<String, String> entry : dayEntries) {
                    uniqueSubjects.add(entry.get("subject"));
                }
            }

            return ResponseEntity.ok(Map.of(
                "message", "Timetable parsed successfully", 
                "timetable", parsedSchedule,
                "uniqueSubjects", uniqueSubjects
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing timetable: " + e.getMessage());
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<?> confirmSubjects(@RequestBody Map<String, List<Map<String, String>>> timetableMatrix) {
        try {
            User user = getCurrentUser();
            
            // Delete old schedule for this user to allow fresh upload
            scheduleRepository.deleteByUserId(user.getId());

            // Get existing subjects to avoid creating duplicates
            List<Subject> existingSubjects = subjectRepository.findByUserId(user.getId());
            Map<String, Subject> subjectMap = existingSubjects.stream()
                .collect(Collectors.toMap(s -> s.getName().toUpperCase(), s -> s));

            for (Map.Entry<String, List<Map<String, String>>> entry : timetableMatrix.entrySet()) {
                String day = entry.getKey().toUpperCase();
                for (Map<String, String> cell : entry.getValue()) {
                    String subjectName = cell.get("subject");
                    if (subjectName == null || subjectName.trim().isEmpty()) continue;
                    
                    String time = cell.get("time");
                    if (time == null) time = "00:00-00:00";
                    
                    String[] timeParts = time.split("-");
                    String startTime = timeParts.length > 0 ? timeParts[0] : "";
                    String endTime = timeParts.length > 1 ? timeParts[1] : "";

                    // Find or create subject
                    Subject subject = subjectMap.get(subjectName.toUpperCase());
                    if (subject == null) {
                        subject = new Subject();
                        subject.setName(subjectName);
                        subject.setUser(user);
                        subjectRepository.save(subject);
                        subjectMap.put(subjectName.toUpperCase(), subject);
                    }

                    // Create schedule entry
                    Schedule schedule = new Schedule();
                    schedule.setDayOfWeek(day);
                    schedule.setStartTime(startTime);
                    schedule.setEndTime(endTime);
                    schedule.setSubject(subject);
                    schedule.setUser(user);
                    scheduleRepository.save(schedule);
                }
            }

            return ResponseEntity.ok(Map.of("message", "Timetable saved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error saving timetable: " + e.getMessage());
        }
    }

    @GetMapping("/today")
    public ResponseEntity<?> getTodaysSchedule() {
        try {
            User user = getCurrentUser();
            String today = LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase();
            List<Schedule> todaySchedules = scheduleRepository.findByUserIdAndDayOfWeek(user.getId(), today);
            
            List<Map<String, Object>> result = new ArrayList<>();
            for (Schedule schedule : todaySchedules) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", schedule.getId());
                map.put("startTime", schedule.getStartTime());
                map.put("endTime", schedule.getEndTime());
                map.put("subject", schedule.getSubject());
                
                AttendanceRecord record = attendanceRecordRepository.findByScheduleIdAndDate(schedule.getId(), LocalDate.now());
                if (record != null) {
                    map.put("markedStatus", record.getStatus());
                } else {
                    map.put("markedStatus", null);
                }
                result.add(map);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching today's schedule: " + e.getMessage());
        }
    }

    private Map<String, List<Map<String, String>>> processImageWithOpenCV(File file) throws Exception {
        Mat src = imread(file.getAbsolutePath());
        if (src.empty()) {
            throw new Exception("Cannot read image using OpenCV");
        }

        // 1. Convert to Grayscale
        Mat gray = new Mat();
        cvtColor(src, gray, COLOR_BGR2GRAY);

        // 2. Adaptive Thresholding to get binary image
        Mat binary = new Mat();
        bitwise_not(gray, gray); // Invert image so lines are white
        adaptiveThreshold(gray, binary, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 15, -2);

        // 3. Detect horizontal lines
        Mat horizontal = binary.clone();
        int horizontalSize = Math.max(1, horizontal.cols() / 40); // More permissive line detection
        Mat horizontalStructure = getStructuringElement(MORPH_RECT, new Size(horizontalSize, 1));
        erode(horizontal, horizontal, horizontalStructure);
        dilate(horizontal, horizontal, horizontalStructure);

        // 4. Detect vertical lines
        Mat vertical = binary.clone();
        int verticalSize = Math.max(1, vertical.rows() / 40); // More permissive line detection
        Mat verticalStructure = getStructuringElement(MORPH_RECT, new Size(1, verticalSize));
        erode(vertical, vertical, verticalStructure);
        dilate(vertical, vertical, verticalStructure);

        // 5. Create mask of table by combining lines
        Mat mask = new Mat();
        add(horizontal, vertical, mask);

        // Find contours (cells)
        MatVector contours = new MatVector();
        Mat hierarchy = new Mat();
        findContours(mask, contours, hierarchy, RETR_TREE, CHAIN_APPROX_SIMPLE);

        List<Rect> boundingBoxes = new ArrayList<>();
        for (int i = 0; i < contours.size(); i++) {
            Rect rect = boundingRect(contours.get(i));
            // Filter noise by area, but be more permissive for small timetables
            if (rect.area() > 500 && rect.width() > 15 && rect.height() > 10) {
                // Ensure we don't just capture the entire image border
                if (rect.width() < src.cols() * 0.95 && rect.height() < src.rows() * 0.95) {
                    boundingBoxes.add(rect);
                }
            }
        }

        // Fallback to NLP if no grid found
        if (boundingBoxes.size() < 5) {
            System.out.println("No clear table found, falling back to full image OCR");
            File tempProcessed = new File(System.getProperty("java.io.tmpdir") + "/processed.png");
            bitwise_not(gray, gray);
            imwrite(tempProcessed.getAbsolutePath(), gray);
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath("tessdata");
            String text = tesseract.doOCR(tempProcessed);
            return parseTextToMatrixFallback(text);
        }

        // --- PERFECT EXTRACTION ALGORITHM ---
        
        // Step A: Sort bounding boxes by Y coordinate to group into rows
        boundingBoxes.sort(Comparator.comparingInt(Rect::y));
        
        List<List<Rect>> rows = new ArrayList<>();
        List<Rect> currentRow = new ArrayList<>();
        
        int currentY = boundingBoxes.get(0).y();
        int rowTolerance = 15; // Pixels tolerance for same row
        
        for (Rect rect : boundingBoxes) {
            if (Math.abs(rect.y() - currentY) > rowTolerance) {
                rows.add(new ArrayList<>(currentRow));
                currentRow.clear();
                currentY = rect.y();
            }
            currentRow.add(rect);
        }
        if (!currentRow.isEmpty()) {
            rows.add(currentRow);
        }

        // Step B: Sort cells in each row by X coordinate
        for (List<Rect> row : rows) {
            row.sort(Comparator.comparingInt(Rect::x));
        }

        // We revert bitwise_not on gray for OCR, so text is black on white
        bitwise_not(gray, gray);

        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("tessdata");
        tesseract.setPageSegMode(6); // Assume a single uniform block of text
        
        int numRows = rows.size();
        int maxCols = rows.stream().mapToInt(List::size).max().orElse(0);
        String[][] textMatrix = new String[numRows][maxCols];

        for (int i = 0; i < numRows; i++) {
            List<Rect> row = rows.get(i);
            for (int j = 0; j < row.size(); j++) {
                Rect rect = row.get(j);
                
                // Crop the cell from the grayscale image, slightly shrunk to avoid borders
                int padding = 4;
                int x = Math.max(0, rect.x() + padding);
                int y = Math.max(0, rect.y() + padding);
                int w = Math.max(1, rect.width() - padding * 2);
                int h = Math.max(1, rect.height() - padding * 2);
                
                // Ensure bounds
                x = Math.min(x, src.cols() - 1);
                y = Math.min(y, src.rows() - 1);
                w = Math.min(w, src.cols() - x);
                h = Math.min(h, src.rows() - y);

                Rect safeRect = new Rect(x, y, w, h);
                if (safeRect.width() <= 0 || safeRect.height() <= 0) {
                     textMatrix[i][j] = "";
                     continue;
                }

                Mat cellMat = new Mat(gray, safeRect);
                
                File tempCell = new File(System.getProperty("java.io.tmpdir") + "/cell_" + i + "_" + j + ".png");
                imwrite(tempCell.getAbsolutePath(), cellMat);
                
                String cellText = "";
                try {
                    cellText = tesseract.doOCR(tempCell).trim().replaceAll("\n", " ");
                } catch (Exception e) {
                    // Ignore OCR errors on specific cells
                }
                textMatrix[i][j] = cleanCellText(cellText);
            }
        }

        // Build Structured Data
        Map<String, List<Map<String, String>>> parsedSchedule = new LinkedHashMap<>();
        
        // Heuristic: Find which dimension is Days and which is Times
        boolean daysInCol0 = false;
        for (int i = 0; i < numRows; i++) {
            if (textMatrix[i][0] != null && textMatrix[i][0].toUpperCase().matches(".*(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY).*")) {
                daysInCol0 = true;
                break;
            }
        }

        if (daysInCol0) {
            // Days in Col 0, Times in Row 0
            for (int i = 1; i < numRows; i++) {
                String day = textMatrix[i][0];
                if (day == null || day.isEmpty()) continue;
                day = extractDay(day);
                if (day == null) continue;
                
                parsedSchedule.putIfAbsent(day, new ArrayList<>());
                
                for (int j = 1; j < maxCols; j++) {
                    if (j >= rows.get(i).size()) break;
                    String time = textMatrix[0][j] != null ? extractTime(textMatrix[0][j]) : "00:00-00:00";
                    String subject = textMatrix[i][j];
                    if (subject != null && isValidSubject(subject)) {
                        Map<String, String> entry = new HashMap<>();
                        entry.put("time", time);
                        entry.put("subject", subject);
                        parsedSchedule.get(day).add(entry);
                    }
                }
            }
        } else {
            // Days in Row 0, Times in Col 0 (or fallback logic)
            for (int j = 1; j < maxCols; j++) {
                String day = textMatrix[0][j];
                if (day == null || day.isEmpty()) continue;
                day = extractDay(day);
                if (day == null) continue;

                parsedSchedule.putIfAbsent(day, new ArrayList<>());
                
                for (int i = 1; i < numRows; i++) {
                    if (j >= rows.get(i).size()) break;
                    String time = textMatrix[i][0] != null ? extractTime(textMatrix[i][0]) : "00:00-00:00";
                    String subject = textMatrix[i][j];
                    if (subject != null && isValidSubject(subject)) {
                        Map<String, String> entry = new HashMap<>();
                        entry.put("time", time);
                        entry.put("subject", subject);
                        parsedSchedule.get(day).add(entry);
                    }
                }
            }
        }

        // If the parsing was completely empty, fallback
        if (parsedSchedule.isEmpty()) {
             System.out.println("Matrix extraction yielded empty results. Falling back to NLP.");
             File tempProcessed = new File(System.getProperty("java.io.tmpdir") + "/processed.png");
             imwrite(tempProcessed.getAbsolutePath(), gray);
             String text = tesseract.doOCR(tempProcessed);
             return parseTextToMatrixFallback(text);
        }

        return parsedSchedule;
    }
    
    private String cleanCellText(String text) {
        if (text == null) return "";
        // Extract the first meaningful part before slashes or brackets
        String cleaned = text.trim().toUpperCase();
        
        // e.g. ML LAB(A1)/PROJECT PHASE I(A2) -> Extract "ML LAB"
        // First split by '/' and take the first part
        if (cleaned.contains("/")) {
            cleaned = cleaned.split("/")[0];
        }
        
        // Then remove anything inside brackets
        cleaned = cleaned.replaceAll("\\(.*?\\)", "");
        
        // Remove standard non-alphanumeric (keep spaces)
        cleaned = cleaned.replaceAll("[^A-Z0-9\\s]", "").trim();
        
        return cleaned;
    }
    
    private String extractDay(String text) {
        String[] days = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
        for (String d : days) {
            if (text.contains(d)) return d;
        }
        return null;
    }
    
    private String extractTime(String text) {
        java.util.regex.Matcher timeMatcher = java.util.regex.Pattern.compile("\\d{1,2}:\\d{2}\\s*(AM|PM)?\\s*-\\s*\\d{1,2}:\\d{2}\\s*(AM|PM)?").matcher(text);
        if (timeMatcher.find()) {
            return timeMatcher.group();
        }
        return "00:00-00:00";
    }
    
    private boolean isValidSubject(String word) {
        if (word == null || word.trim().isEmpty()) return false;
        
        List<String> stopWords = java.util.Arrays.asList(
            "ROOM", "FACULTY", "BREAK", "LUNCH", "SECTION", "SEMESTER", "CLASS", 
            "PROF", "DR", "MR", "MRS", "TUTORIAL", "SELF LEARNING", "REMEDIAL", "MENTORING", 
            "CODE COURSE", "LTPS", "ACADEMIC HEAD", "PRINCIPAL", "DEPARTMENT", "HOD", "TIME TABLE",
            "A1", "A2", "TG", "SET", "PROJECT PHASE"
        );
        
        if (word.length() < 2 || word.length() > 25) return false;
        if (word.matches("^[0-9]+$")) return false; // Only numbers
        if (word.matches("^[A-Z]{3}\\d{3}$")) return false; // BCS601
        
        for (String stop : stopWords) {
            if (word.equals(stop) || word.contains(stop + " ") || word.contains(" " + stop)) {
                // If the entire word is a stop word or contains it as a distinct token, it might be invalid.
                // But we must be careful not to discard "ML LAB" if stopword is "LAB"
                // The prompt says ignore invalid cells completely if they match those things.
                return false;
            }
        }
        return true;
    }

    // Helper to filter words and map to structure
    private Map<String, List<Map<String, String>>> parseTextToMatrixFallback(String text) {
        Map<String, List<Map<String, String>>> timetable = new LinkedHashMap<>();
        String currentDay = "Unassigned";
        
        List<String> stopWords = java.util.Arrays.asList(
            "ROOM", "FACULTY", "LAB", "BREAK", "LUNCH", "SECTION", "SEMESTER", "CLASS", 
            "PROF", "DR", "MR", "MRS", "TUTORIAL", "SELF LEARNING", "REMEDIAL", "MENTORING", 
            "CODE COURSE", "LTPS", "ACADEMIC HEAD", "PRINCIPAL", "DEPARTMENT", "HOD"
        );

        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim().toUpperCase();
            if (line.isEmpty()) continue;

            if (line.matches("^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY).*")) {
                currentDay = line.split("\\s+")[0];
                timetable.putIfAbsent(currentDay, new ArrayList<>());
                continue;
            }

            // Extract time if exists
            String time = "00:00-00:00";
            java.util.regex.Matcher timeMatcher = java.util.regex.Pattern.compile("\\d{1,2}:\\d{2}\\s*(AM|PM)?\\s*-\\s*\\d{1,2}:\\d{2}\\s*(AM|PM)?").matcher(line);
            if (timeMatcher.find()) {
                time = timeMatcher.group();
                line = line.replace(time, "");
            }

            String cleanedLine = cleanCellText(line);
            String[] words = cleanedLine.split("\\s+");
            
            // We need to rebuild subject names carefully since spaces were kept in cleanCellText
            // But actually the fallback parser reads word by word which is bad.
            // Let's just treat the rest of the line as the subject name if it's valid.
            String subjectName = line;
            if (subjectName.contains("/")) {
                subjectName = subjectName.split("/")[0];
            }
            subjectName = subjectName.replaceAll("\\(.*?\\)", "").trim();
            subjectName = subjectName.replaceAll("[^A-Z0-9\\s]", "").trim();
            
            if (isValidSubject(subjectName)) {
                if (currentDay.equals("Unassigned")) {
                    currentDay = "MONDAY"; // Default
                }
                
                timetable.putIfAbsent(currentDay, new ArrayList<>());
                Map<String, String> cell = new HashMap<>();
                cell.put("time", time);
                cell.put("subject", subjectName);
                timetable.get(currentDay).add(cell);
            } else {
                // If the entire line is not a valid subject, try word by word
                for (String word : words) {
                    if (isValidSubject(word)) {
                        if (currentDay.equals("Unassigned")) {
                            currentDay = "MONDAY"; // Default
                        }
                        timetable.putIfAbsent(currentDay, new ArrayList<>());
                        Map<String, String> cell = new HashMap<>();
                        cell.put("time", time);
                        cell.put("subject", word);
                        timetable.get(currentDay).add(cell);
                    }
                }
            }
        }
        return timetable;
    }
}
