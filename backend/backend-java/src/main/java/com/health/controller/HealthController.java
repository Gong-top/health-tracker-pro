package com.health.controller;

import com.health.repository.ReportRepository;
import com.health.service.AttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") 
public class HealthController {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private AttachmentService attachmentService;

    // --- 最终版验证接口 ---
    @GetMapping("/ping")
    public String ping() {
        return "v4-active-bulletproof";
    }

    private ResponseEntity<?> handleAction(Runnable action) {
        try {
            action.run();
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
        Map<String, Object> user = reportRepository.findByUsername(loginData.get("username"));
        if (user != null && loginData.get("password").equals(user.get("password"))) return ResponseEntity.ok(user);
        return ResponseEntity.status(401).body(Map.of("error", "用户名或密码错误"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> regData) {
        return handleAction(() -> reportRepository.saveUser(regData.get("username"), regData.get("password"), regData.get("nickname"), regData.get("email")));
    }

    @PutMapping("/user/profile/{userId}")
    public ResponseEntity<?> updateProfile(@PathVariable Long userId, @RequestBody Map<String, String> data) {
        return handleAction(() -> reportRepository.updateUserProfile(userId, data.get("nickname"), data.get("email")));
    }

    @GetMapping("/habits/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getUserHabits(@PathVariable Long userId) {
        return ResponseEntity.ok(reportRepository.findUserHabits(userId));
    }

    @PostMapping("/habits/{userId}")
    public ResponseEntity<?> addHabit(@PathVariable Long userId, @RequestBody Map<String, Object> data) {
        return handleAction(() -> {
            String name = (String) data.get("habitName");
            Object targetVal = data.get("targetValue");
            Double target = targetVal == null ? 1.0 : Double.valueOf(targetVal.toString());
            String unit = (String) data.get("targetUnit");
            String frequency = (String) data.get("frequency");
            reportRepository.saveNewHabit(userId, name, target, unit, frequency);
        });
    }

    @PutMapping("/habits/{id}/status")
    public ResponseEntity<Void> toggleStatus(@PathVariable Long id, @RequestParam boolean enabled) {
        reportRepository.toggleHabitStatus(id, enabled);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/habits/{id}/settings")
    public ResponseEntity<?> updateHabitSettings(@PathVariable Long id, @RequestBody Map<String, Object> settings) {
        return handleAction(() -> {
            String name = (String) settings.get("habitName");
            Object targetVal = settings.get("targetValue");
            Double target = targetVal == null ? 1.0 : Double.valueOf(targetVal.toString());
            String unit = (String) settings.get("targetUnit");
            String freq = (String) (settings.get("frequency") != null ? settings.get("frequency") : "day");
            String time = (String) (settings.get("reminderTime") != null ? settings.get("reminderTime") : "08:00:00");
            reportRepository.updateHabitSettings(id, name, target, unit, freq, time);
        });
    }

    @DeleteMapping("/habits/{userHabitId}")
    public ResponseEntity<?> deleteHabit(@PathVariable Long userHabitId) {
        return handleAction(() -> reportRepository.deleteUserHabit(userHabitId));
    }

    @PostMapping("/record/detail")
    public ResponseEntity<?> createDetailedRecord(@RequestBody Map<String, Object> data) {
        return handleAction(() -> {
            Long id = Long.valueOf(data.get("user_habit_id").toString());
            String date = (String) data.get("record_date");
            Double val = Double.valueOf(data.get("fact_value").toString());
            String note = (String) data.get("note");
            reportRepository.insertDetailedRecord(id, date, val, note);
        });
    }

    @PostMapping("/exercise/{userId}")
    public ResponseEntity<?> addExercise(@PathVariable Long userId, @RequestBody Map<String, Object> data) {
        return handleAction(() -> {
            String type = (String) data.get("type");
            Integer dur = Integer.valueOf(data.get("duration").toString());
            Double cal = Double.valueOf(data.get("calories") != null ? data.get("calories").toString() : "0");
            String feel = (String) data.get("feeling");
            String date = (String) data.get("date");
            String note = (String) data.get("note");
            reportRepository.saveExerciseRecord(userId, type, dur, cal, feel, date, note);
        });
    }

    @PostMapping("/health/{userId}")
    public ResponseEntity<?> addHealthData(@PathVariable Long userId, @RequestBody Map<String, Object> data) {
        return handleAction(() -> reportRepository.batchInsertHealthData(userId, List.of(data)));
    }

    @GetMapping("/report/weekly/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getWeeklyReport(@PathVariable Long userId) {
        return ResponseEntity.ok(reportRepository.findWeeklyReport(userId));
    }

    @GetMapping("/report/trend/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getTrend(@PathVariable Long userId) {
        return ResponseEntity.ok(reportRepository.getPunchTrend(userId));
    }

    @GetMapping("/report/health/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getHealthTrend(@PathVariable Long userId) {
        return ResponseEntity.ok(reportRepository.findHealthTrend(userId));
    }

    @GetMapping("/report/exercise/summary/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getExerciseSummary(@PathVariable Long userId) {
        return ResponseEntity.ok(reportRepository.findWeeklyExerciseSummary(userId));
    }

    @GetMapping("/habits/{id}/punch/today")
    public ResponseEntity<Map<String, Double>> getTodayPunch(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("todayValue", reportRepository.findTodayPunchValue(id)));
    }

    @GetMapping("/habits/{id}/punch/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentPunches(@PathVariable Long id) {
        return ResponseEntity.ok(reportRepository.findRecentPunchRecords(id));
    }

    @GetMapping("/report/exercise/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getExerciseHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(reportRepository.findExerciseHistory(userId));
    }

    @GetMapping("/report/punches/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getPunchHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(reportRepository.findHabitPunchHistory(userId));
    }

    @GetMapping("/report/export/{userId}/{yearMonth}")
    public ResponseEntity<String> exportReport(@PathVariable Long userId, @PathVariable String yearMonth) {
        List<Map<String, Object>> data = reportRepository.callMonthlyReport(userId, yearMonth);
        StringBuilder csv = new StringBuilder("类型,项目,总计,完成量\n");
        for (Map<String, Object> row : data) {
            csv.append(row.get("report_type")).append(",").append(row.get("item_name")).append(",").append(row.get("total_days")).append(",").append(row.get("finish_days")).append("\n");
        }
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=report.csv").body(csv.toString());
    }

    @GetMapping("/files/{fileName:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get("uploads").resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"").body(resource);
            return ResponseEntity.notFound().build();
        } catch (Exception e) { return ResponseEntity.internalServerError().build(); }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("userId") Long userId, @RequestParam("relateType") String relateType, @RequestParam("relateId") Long relateId) {
        try {
            return ResponseEntity.ok(Map.of("filePath", attachmentService.saveAttachment(file, userId, relateType, relateId)));
        } catch (Exception e) { return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage())); }
    }
}
