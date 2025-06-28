package com.xiaomiproject.controller;

import com.xiaomiproject.dto.LoginRequest;
import com.xiaomiproject.dto.RegisterRequest;
import com.xiaomiproject.repository.UserRepository;
import com.xiaomiproject.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        try {
            authService.register(registerRequest);
            return ResponseEntity.ok("用户注册成功!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        // 使用AuthenticationManager进行认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        // 如果认证成功，将Authentication对象设置到SecurityContext中
        // Spring Security会自动处理Session，并在响应头中返回Set-Cookie: JSESSIONID=...
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // --- 新增逻辑：更新最后登录时间 ---
        // 认证成功后，从 authentication 对象中获取用户名
        String username = authentication.getName();
        // 使用 Java 8 的 Optional 和 Lambda 表达式，代码更优雅
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLoginTime(LocalDateTime.now());
            userRepository.save(user); // 保存更新后的用户信息
        });

        return ResponseEntity.ok("用户登录成功!");
    }
}
