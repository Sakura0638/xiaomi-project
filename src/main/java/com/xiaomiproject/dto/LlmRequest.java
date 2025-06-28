package com.xiaomiproject.dto;

import lombok.Data;
import java.util.List;

@Data
public class LlmRequest {
    // 假设您的API需要模型名称和消息列表
    private String model;
    private List<Message> messages;
    // 可能还有 temperature, max_tokens 等参数，根据需要添加
}
