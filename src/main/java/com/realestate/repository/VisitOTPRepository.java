package com.realestate.repository;

import com.realestate.entity.VisitOTP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VisitOTPRepository extends JpaRepository<VisitOTP, Long> {

    Optional<VisitOTP> findBySiteVisitId(Long siteVisitId);
}
