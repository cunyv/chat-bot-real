package com.sports.customer.controller;

import com.sports.customer.dto.KnowledgeRequest;
import com.sports.customer.entity.KnowledgeBase;
import com.sports.customer.repository.KnowledgeBaseRepository;
import com.sports.customer.service.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 知识库控制器
 */
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class KnowledgeController {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final RagService ragService;

    /**
     * 添加知识
     */
    @PostMapping
    public ResponseEntity<KnowledgeBase> addKnowledge(@Valid @RequestBody KnowledgeRequest request) {
        KnowledgeBase knowledge = KnowledgeBase.builder()
                .category(request.getCategory())
                .title(request.getTitle())
                .content(request.getContent())
                .metadata(request.getMetadata())
                .build();

        KnowledgeBase saved = knowledgeBaseRepository.save(knowledge);

        // 异步处理向量
        try {
            ragService.processKnowledge(saved);
        } catch (Exception e) {
            // 向量处理失败不影响保存
            e.printStackTrace();
        }

        return ResponseEntity.ok(saved);
    }

    /**
     * 更新知识
     */
    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeBase> updateKnowledge(
            @PathVariable Long id,
            @Valid @RequestBody KnowledgeRequest request) {
        KnowledgeBase knowledge = knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("知识不存在"));

        knowledge.setCategory(request.getCategory());
        knowledge.setTitle(request.getTitle());
        knowledge.setContent(request.getContent());
        knowledge.setMetadata(request.getMetadata());

        KnowledgeBase saved = knowledgeBaseRepository.save(knowledge);

        // 重新处理向量
        try {
            ragService.processKnowledge(saved);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok(saved);
    }

    /**
     * 删除知识
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKnowledge(@PathVariable Long id) {
        knowledgeBaseRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取所有知识
     */
    @GetMapping
    public ResponseEntity<List<KnowledgeBase>> getAllKnowledge() {
        List<KnowledgeBase> list = knowledgeBaseRepository.findAll();
        return ResponseEntity.ok(list);
    }

    /**
     * 根据分类获取知识
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<KnowledgeBase>> getByCategory(@PathVariable String category) {
        List<KnowledgeBase> list = knowledgeBaseRepository.findByCategoryAndStatus(category, "ACTIVE");
        return ResponseEntity.ok(list);
    }

    /**
     * 搜索知识
     */
    @GetMapping("/search")
    public ResponseEntity<List<KnowledgeBase>> searchKnowledge(@RequestParam String keyword) {
        List<KnowledgeBase> list = knowledgeBaseRepository.searchByKeyword(keyword);
        return ResponseEntity.ok(list);
    }

    /**
     * 知识检索测试
     */
    @PostMapping("/search")
    public ResponseEntity<List<KnowledgeBase>> searchByQuery(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        List<KnowledgeBase> results = ragService.retrieveRelevantKnowledge(query);
        return ResponseEntity.ok(results);
    }

    /**
     * 批量处理知识库向量
     */
    @PostMapping("/process")
    public ResponseEntity<String> processAllKnowledge() {
        ragService.processAllKnowledge();
        return ResponseEntity.ok("知识库处理完成");
    }
}
