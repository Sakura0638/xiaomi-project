package com.xiaomiproject.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaomiproject.dto.QuestionRequest;
import com.xiaomiproject.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Value("${llm.default.model}")
    private String defaultModel;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    @GetMapping(value = "/stream-ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAskQuestion(@RequestParam String question,
                                        @RequestParam(required = false) String conversationId,
                                        @RequestParam(required = false) String model, // 接收模型类型参数
                                        Principal principal) {
        if (principal == null) {
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new IllegalStateException("User not authenticated"));
            return emitter;
        }

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        final String finalConversationId = (conversationId == null || conversationId.trim().isEmpty())
                ? UUID.randomUUID().toString()
                : conversationId;

        // 如果未指定模型，则使用默认模型
        final String modelType = (model == null || model.trim().isEmpty()) ? defaultModel : model;

        sseExecutor.execute(() -> {
            try {
                Optional<String> localAnswer = chatService.checkLocalSources(question);

                if (localAnswer.isPresent()) {
                    emitter.send(SseEmitter.event().data(localAnswer.get()));
                    chatService.saveHistory(finalConversationId, question, localAnswer.get(), principal.getName());
                    emitter.complete();
                } else {
                    StringBuilder fullResponse = new StringBuilder();
                    QuestionRequest request = new QuestionRequest();
                    request.setQuestion(question);
                    request.setConversationId(finalConversationId);

                    chatService.getAnswerStream(request, principal.getName(), modelType) // 传递模型类型
                            .subscribe(
                                    rawContent -> {
                                        String[] chunks = rawContent.split("data: ");
                                        for (String chunk : chunks) {
                                            if (chunk.trim().isEmpty() || "[DONE]".equalsIgnoreCase(chunk.trim())) {
                                                continue;
                                            }
                                            try {
                                                JsonNode rootNode = objectMapper.readTree(chunk);
                                                JsonNode deltaNode = rootNode.path("choices").path(0).path("delta");
                                                if (deltaNode.has("content")) {
                                                    String textContent = deltaNode.get("content").asText();
                                                    if (textContent != null && !textContent.isEmpty() && !"null".equalsIgnoreCase(textContent)) {
                                                        emitter.send(SseEmitter.event().data(textContent));
                                                        fullResponse.append(textContent);
                                                    }
                                                }
                                            } catch (IOException e) {
                                                // 忽略单个块的解析错误
                                            }
                                        }
                                    },
                                    emitter::completeWithError,
                                    () -> {
                                        emitter.complete();
                                        chatService.saveHistory(finalConversationId, question, fullResponse.toString(), principal.getName());
                                    }
                            );
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
