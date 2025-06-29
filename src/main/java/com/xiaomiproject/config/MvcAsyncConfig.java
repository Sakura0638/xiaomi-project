package com.xiaomiproject.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcAsyncConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 配置一个更高效的线程池来处理异步请求（包括SSE和响应式类型）
        configurer.setTaskExecutor(mvcTaskExecutor());
        // 可以设置默认的超时时间
        // configurer.setDefaultTimeout(30_000L);
    }

    @Bean
    public AsyncTaskExecutor mvcTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 根据您的服务器性能和预期负载进行调整
        executor.setCorePoolSize(5);      // 核心线程数
        executor.setMaxPoolSize(20);     // 最大线程数
        executor.setQueueCapacity(100);  // 任务队列容量
        executor.setThreadNamePrefix("mvc-async-");
        executor.initialize();
        return executor;
    }
}
