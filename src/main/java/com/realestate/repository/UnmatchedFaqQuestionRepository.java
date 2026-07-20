package com.realestate.repository;

import com.realestate.entity.UnmatchedFaqQuestion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnmatchedFaqQuestionRepository extends JpaRepository<UnmatchedFaqQuestion, Long> {

    @EntityGraph(attributePaths = {"user", "promotedFaq"})
    List<UnmatchedFaqQuestion> findByStatusOrderByCreatedAtDesc(UnmatchedFaqQuestion.Status status);

    long countByStatus(UnmatchedFaqQuestion.Status status);
}
