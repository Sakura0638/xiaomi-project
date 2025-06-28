package com.xiaomiproject.service;

import com.xiaomiproject.entity.ConversationHistory;
import com.xiaomiproject.entity.Knowledge;
import com.xiaomiproject.repository.ConversationHistoryRepository;
import com.xiaomiproject.repository.KnowledgeRepository;
import com.xiaomiproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
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
    private LlmService llmService; // 这里会注入我们修改后的真实LlmService

    // 使用ConcurrentHashMap作为简单的内存缓存
    private final Map<String, String> questionCache = new ConcurrentHashMap<>();

    /**
     * 处理用户的提问，并遵循 缓存 -> 知识库 -> 大模型 的顺序
     * @param question 问题
     * @param username 当前登录用户的用户名
     * @return 回答
     */
    public String getAnswer(String question, String username) {
        // 步骤1: 检查内存缓存
        if (questionCache.containsKey(question)) {
            System.out.println("命中缓存: " + question);
            // 注意：命中缓存的回答也应该记录历史
            String cachedAnswer = questionCache.get(question);
            saveHistory(question, cachedAnswer, username);
            return cachedAnswer;
        }

        // 步骤2: 检查本地知识库
        Optional<Knowledge> knowledgeOpt = knowledgeRepository.findByQuestion(question);
        if (knowledgeOpt.isPresent()) {
            System.out.println("命中知识库: " + question);
            String answer = knowledgeOpt.get().getAnswer();
            questionCache.put(question, answer); // 存入缓存
            saveHistory(question, answer, username);
            return answer;
        }

        // 步骤3: 调用大模型
        System.out.println("缓存和知识库未命中，调用大模型: " + question);
        String answer = llmService.getCompletion(question); // 调用真实API
        questionCache.put(question, answer); // 存入缓存
        saveHistory(question, answer, username);

        return answer;
    }

    /**
     * 保存问答历史
     * @param question 问题
     * @param answer 回答
     * @param username 用户名
     */
    private void saveHistory(String question, String answer, String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            ConversationHistory history = new ConversationHistory();
            history.setUserId(user.getId());
            history.setQuestion(question);
            history.setAnswer(answer);
            history.setTimestamp(LocalDateTime.now());
            historyRepository.save(history);
        });
    }
}
