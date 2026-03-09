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

    // --- 用户模块 ---
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
        String username = loginData.get("username");
        String password = loginData.get("password");
        
        Map<String, Object> user = reportRepository.findByUsername(username);
        if (user != null && password.equals(user.get("password"))) {
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.status(401).body(Map.of("error", "用户名或密码错误"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> regData) {
        try {
            reportRepository.saveUser(
                regData.get("username"),
                regData.get("password"),
                regData.get("nickname"),
                regData.get("email")
            );
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", "注册失败，用户名或邮箱可能已存在"));
        }
    }

    @PutMapping("/user/profile/{userId}")
    public ResponseEntity<?> updateProfile(@PathVariable Long userId, @RequestBody Map<String, String> data) {
        reportRepository.updateUserProfile(userId, data.get("nickname"), data.get("email"));
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    // 新增：查看/下载附件接口
    @GetMapping("/files/{fileName:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get("uploads").resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("application/octet-stream"))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 1. 仪表盘周报接口
    @GetMapping("/report/weekly/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getWeeklyReport(@PathVariable Long userId) {
        return ResponseEntity.ok(reportRepository.findWeeklyReport(userId));
    }

    // 2. 仪表盘趋势图接口
    @GetMapping("/report/trend/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getTrend(@PathVariable Long userId) {
        return ResponseEntity.ok(reportRepository.getPunchTrend(userId));
    }

    // 3. 健康趋势图接口
    @GetMapping("/report/health/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getHealthTrend(@PathVariable Long userId) {
        return ResponseEntity.ok(reportRepository.findHealthTrend(userId));
    }

    // 4. 月度报告接口
    @GetMapping("/report/monthly/{userId}/{yearMonth}")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyReport(
            @PathVariable Long userId, @PathVariable String yearMonth) {
        return ResponseEntity.ok(reportRepository.callMonthlyReport(userId, yearMonth));
    }

    // 5. 导出 CSV 报告
    @GetMapping("/report/export/{userId}/{yearMonth}")
    public ResponseEntity<String> exportReport(@PathVariable Long userId, @PathVariable String yearMonth) {
        List<Map<String, Object>> data = reportRepository.callMonthlyReport(userId, yearMonth);
        StringBuilder csv = new StringBuilder("类型,项目,总计,完成量\n");
        for (Map<String, Object> row : data) {
            csv.append(row.get("report_type")).append(",")
               .append(row.get("item_name")).append(",")
               .append(row.get("total_days")).append(",")
               .append(row.get("finish_days")).append("\n");
        }
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=report.csv")
                .body(csv.toString());
    }

    // 6. 打卡保存接口
    @PostMapping("/record")
    public ResponseEntity<Map<String, String>> createRecord(@RequestBody Map<String, Object> recordData) {
        reportRepository.insertRecord(
            Long.valueOf(recordData.get("user_habit_id").toString()),
            (String) recordData.get("record_date"),
            Double.valueOf(recordData.get("fact_value").toString())
        );
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    // 7. 详细打卡接口 (带备注)
    @PostMapping("/record/detail")
    public ResponseEntity<Map<String, String>> createDetailedRecord(@RequestBody Map<String, Object> data) {
        reportRepository.insertDetailedRecord(
            Long.valueOf(data.get("user_habit_id").toString()),
            (String) data.get("record_date"),
            Double.valueOf(data.get("fact_value").toString()),
            (String) data.get("note")
        );
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    // 8. 习惯管理接口
    @GetMapping("/habits/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getUserHabits(@PathVariable Long userId) {
        return ResponseEntity.ok(reportRepository.findUserHabits(userId));
    }

    @PostMapping("/habits/{userId}")
    public ResponseEntity<Map<String, String>> addHabit(
            @PathVariable Long userId, @RequestBody Map<String, Object> habitData) {
        reportRepository.saveNewHabit(
            userId, 
            (String) habitData.get("habitName"), 
            Double.valueOf(habitData.get("targetValue").toString()), 
            (String) habitData.get("targetUnit")
        );
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/habits/{id}/status")
    public ResponseEntity<Void> toggleStatus(@PathVariable Long id, @RequestParam boolean enabled) {
        reportRepository.toggleHabitStatus(id, enabled);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/habits/{id}/settings")
    public ResponseEntity<Map<String, String>> updateHabitSettings(
            @PathVariable Long id, @RequestBody Map<String, Object> settings) {
        reportRepository.updateHabitSettings(
            id,
            (String) settings.get("habitName"),
            Double.valueOf(settings.get("targetValue").toString()),
            (String) settings.get("targetUnit"),
            (String) (settings.get("frequency") != null ? settings.get("frequency") : "day"),
            (String) (settings.get("reminderTime") != null ? settings.get("reminderTime") : "08:00:00")
        );
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/habits/{userHabitId}")
    public ResponseEntity<Map<String, String>> deleteHabit(@PathVariable Long userHabitId) {
        reportRepository.deleteUserHabit(userHabitId);
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/habits/{id}/punch/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentPunches(@PathVariable Long id) {
        return ResponseEntity.ok(reportRepository.findRecentPunchRecords(id));
    }

    @GetMapping("/habits/{id}/punch/today")
    public ResponseEntity<Map<String, Double>> getTodayPunch(@PathVariable Long id) {
        Map<String, Double> res = new HashMap<>();
        res.put("todayValue", reportRepository.findTodayPunchValue(id));
        return ResponseEntity.ok(res);
    }

    // 9. 运动记录录入
    @PostMapping("/exercise/{userId}")
    public ResponseEntity<Map<String, String>> addExercise(
            @PathVariable Long userId, @RequestBody Map<String, Object> data) {
        reportRepository.saveExerciseRecord(
            userId,
            (String) data.get("type"),
            Integer.valueOf(data.get("duration").toString()),
            Double.valueOf(data.get("calories") != null ? data.get("calories").toString() : "0"),
            (String) data.get("feeling"),
            (String) data.get("date"),
            (String) data.get("note")
        );
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    // 10. 健康数据录入
    @PostMapping("/health/{userId}")
    public ResponseEntity<Map<String, String>> addHealthData(
            @PathVariable Long userId, @RequestBody Map<String, Object> data) {
        reportRepository.batchInsertHealthData(userId, List.of(data));
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/report/exercise/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getExerciseHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(reportRepository.findExerciseHistory(userId));
    }

    @GetMapping("/report/punches/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getPunchHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(reportRepository.findHabitPunchHistory(userId));
    }

    @GetMapping("/report/exercise/summary/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getExerciseSummary(@PathVariable Long userId) {
        return ResponseEntity.ok(reportRepository.findWeeklyExerciseSummary(userId));
    }

    // 11. 附件上传
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId,
            @RequestParam("relateType") String relateType,
            @RequestParam("relateId") Long relateId) {
        try {
            String filePath = attachmentService.saveAttachment(file, userId, relateType, relateId);
            Map<String, String> response = new HashMap<>();
            response.put("filePath", filePath);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
