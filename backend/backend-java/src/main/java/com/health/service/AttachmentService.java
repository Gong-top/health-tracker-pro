package com.health.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class AttachmentService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 保存文件到服务器并记录元数据到数据库 attachment 表
     * 满足：数据库存储文件/附件类型数据要求
     */
    public String saveAttachment(MultipartFile file, Long userId, String relateType, Long relateId) throws Exception {
        // 1. 物理存储
        String originalName = file.getOriginalFilename();
        String fileName = UUID.randomUUID().toString() + "_" + originalName;
        
        // 确保目录存在
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        Path filePath = uploadPath.resolve(fileName);
        Files.write(filePath, file.getBytes());

        // 2. 数据库持久化 (契合附件表结构)
        String sql = "INSERT INTO attachment (user_id, file_name, file_path, file_type, file_size, relate_type, relate_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        jdbcTemplate.update(sql, 
            userId, 
            originalName, 
            filePath.toString(), 
            file.getContentType(), 
            file.getSize(), 
            relateType, 
            relateId
        );

        return fileName;
    }
}
