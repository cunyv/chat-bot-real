package com.sports.customer.service;

import com.sports.customer.config.ChatConfig;
import com.sports.customer.dto.ChatRequest;
import com.sports.customer.dto.ChatResponse;
import com.sports.customer.entity.ChatSession;
import com.sports.customer.entity.Conversation;
import com.sports.customer.entity.KnowledgeBase;
import com.sports.customer.repository.ChatSessionRepository;
import com.sports.customer.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 对话服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ConversationRepository conversationRepository;
    private final RagService ragService;
    private final MimoService mimoService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatConfig chatConfig;

    private static final String SESSION_CACHE_KEY = "chat:session:";
    private static final String CONTEXT_CACHE_KEY = "chat:context:";

    /**
     * 发送消息并获取回复
     */
    @Transactional
    public ChatResponse sendMessage(ChatRequest request) {
        // 1. 获取或创建会话
        ChatSession session = getOrCreateSession(request.getSessionId(), request.getUserId());

        // 2. 保存用户消息
        Conversation userMessage = Conversation.builder()
                .sessionId(session.getSessionId())
                .userId(request.getUserId())
                .role(Conversation.Role.USER.name())
                .content(request.getMessage())
                .build();
        conversationRepository.save(userMessage);

        // 3. RAG检索相关知识
        List<KnowledgeBase> relevantKnowledge = ragService.retrieveRelevantKnowledge(request.getMessage());
        String ragContext = ragService.buildRagContext(relevantKnowledge);

        // 4. 构建对话上下文
        List<MimoService.ChatMessage> messages = buildChatContext(session.getSessionId(), request.getMessage(), ragContext);

        // 5. 调用LLM获取回复
        String assistantReply = mimoService.chatCompletion(messages);

        // 6. 保存助手回复
        List<Long> retrievedDocIds = relevantKnowledge.stream()
                .map(KnowledgeBase::getId)
                .collect(Collectors.toList());

        Conversation assistantMessage = Conversation.builder()
                .sessionId(session.getSessionId())
                .userId(request.getUserId())
                .role(Conversation.Role.ASSISTANT.name())
                .content(assistantReply)
                .retrievedDocs(retrievedDocIds)
                .build();
        conversationRepository.save(assistantMessage);

        // 7. 更新会话标题（如果是新会话）
        if (session.getTitle() == null || session.getTitle().startsWith("新会话")) {
            String title = request.getMessage().length() > 20
                    ? request.getMessage().substring(0, 20) + "..."
                    : request.getMessage();
            session.setTitle(title);
            chatSessionRepository.save(session);
        }

        // 8. 更新Redis缓存
        updateSessionCache(session);

        // 9. 构建响应
        List<ChatResponse.RetrievedDoc> retrievedDocs = relevantKnowledge.stream()
                .map(kb -> ChatResponse.RetrievedDoc.builder()
                        .id(kb.getId())
                        .category(kb.getCategory())
                        .title(kb.getTitle())
                        .content(kb.getContent().length() > 200
                                ? kb.getContent().substring(0, 200) + "..."
                                : kb.getContent())
                        .build())
                .collect(Collectors.toList());

        return ChatResponse.builder()
                .sessionId(session.getSessionId())
                .message(assistantReply)
                .retrievedDocs(retrievedDocs)
                .build();
    }

    /**
     * 获取或创建会话
     */
    private ChatSession getOrCreateSession(String sessionId, String userId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            return chatSessionRepository.findBySessionId(sessionId)
                    .orElseThrow(() -> new RuntimeException("会话不存在: " + sessionId));
        }
        // 创建新会话
        String newSessionId = UUID.randomUUID().toString().replace("-", "");
        ChatSession newSession = ChatSession.builder()
                .sessionId(newSessionId)
                .userId(userId)
                .title("新会话")
                .build();
        return chatSessionRepository.save(newSession);
    }

    /**
     * 构建对话上下文
     */
    private List<MimoService.ChatMessage> buildChatContext(String sessionId, String currentMessage, String ragContext) {
        List<MimoService.ChatMessage> messages = new ArrayList<>();

        // 系统提示词
        String systemPrompt = buildSystemPrompt(ragContext);
        messages.add(new MimoService.ChatMessage("system", systemPrompt));

        // 获取历史对话
        List<Conversation> history = conversationRepository.findBySessionIdOrderByCreatedAtDesc(
                sessionId, PageRequest.of(0, chatConfig.getMaxContextMessages()));

        // 反转为时间正序
        Collections.reverse(history);

        // 添加历史消息
        for (Conversation conv : history) {
            messages.add(new MimoService.ChatMessage(conv.getRole().toLowerCase(), conv.getContent()));
        }

        // 添加当前消息
        messages.add(new MimoService.ChatMessage("user", currentMessage));

        return messages;
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(String ragContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是运动品类撮合交易平台的智能客服助手。\n");
        prompt.append("你的职责是帮助用户了解教练和场地信息，协助用户做出预约决策。\n\n");
        prompt.append("回答要求：\n");
        prompt.append("1. 友好、专业、耐心\n");
        prompt.append("2. 基于知识库信息回答，不要编造信息\n");
        prompt.append("3. 如果知识库没有相关信息，建议用户联系人工客服\n");
        prompt.append("4. 适当使用emoji让对话更生动\n\n");

        if (ragContext != null && !ragContext.isEmpty()) {
            prompt.append(ragContext);
        }

        return prompt.toString();
    }

    /**
     * 更新会话缓存
     */
    private void updateSessionCache(ChatSession session) {
        String cacheKey = SESSION_CACHE_KEY + session.getSessionId();
        redisTemplate.opsForValue().set(cacheKey, session, chatConfig.getSessionTimeout(), TimeUnit.SECONDS);
    }

    /**
     * 获取用户的会话列表
     */
    public List<ChatSession> getUserSessions(String userId) {
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    /**
     * 获取会话的对话历史
     */
    public List<Conversation> getSessionHistory(String sessionId) {
        return conversationRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    /**
     * 关闭会话
     */
    @Transactional
    public void closeSession(String sessionId) {
        ChatSession session = chatSessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在: " + sessionId));
        session.setStatus("CLOSED");
        chatSessionRepository.save(session);

        // 清除缓存
        redisTemplate.delete(SESSION_CACHE_KEY + sessionId);
        redisTemplate.delete(CONTEXT_CACHE_KEY + sessionId);
    }
}
