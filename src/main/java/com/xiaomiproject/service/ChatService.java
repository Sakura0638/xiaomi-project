package com.xiaomiproject.service;

import com.xiaomiproject.dto.ChatResponse;
import com.xiaomiproject.dto.QuestionRequest;
import com.xiaomiproject.entity.ConversationHistory;
import com.xiaomiproject.entity.Knowledge;
import com.xiaomiproject.repository.ConversationHistoryRepository;
import com.xiaomiproject.repository.KnowledgeRepository;
import com.xiaomiproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    @Autowired
    private KnowledgeRepository knowledgeRepository;
    @Autowired
    private ConversationHistoryRepository historyRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LlmService llmService;

    private final Map<String, String> questionCache = new ConcurrentHashMap<>();

    /**
     * 新增：检查本地资源（缓存和知识库）
     * @param question 用户的问题
     * @return 如果找到答案，则返回包含答案的 Optional，否则返回 empty
     */
    public Optional<String> checkLocalSources(String question) {
        // 1. 检查内存缓存
        if (questionCache.containsKey(question)) {
            System.out.println("命中缓存: " + question);
            return Optional.of(questionCache.get(question));
        }

        // 2. 检查本地知识库
        Optional<Knowledge> knowledgeOpt = knowledgeRepository.findByQuestion(question);
        if (knowledgeOpt.isPresent()) {
            System.out.println("命中知识库: " + question);
            String answer = knowledgeOpt.get().getAnswer();
            questionCache.put(question, answer); // 存入缓存
            return Optional.of(answer);
        }

        // 3. 本地未找到
        return Optional.empty();
    }

    public ChatResponse getAnswer(QuestionRequest request, String username) {
        String question = request.getQuestion();
        String conversationId = request.getConversationId();

        if (conversationId == null || conversationId.trim().isEmpty()) {
            conversationId = UUID.randomUUID().toString();
        }

        // 使用新方法检查本地资源
        Optional<String> localAnswerOpt = checkLocalSources(question);
        String answer = localAnswerOpt.orElseGet(() -> {
            // 如果本地没有，则调用大模型
            System.out.println("调用大模型: " + question);
            String llmAnswer = llmService.getCompletion(question);
            questionCache.put(question, llmAnswer); // 存入缓存
            return llmAnswer;
        });

        saveHistory(conversationId, question, answer, username);
        return new ChatResponse(answer, conversationId);
    }

    public void saveHistory(String conversationId, String question, String answer, String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            ConversationHistory history = new ConversationHistory();
            history.setConversationId(conversationId);
            history.setUserId(user.getId());
            history.setQuestion(question);
            history.setAnswer(answer);
            history.setTimestamp(LocalDateTime.now());
            historyRepository.save(history);
        });
    }

    public Flux<String> getAnswerStream(QuestionRequest request, String username) {
        System.out.println("调用大模型 (流式): " + request.getQuestion());
        return llmService.getCompletionStream(request.getQuestion());
    }
}
