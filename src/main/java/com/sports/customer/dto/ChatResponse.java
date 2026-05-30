package com.sports.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 聊天响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 回复内容
     */
    private String message;

    /**
     * 检索到的参考文档
     */
    private List<RetrievedDoc> retrievedDocs;

    /**
     * 检索到的文档
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrievedDoc {
        private Long id;
        private String category;
        private String title;
        private String content;
        private Double similarity;
    }
}
