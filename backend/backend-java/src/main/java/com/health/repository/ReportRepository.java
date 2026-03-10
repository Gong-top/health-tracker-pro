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

    // --- 报表模块 (增强版：直接查询不依赖视图) ---
    public List<Map<String, Object>> findWeeklyReport(Long userId) {
        String sql = "SELECT " +
                     "  uh.user_habit_id, " +
                     "  coalesce(uh.custom_name, h.habit_name) as fact_name, " +
                     "  uh.target_value as daily_target, " +
                     "  uh.target_unit, " +
                     "  (SELECT coalesce(sum(r.fact_value), 0) FROM record r WHERE r.user_habit_id = uh.user_habit_id AND r.record_date >= date_sub(curdate(), interval 7 day)) as total_finished, " +
                     "  CASE WHEN uh.frequency_unit = 'day' THEN round(uh.target_value * 7, 1) ELSE round(uh.target_value, 1) END as weekly_target " +
                     "FROM user_habit uh " +
                     "JOIN habit h ON uh.habit_id = h.habit_id " +
                     "WHERE uh.user_id = ? AND uh.user_habit_status = true";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, userId);
        // 后写计算百分比逻辑，防止 SQL 除以 0 报错
        for (Map<String, Object> map : list) {
            double finished = ((Number) map.get("total_finished")).doubleValue();
            double target = ((Number) map.get("weekly_target")).doubleValue();
            map.put("completion_rate_num", target > 0 ? Math.round((finished / target) * 1000) / 10.0 : 0);
        }
        return list;
    }

    public List<Map<String, Object>> getPunchTrend(Long userId) {
        // 直接从统计表取数据，不再依赖趋势视图
        String sql = "SELECT sta_date as record_date, finished_habits as daily_total FROM statistic WHERE user_id = ? ORDER BY sta_date ASC LIMIT 7";
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
        String sql = "SELECT r.*, coalesce(uh.custom_name, h.habit_name) as habit_name " +
                     "FROM record r " +
                     "JOIN user_habit uh ON r.user_habit_id = uh.user_habit_id " +
                     "JOIN habit h ON uh.habit_id = h.habit_id " +
                     "WHERE uh.user_id = ? " +
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
                     "FROM user_habit uh " +
                     "JOIN habit h ON uh.habit_id = h.habit_id " +
                     "WHERE uh.user_id = ?"; // 去掉状态过滤，显示所有习惯
        return jdbcTemplate.queryForList(sql, userId);
    }

    public int deleteUserHabit(Long userHabitId) {
        String sql = "DELETE FROM user_habit WHERE user_habit_id = ?";
        return jdbcTemplate.update(sql, userHabitId);
    }

    public int toggleHabitStatus(Long userHabitId, boolean status) {
        String sql = "UPDATE user_habit SET user_habit_status = ?, update_time = CURRENT_TIMESTAMP WHERE user_habit_id = ?";
        return jdbcTemplate.update(sql, status, userHabitId);
    }

    /**
     * 增强版：支持添加习惯，若已存在则直接关联
     */
    public void saveNewHabit(Long userId, String name, Double target, String unit) {
        // 1. 检查习惯是否已在字典表中
        String checkSql = "SELECT habit_id FROM habit WHERE habit_name = ? LIMIT 1";
        List<Long> ids = jdbcTemplate.query(checkSql, (rs, rowNum) -> rs.getLong("habit_id"), name);
        
        Long habitId;
        if (ids.isEmpty()) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO habit (habit_name, category) VALUES (?, '自定义')",
                    Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, name);
                return ps;
            }, keyHolder);
            habitId = keyHolder.getKey().longValue();
        } else {
            habitId = ids.get(0);
        }

        // 2. 检查用户是否已经有了这个习惯（防止重复关联报 500）
        String relationCheck = "SELECT count(*) FROM user_habit WHERE user_id = ? AND habit_id = ?";
        Integer count = jdbcTemplate.queryForObject(relationCheck, Integer.class, userId, habitId);
        
        if (count == 0) {
            String sql = "INSERT INTO user_habit (user_id, habit_id, custom_name, target_value, target_unit, start_date, user_habit_status) " +
                         "VALUES (?, ?, ?, ?, ?, CURDATE(), true)";
            jdbcTemplate.update(sql, userId, habitId, name, target, unit);
        }
    }

    public int updateHabitSettings(Long userHabitId, String name, Double target, String unit, String frequency, String reminderTime) {
        String sql = "UPDATE user_habit SET custom_name = ?, target_value = ?, target_unit = ?, frequency_unit = ?, reminder_time = ?, update_time = CURRENT_TIMESTAMP WHERE user_habit_id = ?";
        return jdbcTemplate.update(sql, name, target, unit, frequency, reminderTime, userHabitId);
    }

    public int insertRecord(Long userHabitId, String recordDate, Double factValue) {
        return insertDetailedRecord(userHabitId, recordDate, factValue, "快速打卡");
    }

    public int insertDetailedRecord(Long userHabitId, String recordDate, Double factValue, String note) {
        String sql = "INSERT INTO record (user_habit_id, record_date, fact_value, note, record_status) " +
                     "VALUES (?, ?, ?, ?, 1) " +
                     "ON DUPLICATE KEY UPDATE fact_value = fact_value + ?, note = ?, record_status = 1, update_time = CURRENT_TIMESTAMP";
        return jdbcTemplate.update(sql, userHabitId, recordDate, factValue, note, factValue, note);
    }

    public int saveExerciseRecord(Long userId, String type, Integer duration, Double calories, String feeling, String date, String note) {
        String sql = "INSERT INTO exercise_record (user_id, sports_id, sports_type, duration, calories, feeling, exercise_record_date, note) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        return jdbcTemplate.update(sql, userId, 1L, type, duration, calories, feeling, date, note);
    }

    public void batchInsertHealthData(Long userId, List<Map<String, Object>> dataList) {
        String sql = "INSERT INTO health_data (user_id, data_type, data_value, data_unit, record_date) VALUES (?, ?, ?, ?, ?)";
        for (Map<String, Object> data : dataList) {
            jdbcTemplate.update(sql, userId, data.get("data_type"), data.get("data_value"), data.get("data_unit"), data.get("record_date"));
        }
    }
}
