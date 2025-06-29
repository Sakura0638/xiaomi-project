package com.xiaomiproject.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaomiproject.dto.ChatResponse;
import com.xiaomiproject.dto.QuestionRequest;
import com.xiaomiproject.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    private final ObjectMapper objectMapper = new ObjectMapper();
    // 使用 CachedThreadPool 以便更好地处理并发请求
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> askQuestion(@RequestBody QuestionRequest request, Principal principal) {
        // ... (此方法保持不变)
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            ChatResponse errorResponse = new ChatResponse("问题内容不能为空。", request.getConversationId());
            return ResponseEntity.badRequest().body(errorResponse);
        }

        String username = principal.getName();
        ChatResponse response = chatService.getAnswer(request, username);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/stream-ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAskQuestion(@RequestParam String question,
                                        @RequestParam(required = false) String conversationId,
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

        sseExecutor.execute(() -> {
            try {
                // --- 核心修改：先检查本地资源 ---
                Optional<String> localAnswer = chatService.checkLocalSources(question);

                if (localAnswer.isPresent()) {
                    // 如果在知识库或缓存中找到答案，直接发送并关闭
                    emitter.send(SseEmitter.event().data(localAnswer.get()));
                    chatService.saveHistory(finalConversationId, question, localAnswer.get(), principal.getName());
                    emitter.complete();
                } else {
                    // 如果本地没有，才调用大模型流式接口
                    StringBuilder fullResponse = new StringBuilder();
                    QuestionRequest request = new QuestionRequest();
                    request.setQuestion(question);
                    request.setConversationId(finalConversationId);

                    chatService.getAnswerStream(request, principal.getName())
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
