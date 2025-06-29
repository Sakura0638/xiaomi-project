package com.xiaomiproject.service;

import com.xiaomiproject.service.llm.LlmApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LlmService {

    private final Map<String, LlmApi> llmApiMap;
    private final LlmApi defaultLlmApi;

    @Autowired
    public LlmService(List<LlmApi> llmApis, @Value("${llm.default.model}") String defaultModel) {
        // 通过依赖注入，Spring会自动找到所有LlmApi接口的实现类
        this.llmApiMap = llmApis.stream()
                .collect(Collectors.toMap(LlmApi::getModelType, Function.identity()));
        // 设置默认模型
        this.defaultLlmApi = llmApiMap.get(defaultModel);
        if (this.defaultLlmApi == null) {
            throw new IllegalStateException("Default LLM provider not found: " + defaultModel);
        }
    }

    /**
     * 根据模型类型获取对应的API实现
     */
    private LlmApi getLlmApi(String modelType) {
        return llmApiMap.getOrDefault(modelType, defaultLlmApi);
    }

    /**
     * 调用指定模型的流式API
     */
    public Flux<String> getCompletionStream(String question, String modelType) {
        return getLlmApi(modelType).getCompletionStream(question);
    }
}
