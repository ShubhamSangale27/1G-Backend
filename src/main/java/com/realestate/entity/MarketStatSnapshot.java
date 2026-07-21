package com.realestate.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "market_stat_snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketStatSnapshot {

    public enum Granularity {
        MONTHLY, QUARTERLY, YEARLY
    }

    public enum SourceType {
        ADMIN, RBI_SEED, MANUAL_IMPORT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "market_area_id", nullable = false)
    private MarketArea marketArea;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Granularity granularity = Granularity.QUARTERLY;

    @Column(name = "price_index", nullable = false, precision = 14, scale = 4)
    private BigDecimal priceIndex;

    @Column(name = "avg_price_per_sqft", precision = 14, scale = 2)
    private BigDecimal avgPricePerSqft;

    @Column(name = "yoy_growth_pct", precision = 8, scale = 4)
    private BigDecimal yoyGrowthPct;

    @Column(name = "transaction_volume")
    private Integer transactionVolume;

    @Column(name = "rental_yield_pct", precision = 8, scale = 4)
    private BigDecimal rentalYieldPct;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    @Builder.Default
    private SourceType sourceType = SourceType.ADMIN;

    @Column(name = "source_label", length = 120)
    private String sourceLabel;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
