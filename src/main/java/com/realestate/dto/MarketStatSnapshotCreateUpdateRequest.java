package com.realestate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketStatSnapshotCreateUpdateRequest {

    @NotNull
    private Long marketAreaId;

    @NotNull
    private LocalDate snapshotDate;

    private String granularity;

    @NotNull
    private BigDecimal priceIndex;

    private BigDecimal avgPricePerSqft;
    private BigDecimal yoyGrowthPct;
    private Integer transactionVolume;
    private BigDecimal rentalYieldPct;
    private String sourceType;
    private String sourceLabel;
    private BigDecimal confidenceScore;
}
