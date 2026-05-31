package com.realestate.repository;

import com.realestate.entity.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    Optional<BlogPost> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Page<BlogPost> findByPublishedTrue(Pageable pageable);
    Optional<BlogPost> findBySlugAndPublishedTrue(String slug);
    Page<BlogPost> findByAuthorId(Long authorId, Pageable pageable);

    @Query("SELECT p FROM BlogPost p WHERE p.published = true " +
           "AND (:category IS NULL OR :category = '' OR p.category = :category) " +
           "AND (:tag IS NULL OR :tag = '' OR CONCAT(',', LOWER(p.tags), ',') LIKE CONCAT('%,', LOWER(:tag), ',%'))")
    Page<BlogPost> findPublishedFiltered(@Param("category") String category, @Param("tag") String tag, Pageable pageable);

    @Query("SELECT DISTINCT p.category FROM BlogPost p WHERE p.published = true AND p.category IS NOT NULL AND p.category <> '' ORDER BY p.category")
    List<String> findDistinctPublishedCategories();

    @Query("SELECT p.tags FROM BlogPost p WHERE p.published = true AND p.tags IS NOT NULL AND p.tags <> ''")
    List<String> findPublishedTagStrings();
}
