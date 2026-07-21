package com.realestate.repository;

import com.realestate.entity.SiteVisitComment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SiteVisitCommentRepository extends JpaRepository<SiteVisitComment, Long> {

    List<SiteVisitComment> findBySiteVisitIdOrderByCreatedAtAsc(Long siteVisitId);

    void deleteByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM SiteVisitComment c WHERE c.siteVisit.property.id = :propertyId")
    void deleteBySiteVisitPropertyId(@Param("propertyId") Long propertyId);
}
