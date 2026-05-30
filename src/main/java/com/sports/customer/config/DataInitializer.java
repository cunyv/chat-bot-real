package com.sports.customer.config;

import com.sports.customer.entity.KnowledgeBase;
import com.sports.customer.repository.KnowledgeBaseRepository;
import com.sports.customer.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据初始化器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final RagService ragService;

    @Override
    public void run(String... args) {
        // 检查是否需要初始化向量
        List<KnowledgeBase> allKnowledge = knowledgeBaseRepository.findByStatus("ACTIVE");
        boolean needInit = allKnowledge.stream().anyMatch(k -> k.getEmbedding() == null);

        if (needInit) {
            log.info("检测到未处理的知识库数据，开始初始化向量...");
            try {
                ragService.processAllKnowledge();
                log.info("知识库向量初始化完成");
            } catch (Exception e) {
                log.warn("知识库向量初始化失败，请手动调用 /api/knowledge/process 接口", e);
            }
        }
    }
}
