package com.xiaomiproject.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
public class ConversationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    @Column(columnDefinition = "TEXT")
    private String question;
    @Column(columnDefinition = "TEXT")
    private String answer;
    // 使用Java 8的LocalDateTime
    private LocalDateTime timestamp;
    @Column(nullable = false)
    private String conversationId; // 用于标识同一组对话
}
