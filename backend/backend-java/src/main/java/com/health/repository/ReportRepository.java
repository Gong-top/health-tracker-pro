package com.health.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Repository
public class ReportRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Map<String, Object> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, username);
        return users.isEmpty() ? null : users.get(0);
    }

    public void saveUser(String username, String password, String nickname, String email) {
        jdbcTemplate.update("INSERT INTO users (username, password, nickname, email) VALUES (?, ?, ?, ?)", username, password, nickname, email);
    }

    public void updateUserProfile(Long userId, String nickname, String email) {
        jdbcTemplate.update("UPDATE users SET nickname = ?, email = ? WHERE id = ?", nickname, email, userId);
    }

    public List<Map<String, Object>> findWeeklyReport(Long userId) {
        String sql = "SELECT uh.user_habit_id, coalesce(uh.custom_name, h.habit_name) as fact_name, " +
                     "uh.target_value as daily_target, uh.target_unit, " +
                     "(SELECT coalesce(sum(r.fact_value), 0) FROM record r WHERE r.user_habit_id = uh.user_habit_id AND r.record_date >= date_sub(curdate(), interval 7 day)) as total_finished, " +
                     "CASE WHEN uh.frequency_unit = 'day' THEN round(uh.target_value * 7, 1) ELSE round(uh.target_value, 1) END as weekly_target " +
                     "FROM user_habit uh JOIN habit h ON uh.habit_id = h.habit_id " +
                     "WHERE uh.user_id = ? AND uh.user_habit_status = true";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, userId);
        for (Map<String, Object> map : list) {
            double finished = map.get("total_finished") != null ? ((Number) map.get("total_finished")).doubleValue() : 0;
            double target = map.get("weekly_target") != null ? ((Number) map.get("weekly_target")).doubleValue() : 1;
            map.put("completion_rate_num", target > 0 ? Math.round((finished / target) * 1000) / 10.0 : 0);
        }
        return list;
    }

    public List<Map<String, Object>> getPunchTrend(Long userId) {
        return jdbcTemplate.queryForList("SELECT sta_date as record_date, finished_habits as daily_total FROM statistic WHERE user_id = ? ORDER BY sta_date ASC LIMIT 7", userId);
    }

    public List<Map<String, Object>> findWeeklyExerciseSummary(Long userId) {
        return jdbcTemplate.queryForList("SELECT coalesce(sum(duration), 0) as total_duration, coalesce(sum(calories), 0) as total_calories FROM exercise_record WHERE user_id = ? AND exercise_record_date >= date_sub(curdate(), interval 7 day)", userId);
    }

    public void saveNewHabit(Long userId, String name, Double target, String unit, String frequency) {
        if (name == null || name.trim().isEmpty()) throw new RuntimeException("习惯名称不能为空");
        List<Map<String, Object>> habits = jdbcTemplate.queryForList("SELECT habit_id FROM habit WHERE habit_name = ?", name);
        Long habitId;
        if (habits.isEmpty()) {
            KeyHolder kh = new GeneratedKeyHolder();
            jdbcTemplate.update(c -> {
                PreparedStatement ps = c.prepareStatement("INSERT INTO habit (habit_name, category) VALUES (?, '自定义')", Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, name);
                return ps;
            }, kh);
            habitId = kh.getKey().longValue();
        } else {
            habitId = ((Number) habits.get(0).get("habit_id")).longValue();
        }
        String sql = "INSERT INTO user_habit (user_id, habit_id, custom_name, target_value, target_unit, frequency_unit, start_date, user_habit_status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, CURDATE(), true) " +
                     "ON DUPLICATE KEY UPDATE target_value = VALUES(target_value), target_unit = VALUES(target_unit), frequency_unit = VALUES(frequency_unit), user_habit_status = true";
        jdbcTemplate.update(sql, userId, habitId, name, target, unit, (frequency != null ? frequency : "day"));
    }

    public void updateHabitSettings(Long id, String name, Double target, String unit, String freq, String time) {
        jdbcTemplate.update("UPDATE user_habit SET custom_name = ?, target_value = ?, target_unit = ?, frequency_unit = ?, reminder_time = ?, update_time = CURRENT_TIMESTAMP WHERE user_habit_id = ?", name, target, unit, freq, time, id);
    }

    public void deleteUserHabit(Long id) {
        jdbcTemplate.update("DELETE FROM user_habit WHERE user_habit_id = ?", id);
    }

    public void toggleHabitStatus(Long id, boolean status) {
        jdbcTemplate.update("UPDATE user_habit SET user_habit_status = ?, update_time = CURRENT_TIMESTAMP WHERE user_habit_id = ?", status, id);
    }

    public void insertDetailedRecord(Long id, String date, Double val, String note) {
        jdbcTemplate.update("INSERT INTO record (user_habit_id, record_date, fact_value, note, record_status) VALUES (?, ?, ?, ?, 1) ON DUPLICATE KEY UPDATE fact_value = fact_value + ?, note = ?, record_status = 1, update_time = CURRENT_TIMESTAMP", id, date, val, note, val, note);
    }

    public void saveExerciseRecord(Long userId, String type, Integer dur, Double cal, String feel, String date, String note) {
        jdbcTemplate.update("INSERT INTO exercise_record (user_id, sports_id, sports_type, duration, calories, feeling, exercise_record_date, note) VALUES (?, 1, ?, ?, ?, ?, ?, ?)", userId, type, dur, cal, feel, date, note);
    }

    public void batchInsertHealthData(Long userId, List<Map<String, Object>> dataList) {
        for (Map<String, Object> data : dataList) {
            jdbcTemplate.update("INSERT INTO health_data (user_id, data_type, data_value, data_unit, record_date) VALUES (?, ?, ?, ?, ?)", userId, data.get("data_type"), data.get("data_value"), data.get("data_unit"), data.get("record_date"));
        }
    }

    public List<Map<String, Object>> findUserHabits(Long userId) {
        return jdbcTemplate.queryForList("SELECT uh.*, h.habit_name as base_name, coalesce(uh.custom_name, h.habit_name) as habit_name FROM user_habit uh JOIN habit h ON uh.habit_id = h.habit_id WHERE uh.user_id = ?", userId);
    }

    public Double findTodayPunchValue(Long id) {
        return jdbcTemplate.queryForObject("SELECT COALESCE(SUM(fact_value), 0) FROM record WHERE user_habit_id = ? AND record_date = CURDATE()", Double.class, id);
    }

    public List<Map<String, Object>> findRecentPunchRecords(Long id) {
        return jdbcTemplate.queryForList("SELECT * FROM record WHERE user_habit_id = ? ORDER BY record_date DESC LIMIT 5", id);
    }

    public List<Map<String, Object>> callMonthlyReport(Long userId, String yearMonth) {
        return jdbcTemplate.queryForList("CALL monthly_report(?, ?)", userId, yearMonth);
    }

    public List<Map<String, Object>> findHealthTrend(Long userId) {
        return jdbcTemplate.queryForList("SELECT * FROM health_data WHERE user_id = ? ORDER BY record_date DESC LIMIT 20", userId);
    }

    public List<Map<String, Object>> findExerciseHistory(Long userId) {
        return jdbcTemplate.queryForList("SELECT * FROM exercise_record WHERE user_id = ? ORDER BY exercise_record_date DESC LIMIT 20", userId);
    }

    public List<Map<String, Object>> findHabitPunchHistory(Long userId) {
        return jdbcTemplate.queryForList("SELECT r.*, coalesce(uh.custom_name, h.habit_name) as habit_name FROM record r JOIN user_habit uh ON r.user_habit_id = uh.user_habit_id JOIN habit h ON uh.habit_id = h.habit_id WHERE uh.user_id = ? ORDER BY r.record_date DESC LIMIT 20", userId);
    }
}
