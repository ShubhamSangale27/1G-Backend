package com.realestate.repository;

import com.realestate.entity.MarketArea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MarketAreaRepository extends JpaRepository<MarketArea, Long> {

    List<MarketArea> findByActiveTrueAndLevelOrderBySortOrderAscNameAsc(MarketArea.Level level);

    List<MarketArea> findByActiveTrueAndParentIdOrderBySortOrderAscNameAsc(Long parentId);

    List<MarketArea> findByActiveTrueAndLevelAndStateNameIgnoreCaseOrderBySortOrderAscNameAsc(
            MarketArea.Level level, String stateName);

    List<MarketArea> findByActiveTrueAndLevelAndStateNameIgnoreCaseAndCityNameIgnoreCaseOrderBySortOrderAscNameAsc(
            MarketArea.Level level, String stateName, String cityName);

    Optional<MarketArea> findByLevelAndNameIgnoreCaseAndParentIsNull(MarketArea.Level level, String name);

    Optional<MarketArea> findByLevelAndNameIgnoreCaseAndParentId(MarketArea.Level level, String name, Long parentId);

    @Query("""
            SELECT a FROM MarketArea a
            WHERE a.active = true
              AND (:parentId IS NULL OR a.parent.id = :parentId)
              AND (:state IS NULL OR LOWER(a.stateName) = LOWER(:state))
              AND (:city IS NULL OR LOWER(a.cityName) = LOWER(:city))
              AND (:level IS NULL OR a.level = :level)
            ORDER BY a.sortOrder ASC, a.name ASC
            """)
    List<MarketArea> findFiltered(
            @Param("parentId") Long parentId,
            @Param("state") String state,
            @Param("city") String city,
            @Param("level") MarketArea.Level level);

    List<MarketArea> findAllByOrderByLevelAscSortOrderAscNameAsc();

    Optional<MarketArea> findFirstByActiveTrueAndLevelAndStateNameIgnoreCaseAndNameIgnoreCase(
            MarketArea.Level level, String stateName, String name);

    List<MarketArea> findByActiveTrueAndLevelAndStateNameIgnoreCaseAndCityNameIgnoreCase(
            MarketArea.Level level, String stateName, String cityName);
}
