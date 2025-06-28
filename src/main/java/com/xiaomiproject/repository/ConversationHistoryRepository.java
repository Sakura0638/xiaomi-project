package com.xiaomiproject.repository;

import com.xiaomiproject.entity.ConversationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationHistoryRepository extends JpaRepository<ConversationHistory, Long> {
    // 暂时不需要特殊查询方法
}
