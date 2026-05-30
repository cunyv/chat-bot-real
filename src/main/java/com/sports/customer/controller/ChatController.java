package com.sports.customer.controller;

import com.sports.customer.dto.ChatRequest;
import com.sports.customer.dto.ChatResponse;
import com.sports.customer.entity.ChatSession;
import com.sports.customer.entity.Conversation;
import com.sports.customer.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 对话控制器
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    /**
     * 发送消息
     */
    @PostMapping("/send")
    public ResponseEntity<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatService.sendMessage(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取用户的会话列表
     */
    @GetMapping("/sessions/{userId}")
    public ResponseEntity<List<ChatSession>> getUserSessions(@PathVariable String userId) {
        List<ChatSession> sessions = chatService.getUserSessions(userId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * 获取会话的对话历史
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<Conversation>> getSessionHistory(@PathVariable String sessionId) {
        List<Conversation> history = chatService.getSessionHistory(sessionId);
        return ResponseEntity.ok(history);
    }

    /**
     * 关闭会话
     */
    @PostMapping("/session/{sessionId}/close")
    public ResponseEntity<Void> closeSession(@PathVariable String sessionId) {
        chatService.closeSession(sessionId);
        return ResponseEntity.ok().build();
    }
}
