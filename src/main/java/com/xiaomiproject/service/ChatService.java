package com.xiaomiproject.service;

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

    public Optional<String> checkLocalSources(String question) {
        if (questionCache.containsKey(question)) {
            System.out.println("命中缓存: " + question);
            return Optional.of(questionCache.get(question));
        }

        Optional<Knowledge> knowledgeOpt = knowledgeRepository.findByQuestion(question);
        if (knowledgeOpt.isPresent()) {
            System.out.println("命中知识库: " + question);
            String answer = knowledgeOpt.get().getAnswer();
            questionCache.put(question, answer);
            return Optional.of(answer);
        }

        return Optional.empty();
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

    public Flux<String> getAnswerStream(QuestionRequest request, String username, String modelType) {
        System.out.println("调用大模型 (流式): " + request.getQuestion() + " 使用模型: " + modelType);
        return llmService.getCompletionStream(request.getQuestion(), modelType);
    }
}
