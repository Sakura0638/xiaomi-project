package com.xiaomiproject.repository;

import com.xiaomiproject.entity.ConversationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.transaction.Transactional;
import java.util.List;

public interface ConversationHistoryRepository extends JpaRepository<ConversationHistory, Long> {

    List<ConversationHistory> findByUserIdOrderByTimestampDesc(Long userId);
    List<ConversationHistory> findByConversationIdOrderByTimestampAsc(String conversationId);

    // --- 新增方法 ---
    /**
     * 根据对话ID删除所有相关历史记录。
     * @Transactional 注解是必须的，因为它是一个修改数据库的操作。
     * @param conversationId 要删除的对话ID
     * @return 被删除的记录数量
     */
    @Transactional
    Long deleteByConversationId(String conversationId);
}
