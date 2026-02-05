package com.realestate.repository;

import com.realestate.entity.PendingSignup;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PendingSignupRepository extends JpaRepository<PendingSignup, Long> {

    Optional<PendingSignup> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByMobile(String mobile);

    @Modifying
    @Query("DELETE FROM PendingSignup p WHERE p.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
