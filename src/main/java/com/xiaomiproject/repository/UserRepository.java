package com.xiaomiproject.repository;

import com.xiaomiproject.entity.ConversationHistory;
import com.xiaomiproject.entity.Knowledge;
import com.xiaomiproject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 使用Optional避免空指针 (Java 8特性)
    Optional<User> findByUsername(String username);
}
