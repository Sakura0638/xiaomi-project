package com.xiaomiproject.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                // 设置连接超时时间，单位为毫秒。例如10秒。
                .setConnectTimeout(Duration.ofMillis(10000))
                // 设置读取超时时间，单位为毫秒。这个是关键！我们设置为100秒。
                .setReadTimeout(Duration.ofMillis(100000))
                .build();
    }
}
