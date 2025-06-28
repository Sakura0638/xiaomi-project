package com.xiaomiproject.dto;

import lombok.Data;

@Data
public class QuestionRequest {
    // 确保前端发送的JSON对象的键是 "question"
    private String question;
    private String conversationId;
}
