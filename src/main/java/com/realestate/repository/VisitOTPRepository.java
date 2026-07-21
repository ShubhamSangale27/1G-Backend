package com.realestate.repository;

import com.realestate.entity.VisitOTP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VisitOTPRepository extends JpaRepository<VisitOTP, Long> {

    Optional<VisitOTP> findBySiteVisitId(Long siteVisitId);

    @Modifying
    @Query("DELETE FROM VisitOTP o WHERE o.siteVisit.property.id = :propertyId")
    void deleteBySiteVisitPropertyId(@Param("propertyId") Long propertyId);
}
