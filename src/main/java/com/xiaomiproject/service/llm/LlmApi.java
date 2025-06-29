package com.xiaomiproject.service.llm;


import com.xiaomiproject.dto.ChatResponse;
import com.xiaomiproject.dto.QuestionRequest;

public interface LlmApi {

    /**
     * 获取当前实现的模型类型
     * @return 模型类型的唯一标识符 (例如 "deepseek-chat", "gpt-3.5-turbo")
     */
    String getModelType();

    /**
     * 调用大模型API获取回答
     * @param request 包含问题和对话上下文的请求
     * @return 包含回答和对话ID的响应
     */
    ChatResponse getCompletion(QuestionRequest request);
}
