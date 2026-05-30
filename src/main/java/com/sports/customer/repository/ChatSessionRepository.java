package com.sports.customer.repository;

import com.sports.customer.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 会话Repository
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    /**
     * 根据会话ID查询
     */
    Optional<ChatSession> findBySessionId(String sessionId);

    /**
     * 查询用户的所有会话
     */
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(String userId);

    /**
     * 查询用户的活跃会话
     */
    List<ChatSession> findByUserIdAndStatusOrderByUpdatedAtDesc(String userId, String status);
}
