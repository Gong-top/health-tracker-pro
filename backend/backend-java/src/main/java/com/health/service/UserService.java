package com.health.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BCryptPasswordEncoder encoder;

    public int register(String username, String password, String nickname) {
        String encodedPassword = encoder.encode(password);
        String sql = "INSERT INTO users (username, password, nickname) VALUES (?, ?, ?)";
        return jdbcTemplate.update(sql, username, encodedPassword, nickname);
    }

    public Map<String, Object> login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ?";
        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, username);
        
        if (!users.isEmpty() && encoder.matches(password, (String) users.get(0).get("password"))) {
            return users.get(0);
        }
        return null;
    }

    public int updateProfile(Long userId, String nickname, String email, String gender) {
        String sql = "UPDATE users SET nickname = ?, email = ?, gender = ? WHERE id = ?";
        return jdbcTemplate.update(sql, nickname, email, gender, userId);
    }
}
