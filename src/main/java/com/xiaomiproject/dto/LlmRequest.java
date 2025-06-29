package com.xiaomiproject.dto;

import lombok.Data;
import java.util.List;

@Data
public class LlmRequest {
    private String model;
    private List<Message> messages;
    private boolean stream; // 新增字段
    // 可能还有 temperature, max_tokens 等参数，根据需要添加
}
