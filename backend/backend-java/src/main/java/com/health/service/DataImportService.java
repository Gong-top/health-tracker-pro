package com.health.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Service
public class DataImportService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Random random = new Random();

    @Transactional
    public void importBulkData() {
        System.out.println("开始批量导入数据...");

        // 1. 批量插入用户 (使用 INSERT IGNORE 防止重复运行报错)
        String userSql = "INSERT IGNORE INTO users (id, username, password, nickname, gender) VALUES (?, ?, ?, ?, ?)";
        List<Object[]> users = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            users.add(new Object[]{
                (long)i + 2, // 避开前两个手动插入的ID
                "user_" + i, 
                "123456", 
                "测试用户" + i, 
                i % 2 == 0 ? "male" : "female"
            });
        }
        jdbcTemplate.batchUpdate(userSql, users);
        System.out.println("用户数据处理完成");

        // 2. 插入基础习惯 (使用 INSERT IGNORE)
        String habitSql = "INSERT IGNORE INTO habit (habit_id, habit_name, description, category) VALUES (?, ?, ?, ?)";
        String[] categories = {"运动", "学习", "生活", "健康"};
        String[] habitNames = {"深蹲", "背单词", "早起", "多喝水", "冥想", "拉伸", "阅读", "仰卧起坐", "戒糖", "早睡"};
        for (int i = 0; i < habitNames.length; i++) {
            jdbcTemplate.update(habitSql, i + 1, habitNames[i], habitNames[i] + "的描述", categories[random.nextInt(categories.length)]);
        }

        // 3. 为每个用户分配唯一习惯 (修复 Duplicate Entry 问题)
        String userHabitSql = "INSERT IGNORE INTO user_habit (user_id, habit_id, target_value, target_unit, start_date) VALUES (?, ?, ?, ?, ?)";
        for (long userId = 1; userId <= 52; userId++) {
            int habitsCount = 3 + random.nextInt(3);
            Set<Integer> selectedHabits = new HashSet<>();
            while (selectedHabits.size() < habitsCount) {
                selectedHabits.add(random.nextInt(10) + 1);
            }
            for (Integer habitId : selectedHabits) {
                jdbcTemplate.update(userHabitSql, userId, (long)habitId, 1.0, "次", LocalDate.now().minusDays(30));
            }
        }
        System.out.println("用户习惯分配完成");

        // 4. 批量生成 30 天打卡记录
        List<Long> userHabitIds = jdbcTemplate.queryForList("SELECT user_habit_id FROM user_habit", Long.class);
        String recordSql = "INSERT IGNORE INTO record (user_habit_id, record_date, fact_value, record_status, note) VALUES (?, ?, ?, ?, ?)";
        List<Object[]> records = new ArrayList<>();
        
        LocalDate today = LocalDate.now();
        for (Long uhId : userHabitIds) {
            for (int day = 0; day < 30; day++) {
                LocalDate date = today.minusDays(day);
                int status = random.nextDouble() > 0.3 ? 1 : 0;
                records.add(new Object[]{uhId, date, status == 1 ? 1.0 : 0.0, status, "自动生成的记录"});
                
                if (records.size() >= 1000) {
                    jdbcTemplate.batchUpdate(recordSql, records);
                    records.clear();
                }
            }
        }
        if (!records.isEmpty()) jdbcTemplate.batchUpdate(recordSql, records);
        System.out.println("打卡记录同步完成");

        System.out.println("批量导入成功！你可以刷新前端查看图表了。");
    }
}
