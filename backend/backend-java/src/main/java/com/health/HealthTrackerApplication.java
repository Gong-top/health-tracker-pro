package com.health;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HealthTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(HealthTrackerApplication.class, args);
        System.out.println("-------------------------------------------");
        System.out.println("健康管理系统后端启动成功！端口: 8080");
        System.out.println("-------------------------------------------");
    }
}
