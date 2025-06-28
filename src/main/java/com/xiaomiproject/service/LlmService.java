package com.xiaomiproject.service;

import com.xiaomiproject.dto.LlmRequest;
import com.xiaomiproject.dto.LlmResponse;
import com.xiaomiproject.dto.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;

@Service
public class LlmService {

    // 从 application.properties 注入配置
    @Value("${llm.api.url}")
    private String apiUrl;

    @Value("${llm.api.key}")
    private String apiKey;

    // --- 修改开始 ---
    @Autowired
    private RestTemplate restTemplate; // 不再自己new，而是注入
    // --- 修改结束 ---

    /**
     * 调用真实的大语言模型API
     * @param question 用户的问题
     * @return 大模型的回答
     */
    public String getCompletion(String question) {
        // 1. 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey); // 大部分API使用Bearer Token

        // 2. 构建请求体
        // 请根据您的API文档调整这里的模型名称和角色
        LlmRequest requestBody = new LlmRequest();
        requestBody.setModel("deepseek-chat"); // 示例模型
        requestBody.setMessages(Collections.singletonList(new Message("user", question)));

        // 3. 封装请求
        HttpEntity<LlmRequest> entity = new HttpEntity<>(requestBody, headers);

        try {
            // 4. 发送POST请求并获取响应
            LlmResponse response = restTemplate.postForObject(apiUrl, entity, LlmResponse.class);

            // 5. 解析并返回结果
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                // 通常我们取第一个选择的回答
                return response.getChoices().get(0).getMessage().getContent();
            } else {
                return "抱歉，大模型未能返回有效回答。";
            }
        } catch (RestClientException e) {
            // 处理网络异常或API返回的错误状态码
            System.err.println("调用大模型API时发生错误: " + e.getMessage());
            return "抱歉，连接AI服务时出现问题，请稍后再试。";
        }
    }
}
