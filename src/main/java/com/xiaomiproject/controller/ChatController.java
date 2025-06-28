package com.xiaomiproject.controller;

import com.xiaomiproject.dto.ChatResponse;
import com.xiaomiproject.dto.QuestionRequest;
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

    /**
     * 处理用户的提问请求，支持多轮对话
     * @param request 包含问题和可选的 conversationId
     * @param principal Spring Security提供的当前用户信息
     * @return 包含AI回答和 conversationId 的响应
     */
    @PostMapping("/ask")
    // 1. 修改方法签名，返回类型为 ResponseEntity<ChatResponse>
    public ResponseEntity<ChatResponse> askQuestion(@RequestBody QuestionRequest request, Principal principal) {
        // 2. 权限和参数校验
        if (principal == null) {
            // 对于实际的401错误，最好通过Spring Security的异常处理来全局管理，
            // 但在这里返回一个空的ChatResponse并设置状态码也是一种方式。
            // 不过，理论上Security会先拦截，走不到这里。
            return ResponseEntity.status(401).build();
        }

        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            // 如果请求无效，可以返回一个包含错误信息的ChatResponse
            ChatResponse errorResponse = new ChatResponse("问题内容不能为空。", request.getConversationId());
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // 3. 执行核心逻辑（只保留第二套逻辑）
        String username = principal.getName();
        ChatResponse response = chatService.getAnswer(request, username);

        // 4. 返回正确的响应
        return ResponseEntity.ok(response);
    }
}
