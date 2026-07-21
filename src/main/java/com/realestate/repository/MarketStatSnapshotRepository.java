package com.realestate.repository;

import com.realestate.entity.MarketStatSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MarketStatSnapshotRepository extends JpaRepository<MarketStatSnapshot, Long> {

    List<MarketStatSnapshot> findByMarketAreaIdOrderBySnapshotDateAsc(Long marketAreaId);

    @Query("""
            SELECT s FROM MarketStatSnapshot s
            WHERE s.marketArea.id = :areaId
              AND (:fromDate IS NULL OR s.snapshotDate >= :fromDate)
            ORDER BY s.snapshotDate ASC
            """)
    List<MarketStatSnapshot> findByAreaAndFromDate(
            @Param("areaId") Long areaId,
            @Param("fromDate") LocalDate fromDate);

    Optional<MarketStatSnapshot> findFirstByMarketAreaIdOrderBySnapshotDateDesc(Long marketAreaId);

    Optional<MarketStatSnapshot> findByMarketAreaIdAndSnapshotDateAndGranularity(
            Long marketAreaId,
            LocalDate snapshotDate,
            MarketStatSnapshot.Granularity granularity);

    List<MarketStatSnapshot> findByMarketAreaIdOrderBySnapshotDateDesc(Long marketAreaId);
}
