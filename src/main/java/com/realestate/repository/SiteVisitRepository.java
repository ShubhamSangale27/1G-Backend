package com.realestate.repository;

import com.realestate.entity.SiteVisit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SiteVisitRepository extends JpaRepository<SiteVisit, Long> {

    Page<SiteVisit> findByUserId(Long userId, Pageable pageable);

    Optional<SiteVisit> findFirstByUserIdAndPropertyIdAndStatusInOrderByCreatedAtDesc(Long userId, Long propertyId, List<SiteVisit.SiteVisitStatus> statuses);

    Page<SiteVisit> findByAgentId(Long agentId, Pageable pageable);

    List<SiteVisit> findByStatus(SiteVisit.SiteVisitStatus status, Pageable pageable);

    long countByStatus(SiteVisit.SiteVisitStatus status);

    @Query("SELECT COUNT(sv) FROM SiteVisit sv WHERE sv.scheduledAt >= :start AND sv.scheduledAt < :end AND sv.status IN ('PENDING_ASSIGNMENT', 'ASSIGNED')")
    long countDueBetween(@Param("start") Instant start, @Param("end") Instant end);

    /* COALESCE / sentinel avoids "could not determine data type of parameter" in PostgreSQL (no param in IS NULL). Pass agentId = -1 for "all agents". */
    @Query("SELECT sv FROM SiteVisit sv WHERE (sv.scheduledAt >= COALESCE(:from, sv.scheduledAt)) AND (sv.scheduledAt <= COALESCE(:to, sv.scheduledAt)) AND (sv.agent.id = :agentId OR :agentId = -1) ORDER BY CASE WHEN (sv.scheduledAt >= :startToday AND sv.scheduledAt < :endToday) THEN 0 ELSE 1 END, sv.scheduledAt ASC")
    Page<SiteVisit> findAllForAdmin(@Param("from") Instant from, @Param("to") Instant to, @Param("agentId") Long agentId,
                                    @Param("startToday") Instant startToday, @Param("endToday") Instant endToday,
                                    Pageable pageable);

    @Query("SELECT COUNT(sv) FROM SiteVisit sv WHERE sv.agent.id = :agentId AND sv.scheduledAt >= :start AND sv.scheduledAt < :end AND sv.status = 'ASSIGNED'")
    long countDueTodayForAgent(@Param("agentId") Long agentId, @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT sv FROM SiteVisit sv WHERE sv.agent.id = :agentId AND sv.status IN ('ASSIGNED', 'COMPLETED') " +
            "ORDER BY CASE WHEN sv.status = 'COMPLETED' THEN 2 WHEN sv.scheduledAt >= :now THEN 0 ELSE 1 END, " +
            "CASE WHEN sv.status = 'COMPLETED' THEN sv.scheduledAt END DESC, sv.scheduledAt ASC")
    Page<SiteVisit> findByAgentIdUpcomingFirst(@Param("agentId") Long agentId, @Param("now") Instant now, Pageable pageable);

    void deleteByUserId(Long userId);

    void deleteByPropertyId(Long propertyId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE SiteVisit sv SET sv.agent = NULL WHERE sv.agent.id = :agentId")
    void clearAgentAssignments(@Param("agentId") Long agentId);
}
