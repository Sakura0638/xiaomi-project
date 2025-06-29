package com.xiaomiproject.service.llm;

import com.xiaomiproject.dto.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class DashScopeApi implements LlmApi {

    @Value("${llm.dashscope.api.url}")
    private String apiUrl;

    @Value("${llm.dashscope.api.key}")
    private String apiKey;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Override
    public String getModelType() {
        return "dashscope"; // 定义模型类型
    }

    @Override
    public Flux<String> getCompletionStream(String question) {
        WebClient webClient = webClientBuilder.baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "qwen-plus");
        requestBody.put("messages", Collections.singletonList(new Message("user", question)));
        requestBody.put("stream", true);

        return webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class);
    }
}
