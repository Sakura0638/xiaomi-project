package com.xiaomiproject.service;

import com.xiaomiproject.dto.RegisterRequest;
import com.xiaomiproject.entity.User;
import com.xiaomiproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void register(RegisterRequest registerRequest) {
        // 检查用户名是否已存在
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new RuntimeException("Error: Username is already taken!");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        // 使用BCrypt加密密码
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        // 默认角色为USER
        user.setRoles("ROLE_USER");

        userRepository.save(user);
    }
}
