package com.sports.customer.repository;

import com.sports.customer.entity.Conversation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 对话历史Repository
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * 查询会话的对话历史
     */
    List<Conversation> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /**
     * 查询会话最近的对话
     */
    List<Conversation> findBySessionIdOrderByCreatedAtDesc(String sessionId, Pageable pageable);

    /**
     * 查询用户的对话历史
     */
    List<Conversation> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * 统计会话的对话数量
     */
    long countBySessionId(String sessionId);
}
