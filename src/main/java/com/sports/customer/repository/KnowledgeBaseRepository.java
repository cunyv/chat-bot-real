package com.sports.customer.repository;

import com.sports.customer.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识库Repository
 */
@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    /**
     * 根据分类查询活跃的知识
     */
    List<KnowledgeBase> findByCategoryAndStatus(String category, String status);

    /**
     * 根据状态查询所有知识
     */
    List<KnowledgeBase> findByStatus(String status);

    /**
     * 搜索知识（标题或内容包含关键词）
     */
    @Query("SELECT k FROM KnowledgeBase k WHERE k.status = 'ACTIVE' AND (k.title LIKE %:keyword% OR k.content LIKE %:keyword%)")
    List<KnowledgeBase> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 根据分类搜索知识
     */
    @Query("SELECT k FROM KnowledgeBase k WHERE k.category = :category AND k.status = 'ACTIVE' AND (k.title LIKE %:keyword% OR k.content LIKE %:keyword%)")
    List<KnowledgeBase> searchByCategoryAndKeyword(@Param("category") String category, @Param("keyword") String keyword);
}
