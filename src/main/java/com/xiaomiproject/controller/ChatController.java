package com.xiaomiproject.controller;


import com.xiaomiproject.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/ask")
    public ResponseEntity<String> askQuestion(@RequestBody QuestionRequest request, Principal principal) {
        // Principal 对象由 Spring Security 在用户登录后自动注入
        // 它包含了当前认证用户的信息
        if (principal == null) {
            // 理论上 Spring Security 会拦截未认证的请求，但这是一个额外的保险
            return ResponseEntity.status(401).body("未经授权，请先登录。");
        }

        String username = principal.getName();
        String question = request.getQuestion();

        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("问题内容不能为空。");
        }

        String answer = chatService.getAnswer(question, username);

        return ResponseEntity.ok(answer);
    }
}
