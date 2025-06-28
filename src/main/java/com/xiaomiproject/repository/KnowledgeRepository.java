package com.xiaomiproject.repository;

import com.xiaomiproject.entity.Knowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface KnowledgeRepository extends JpaRepository<Knowledge, Long> {
    // 根据问题精确查找
    Optional<Knowledge> findByQuestion(String question);
}


