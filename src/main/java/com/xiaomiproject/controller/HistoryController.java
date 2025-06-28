package com.xiaomiproject.controller;


import com.xiaomiproject.entity.ConversationHistory;
import com.xiaomiproject.entity.User;
import com.xiaomiproject.repository.ConversationHistoryRepository;
import com.xiaomiproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    @Autowired
    private ConversationHistoryRepository historyRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ConversationHistory>> getUserHistory(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build(); // 未授权
        }

        // 根据用户名查找用户
        Optional<User> userOpt = userRepository.findByUsername(principal.getName());
        if (!userOpt.isPresent()) {
            // 理论上不会发生，因为用户已登录
            return ResponseEntity.status(404).body(Collections.emptyList());
        }

        // 获取用户ID并查询历史记录
        Long userId = userOpt.get().getId();
        List<ConversationHistory> histories = historyRepository.findByUserIdOrderByTimestampDesc(userId);

        // --- 使用Java 8 Stream API进行聚合 ---
        // 1. 按 conversationId 分组
        // 2. 对于每组，取时间戳最早的那条记录（即该对话的第一个问题）
        // 3. 收集成一个新的List
        List<ConversationHistory> distinctConversations = histories.stream()
                .collect(Collectors.groupingBy(
                        ConversationHistory::getConversationId,
                        // 找到每组中时间戳最早的那个
                        Collectors.minBy(Comparator.comparing(ConversationHistory::getTimestamp))
                ))
                .values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                // 按时间倒序排列聚合后的对话列表
                .sorted(Comparator.comparing(ConversationHistory::getTimestamp).reversed())
                .collect(Collectors.toList());

        return ResponseEntity.ok(distinctConversations);
    }
    @GetMapping("/{conversationId}")
    public ResponseEntity<List<ConversationHistory>> getConversationDetails(@PathVariable String conversationId, Principal principal) {
        // (可选) 可以在这里加一层权限校验，确保只有该用户能查看自己的对话
        List<ConversationHistory> conversation = historyRepository.findByConversationIdOrderByTimestampAsc(conversationId);
        return ResponseEntity.ok(conversation);
    }
}
