package com.realestate.repository;

import com.realestate.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByIdentifierAndChannelOrderByCreatedAtDesc(String identifier, OtpVerification.OtpChannel channel);

    long countByIdentifierAndChannelAndCreatedAtAfter(String identifier, OtpVerification.OtpChannel channel, Instant createdAt);

    Optional<OtpVerification> findFirstByIdentifierAndChannelAndCreatedAtAfterOrderByCreatedAtAsc(
            String identifier,
            OtpVerification.OtpChannel channel,
            Instant createdAt
    );
}
