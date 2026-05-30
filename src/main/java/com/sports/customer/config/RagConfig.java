package com.sports.customer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAG配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagConfig {

    /**
     * 文本分块大小
     */
    private Integer chunkSize = 500;

    /**
     * 分块重叠大小
     */
    private Integer chunkOverlap = 50;

    /**
     * 检索返回的文档数量
     */
    private Integer topK = 3;

    /**
     * 相似度阈值
     */
    private Double similarityThreshold = 0.7;
}
