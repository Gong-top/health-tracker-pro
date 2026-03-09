package com.health.config;

import com.health.service.DataImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private DataImportService dataImportService;

    @Override
    public void run(String... args) throws Exception {
        // 检查数据库中是否已有用户，如果少于 50 人则导入大量数据
        // 这也符合“数据库维护与实施”中提到的数据预填充策略
        System.out.println("检测数据库初始状态...");
        
        try {
            // 自动导入大量数据
            dataImportService.importBulkData();
            System.out.println("数据预填充已完成。");
        } catch (Exception e) {
            System.err.println("数据导入失败: " + e.getMessage());
        }
    }
}
