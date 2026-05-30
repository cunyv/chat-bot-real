package com.sports.customer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 小米mimo API配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mimo")
public class MimoConfig {

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * API基础URL
     */
    private String baseUrl;

    /**
     * 聊天模型
     */
    private String chatModel;

    /**
     * 嵌入模型
     */
    private String embeddingModel;

    /**
     * 最大Token数
     */
    private Integer maxTokens = 2048;

    /**
     * 温度参数
     */
    private Double temperature = 0.7;
}
