package com.realestate.repository;

import com.realestate.entity.PropertyAnalytics;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PropertyAnalyticsRepository extends JpaRepository<PropertyAnalytics, Long> {

    long countByPropertyIdAndType(Long propertyId, PropertyAnalytics.AnalyticsType type);

    long countByType(PropertyAnalytics.AnalyticsType type);

    @Query("SELECT pa.type, COUNT(pa) FROM PropertyAnalytics pa WHERE pa.property.id = :propertyId GROUP BY pa.type")
    List<Object[]> countByPropertyIdGroupByType(@Param("propertyId") Long propertyId);

    @Query("SELECT pa.property.id, pa.type, COUNT(pa) FROM PropertyAnalytics pa " +
           "WHERE pa.recordedAt >= :from AND pa.recordedAt <= :to GROUP BY pa.property.id, pa.type")
    List<Object[]> aggregateByPropertyAndType(@Param("from") Instant from, @Param("to") Instant to);

    List<PropertyAnalytics> findByPropertyIdAndTypeOrderByRecordedAtDesc(Long propertyId, PropertyAnalytics.AnalyticsType type, Pageable pageable);

    void deleteByPropertyId(Long propertyId);
}
