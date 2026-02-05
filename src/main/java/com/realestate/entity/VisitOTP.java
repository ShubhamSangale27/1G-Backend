package com.realestate.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "visit_otps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitOTP {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_visit_id", nullable = false, unique = true)
    private SiteVisit siteVisit;

    @Column(nullable = false)
    private String otpCode;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @CreationTimestamp
    private Instant createdAt;
}
