package com.xiaomiproject.service;

import com.xiaomiproject.dto.LlmRequest;
import com.xiaomiproject.dto.LlmResponse;
import com.xiaomiproject.dto.Message;
import lombok.var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Collections;

@Service
public class LlmService {

    @Value("${llm.api.url}")
    private String apiUrl;

    @Value("${llm.api.key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    // 注入WebClient Bean
    @Autowired
    private WebClient llmWebClient;

    /**
     * 调用真实的大语言模型API
     * @param question 用户的问题
     * @return 大模型的回答
     */
    public String getCompletion(String question) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        LlmRequest requestBody = new LlmRequest();
        requestBody.setModel("deepseek-chat");
        requestBody.setMessages(Collections.singletonList(new Message("user", question)));

        HttpEntity<LlmRequest> entity = new HttpEntity<>(requestBody, headers);

        try {
            LlmResponse response = restTemplate.postForObject(apiUrl, entity, LlmResponse.class);

            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                return response.getChoices().get(0).getMessage().getContent();
            } else {
                return "抱歉，大模型未能返回有效回答。";
            }
        } catch (RestClientException e) {
            System.err.println("调用大模型API时发生错误: " + e.getMessage());
            return "抱歉，连接AI服务时出现问题，请稍后再试。";
        }
    }

    /**
     * 新增：调用大语言模型API的流式方法
     * @param question 用户的问题
     * @return 包含AI回答数据块的Flux流
     */
    public Flux<String> getCompletionStream(String question) {
        LlmRequest requestBody = new LlmRequest();
        requestBody.setModel("deepseek-chat");
        requestBody.setMessages(Collections.singletonList(new Message("user", question)));
        // 在请求体中增加 stream: true
        // 注意：这需要根据你的LLM API文档来确定具体的请求参数
        // 这里假设你的LLMRequest DTO中增加了 `private boolean stream;`
        // requestBody.setStream(true); // 开启流式

        // 为流式请求创建一个新的请求体，因为模型API可能需要特定参数
        //  "stream": true
        var streamRequestBody = new java.util.HashMap<String, Object>();
        streamRequestBody.put("model", "deepseek-chat");
        streamRequestBody.put("messages", requestBody.getMessages());
        streamRequestBody.put("stream", true); // 关键参数

        return llmWebClient.post()
                .bodyValue(streamRequestBody)
                .retrieve()
                // 将响应体转换为字符串流。每个字符串是一个JSON块。
                .bodyToFlux(String.class)
                // 在这里处理错误
                .onErrorResume(e -> {
                    System.err.println("调用大模型流式API时发生错误: " + e.getMessage());
                    return Flux.just("抱歉，连接AI服务时出现问题，请稍后再试。");
                });
    }
}
