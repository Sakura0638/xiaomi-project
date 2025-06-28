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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

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
        // --- 新增：为本次对话生成唯一ID ---
        String conversationId = UUID.randomUUID().toString();
        // 注意：现在需要将 conversationId 传递给 saveHistory 方法
        // 缓存逻辑暂时不变，但如果需要缓存整个对话，这里也需要调整
        // 步骤1: 检查内存缓存
        if (questionCache.containsKey(question)) {
            System.out.println("命中缓存: " + question);
            // 注意：命中缓存的回答也应该记录历史
            String cachedAnswer = questionCache.get(question);
            saveHistory(conversationId, question, cachedAnswer, username);
            return cachedAnswer;
        }

        // 步骤2: 检查本地知识库
        Optional<Knowledge> knowledgeOpt = knowledgeRepository.findByQuestion(question);
        if (knowledgeOpt.isPresent()) {
            System.out.println("命中知识库: " + question);
            String answer = knowledgeOpt.get().getAnswer();
            questionCache.put(question, answer); // 存入缓存
            saveHistory(conversationId, question, answer, username);
            return answer;
        }

        // 步骤3: 调用大模型
        System.out.println("缓存和知识库未命中，调用大模型: " + question);
        String answer = llmService.getCompletion(question); // 调用真实API
        questionCache.put(question, answer); // 存入缓存
        saveHistory(conversationId, question, answer, username);

        return answer;
    }

    /**
     * 保存问答历史
     * @param question 问题
     * @param answer 回答
     * @param username 用户名
     */
    private void saveHistory(String conversationId,String question, String answer, String username) {
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
    public ChatResponse getAnswer(QuestionRequest request, String username) {
        String question = request.getQuestion();
        String conversationId = request.getConversationId();

        // 1. 判断是新对话还是旧对话
        if (conversationId == null || conversationId.trim().isEmpty()) {
            // 是新对话，生成新ID
            conversationId = UUID.randomUUID().toString();
        }

        // 2. 获取答案的逻辑（可以保持不变，也可以根据多轮对话上下文调整）
        // 这里暂时保持单轮的查找逻辑
        String answer;
        if (questionCache.containsKey(question)) {
            System.out.println("命中缓存: " + question);
            answer = questionCache.get(question);
        } else {
            Optional<Knowledge> knowledgeOpt = knowledgeRepository.findByQuestion(question);
            if (knowledgeOpt.isPresent()) {
                System.out.println("命中知识库: " + question);
                answer = knowledgeOpt.get().getAnswer();
                questionCache.put(question, answer);
            } else {
                System.out.println("调用大模型: " + question);
                // 多轮对话时，这里应该把历史消息传给大模型，此处为简化版
                answer = llmService.getCompletion(question);
                questionCache.put(question, answer);
            }
        }

        // 3. 保存历史记录
        saveHistory(conversationId, question, answer, username);

        // 4. 返回答案和本次对话的ID
        return new ChatResponse(answer, conversationId);
    }
}
