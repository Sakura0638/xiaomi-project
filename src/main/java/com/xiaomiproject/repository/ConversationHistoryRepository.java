package com.xiaomiproject.repository;

import com.xiaomiproject.entity.ConversationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; // 导入 List

public interface ConversationHistoryRepository extends JpaRepository<ConversationHistory, Long> {

    List<ConversationHistory> findByUserIdOrderByTimestampDesc(Long userId);
    List<ConversationHistory> findByConversationIdOrderByTimestampAsc(String conversationId);
}
