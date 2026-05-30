package com.sports.customer.service;

import com.sports.customer.config.RagConfig;
import com.sports.customer.entity.KnowledgeBase;
import com.sports.customer.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG检索服务 - 基于关键词匹配和TF-IDF
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final RagConfig ragConfig;

    /**
     * 检索相关知识
     */
    public List<KnowledgeBase> retrieveRelevantKnowledge(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 提取关键词
        List<String> keywords = extractKeywords(query);
        if (keywords.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 搜索匹配的知识
        List<KnowledgeBase> allKnowledge = knowledgeBaseRepository.findByStatus("ACTIVE");

        // 3. 计算相关性分数
        List<ScoredKnowledge> scoredList = new ArrayList<>();
        for (KnowledgeBase knowledge : allKnowledge) {
            double score = calculateRelevanceScore(knowledge, keywords, query);
            if (score > 0) {
                scoredList.add(new ScoredKnowledge(knowledge, score));
            }
        }

        // 4. 按分数降序排序，取topK
        return scoredList.stream()
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(ragConfig.getTopK())
                .map(sk -> sk.knowledge)
                .collect(Collectors.toList());
    }

    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String query) {
        List<String> keywords = new ArrayList<>();

        // 移除标点符号，保留中文和字母数字
        String cleaned = query.replaceAll("[^\\w\\u4e00-\\u9fa5]+", " ");

        // 简单分词（按空格分隔）
        String[] words = cleaned.split("\\s+");

        // 过滤停用词和短词
        Set<String> stopWords = Set.of("的", "了", "在", "是", "我", "有", "和", "就",
                "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去",
                "你", "会", "着", "没有", "看", "好", "自己", "这", "他", "她", "它",
                "吗", "什么", "怎么", "哪", "那", "些", "想", "能", "可以", "请",
                "帮", "问", "下", "吧", "呢", "啊", "哦", "嗯");

        for (String word : words) {
            word = word.trim().toLowerCase();
            if (word.length() >= 2 && !stopWords.contains(word)) {
                keywords.add(word);
            }
        }

        // 添加领域关键词映射
        Map<String, List<String>> domainMapping = Map.of(
                "教练", List.of("教练", "老师", "指导", "培训", "教学"),
                "场地", List.of("场地", "场馆", "球场", "球馆", "游泳馆", "健身房"),
                "篮球", List.of("篮球", "nba", "投篮", "运球"),
                "网球", List.of("网球", "atp", "wta", "发球"),
                "游泳", List.of("游泳", "泳池", "泳姿", "蛙泳", "自由泳"),
                "瑜伽", List.of("瑜伽", "冥想", "拉伸", "柔韧"),
                "足球", List.of("足球", "球场", "射门", "传球")
        );

        // 扩展关键词
        Set<String> expandedKeywords = new HashSet<>(keywords);
        for (String keyword : keywords) {
            for (Map.Entry<String, List<String>> entry : domainMapping.entrySet()) {
                if (entry.getValue().contains(keyword) || keyword.contains(entry.getKey())) {
                    expandedKeywords.addAll(entry.getValue());
                    expandedKeywords.add(entry.getKey());
                }
            }
        }

        return new ArrayList<>(expandedKeywords);
    }

    /**
     * 计算相关性分数
     */
    private double calculateRelevanceScore(KnowledgeBase knowledge, List<String> keywords, String originalQuery) {
        double score = 0;
        String titleLower = knowledge.getTitle().toLowerCase();
        String contentLower = knowledge.getContent().toLowerCase();
        String categoryLower = knowledge.getCategory().toLowerCase();

        // 标题匹配（权重最高）
        for (String keyword : keywords) {
            if (titleLower.contains(keyword)) {
                score += 3.0;
            }
        }

        // 内容匹配
        for (String keyword : keywords) {
            int count = countOccurrences(contentLower, keyword);
            score += Math.min(count * 0.5, 2.0); // 每个关键词最多2分
        }

        // 分类匹配
        if (originalQuery.contains("教练") && categoryLower.equals("coach")) {
            score += 2.0;
        }
        if (originalQuery.contains("场地") && categoryLower.equals("venue")) {
            score += 2.0;
        }

        // 元数据匹配
        if (knowledge.getMetadata() != null) {
            String metadataStr = knowledge.getMetadata().toString().toLowerCase();
            for (String keyword : keywords) {
                if (metadataStr.contains(keyword)) {
                    score += 1.0;
                }
            }
        }

        return score;
    }

    /**
     * 统计子串出现次数
     */
    private int countOccurrences(String text, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    /**
     * 构建RAG上下文
     */
    public String buildRagContext(List<KnowledgeBase> knowledgeList) {
        if (knowledgeList.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("以下是相关的知识库信息，请基于这些信息回答用户问题：\n\n");

        for (int i = 0; i < knowledgeList.size(); i++) {
            KnowledgeBase kb = knowledgeList.get(i);
            context.append(String.format("【%s】%s\n", getCategoryName(kb.getCategory()), kb.getTitle()));
            context.append(kb.getContent());
            if (kb.getMetadata() != null) {
                context.append("\n详细信息：");
                kb.getMetadata().forEach((key, value) ->
                        context.append(String.format(" %s: %s;", key, value)));
            }
            context.append("\n\n");
        }

        return context.toString();
    }

    /**
     * 获取分类中文名
     */
    private String getCategoryName(String category) {
        return switch (category.toUpperCase()) {
            case "COACH" -> "教练";
            case "VENUE" -> "场地";
            default -> category;
        };
    }

    /**
     * 批量处理知识库（预留接口，当前无需处理）
     */
    public void processAllKnowledge() {
        log.info("当前使用关键词检索模式，无需处理向量");
    }

    /**
     * 处理单个知识（预留接口）
     */
    public void processKnowledge(KnowledgeBase knowledge) {
        // 关键词模式无需处理
    }

    /**
     * 带分数的知识
     */
    private static class ScoredKnowledge {
        final KnowledgeBase knowledge;
        final double score;

        ScoredKnowledge(KnowledgeBase knowledge, double score) {
            this.knowledge = knowledge;
            this.score = score;
        }
    }
}
