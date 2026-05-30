package com.sports.customer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 对话配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "chat")
public class ChatConfig {

    /**
     * 最大上下文消息数
     */
    private Integer maxContextMessages = 10;

    /**
     * 会话超时时间（秒）
     */
    private Integer sessionTimeout = 3600;

    /**
     * 缓存TTL（秒）
     */
    private Integer cacheTtl = 1800;
}
