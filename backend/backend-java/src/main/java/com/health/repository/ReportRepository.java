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

    // --- 用户认证模块 ---
    public Map<String, Object> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, username);
        return users.isEmpty() ? null : users.get(0);
    }

    public void saveUser(String username, String password, String nickname, String email) {
        String sql = "INSERT INTO users (username, password, nickname, email) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, username, password, nickname, email);
    }

    public void updateUserProfile(Long userId, String nickname, String email) {
        String sql = "UPDATE users SET nickname = ?, email = ? WHERE id = ?";
        jdbcTemplate.update(sql, nickname, email, userId);
    }

    // --- 报表模块 ---
    public List<Map<String, Object>> findWeeklyReport(Long userId) {
        String sql = "SELECT uh.user_habit_id, coalesce(uh.custom_name, h.habit_name) as fact_name, " +
                     "uh.target_value as daily_target, uh.target_unit, " +
                     "(SELECT coalesce(sum(r.fact_value), 0) FROM record r WHERE r.user_habit_id = uh.user_habit_id AND r.record_date >= date_sub(curdate(), interval 7 day)) as total_finished, " +
                     "CASE WHEN uh.frequency_unit = 'day' THEN round(uh.target_value * 7, 1) ELSE round(uh.target_value, 1) END as weekly_target " +
                     "FROM user_habit uh JOIN habit h ON uh.habit_id = h.habit_id " +
                     "WHERE uh.user_id = ? AND uh.user_habit_status = true";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, userId);
        for (Map<String, Object> map : list) {
            double finished = ((Number) map.get("total_finished")).doubleValue();
            double target = ((Number) map.get("weekly_target")).doubleValue();
            map.put("completion_rate_num", target > 0 ? Math.round((finished / target) * 1000) / 10.0 : 0);
        }
        return list;
    }

    public List<Map<String, Object>> getPunchTrend(Long userId) {
        String sql = "SELECT sta_date as record_date, finished_habits as daily_total FROM statistic WHERE user_id = ? ORDER BY sta_date ASC LIMIT 7";
        return jdbcTemplate.queryForList(sql, userId);
    }

    public List<Map<String, Object>> findWeeklyExerciseSummary(Long userId) {
        String sql = "SELECT coalesce(sum(duration), 0) as total_duration, coalesce(sum(calories), 0) as total_calories " +
                     "FROM exercise_record WHERE user_id = ? AND exercise_record_date >= date_sub(curdate(), interval 7 day)";
        return jdbcTemplate.queryForList(sql, userId);
    }

    public List<Map<String, Object>> findHealthTrend(Long userId) {
        String sql = "SELECT * FROM health_data WHERE user_id = ? ORDER BY record_date DESC, data_id DESC LIMIT 20";
        return jdbcTemplate.queryForList(sql, userId);
    }

    public List<Map<String, Object>> findRecentPunchRecords(Long userHabitId) {
        String sql = "SELECT * FROM record WHERE user_habit_id = ? ORDER BY record_date DESC, record_id DESC LIMIT 5";
        return jdbcTemplate.queryForList(sql, userHabitId);
    }

    public List<Map<String, Object>> findExerciseHistory(Long userId) {
        String sql = "SELECT * FROM exercise_record WHERE user_id = ? ORDER BY exercise_record_date DESC, exercise_record_id DESC LIMIT 20";
        return jdbcTemplate.queryForList(sql, userId);
    }

    public List<Map<String, Object>> findHabitPunchHistory(Long userId) {
        String sql = "SELECT r.*, coalesce(uh.custom_name, h.habit_name) as habit_name FROM record r " +
                     "JOIN user_habit uh ON r.user_habit_id = uh.user_habit_id " +
                     "JOIN habit h ON uh.habit_id = h.habit_id WHERE uh.user_id = ? " +
                     "ORDER BY r.record_date DESC, r.record_id DESC LIMIT 20";
        return jdbcTemplate.queryForList(sql, userId);
    }

    public Double findTodayPunchValue(Long userHabitId) {
        String sql = "SELECT COALESCE(SUM(fact_value), 0) FROM record WHERE user_habit_id = ? AND record_date = CURDATE()";
        return jdbcTemplate.queryForObject(sql, Double.class, userHabitId);
    }

    public List<Map<String, Object>> callMonthlyReport(Long userId, String yearMonth) {
        String sql = "CALL monthly_report(?, ?)";
        return jdbcTemplate.queryForList(sql, userId, yearMonth);
    }

    public List<Map<String, Object>> findUserHabits(Long userId) {
        String sql = "SELECT uh.*, h.habit_name as base_name, coalesce(uh.custom_name, h.habit_name) as habit_name " +
                     "FROM user_habit uh JOIN habit h ON uh.habit_id = h.habit_id WHERE uh.user_id = ?";
        return jdbcTemplate.queryForList(sql, userId);
    }

    public int deleteUserHabit(Long userHabitId) {
        return jdbcTemplate.update("DELETE FROM user_habit WHERE user_habit_id = ?", userHabitId);
    }

    public int toggleHabitStatus(Long userHabitId, boolean status) {
        return jdbcTemplate.update("UPDATE user_habit SET user_habit_status = ?, update_time = CURRENT_TIMESTAMP WHERE user_habit_id = ?", status, userHabitId);
    }

    public void saveNewHabit(Long userId, String name, Double target, String unit) {
        try {
            String findHabitSql = "SELECT habit_id FROM habit WHERE habit_name = ? LIMIT 1";
            List<Long> habitIds = jdbcTemplate.query(findHabitSql, (rs, rowNum) -> rs.getLong("habit_id"), name);
            Long habitId;
            if (habitIds.isEmpty()) {
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement("INSERT INTO habit (habit_name, category) VALUES (?, '自定义')", Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, name);
                    return ps;
                }, keyHolder);
                habitId = keyHolder.getKey().longValue();
            } else {
                habitId = habitIds.get(0);
            }
            String checkRelation = "SELECT count(*) FROM user_habit WHERE user_id = ? AND habit_id = ?";
            if (jdbcTemplate.queryForObject(checkRelation, Integer.class, userId, habitId) == 0) {
                jdbcTemplate.update("INSERT INTO user_habit (user_id, habit_id, custom_name, target_value, target_unit, start_date, user_habit_status) VALUES (?, ?, ?, ?, ?, CURDATE(), true)", userId, habitId, name, target, unit);
            } else {
                jdbcTemplate.update("UPDATE user_habit SET target_value = ?, target_unit = ?, user_habit_status = true WHERE user_id = ? AND habit_id = ?", target, unit, userId, habitId);
            }
        } catch (Exception e) { throw new RuntimeException("数据库写入失败"); }
    }

    public int updateHabitSettings(Long id, String name, Double target, String unit, String freq, String time) {
        return jdbcTemplate.update("UPDATE user_habit SET custom_name = ?, target_value = ?, target_unit = ?, frequency_unit = ?, reminder_time = ?, update_time = CURRENT_TIMESTAMP WHERE user_habit_id = ?", name, target, unit, freq, time, id);
    }

    public int insertRecord(Long id, String date, Double val) { return insertDetailedRecord(id, date, val, "快速打卡"); }

    public int insertDetailedRecord(Long id, String date, Double val, String note) {
        return jdbcTemplate.update("INSERT INTO record (user_habit_id, record_date, fact_value, note, record_status) VALUES (?, ?, ?, ?, 1) ON DUPLICATE KEY UPDATE fact_value = fact_value + ?, note = ?, record_status = 1, update_time = CURRENT_TIMESTAMP", id, date, val, note, val, note);
    }

    public int saveExerciseRecord(Long userId, String type, Integer dur, Double cal, String feel, String date, String note) {
        return jdbcTemplate.update("INSERT INTO exercise_record (user_id, sports_id, sports_type, duration, calories, feeling, exercise_record_date, note) VALUES (?, 1, ?, ?, ?, ?, ?, ?)", userId, type, dur, cal, feel, date, note);
    }

    public void batchInsertHealthData(Long userId, List<Map<String, Object>> dataList) {
        for (Map<String, Object> data : dataList) {
            jdbcTemplate.update("INSERT INTO health_data (user_id, data_type, data_value, data_unit, record_date) VALUES (?, ?, ?, ?, ?)", userId, data.get("data_type"), data.get("data_value"), data.get("data_unit"), data.get("record_date"));
        }
    }
}
