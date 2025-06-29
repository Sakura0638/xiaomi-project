package com.xiaomiproject.service.llm;

import reactor.core.publisher.Flux;

public interface LlmApi {

    /**
     * 获取当前实现的模型类型
     * @return 模型类型的唯一标识符 (例如 "deepseek-chat", "gpt-3.5-turbo")
     */
    String getModelType();

    /**
     * 调用大模型API获取流式回答
     * @param question 用户的问题
     * @return 包含AI回答数据块的Flux流
     */
    Flux<String> getCompletionStream(String question);
}
