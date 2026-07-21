package com.realestate.dto;

import com.realestate.entity.MarketStatSnapshot;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketStatSnapshotDto {
    private Long id;
    private Long marketAreaId;
    private LocalDate snapshotDate;
    private String granularity;
    private BigDecimal priceIndex;
    private BigDecimal avgPricePerSqft;
    private BigDecimal yoyGrowthPct;
    private Integer transactionVolume;
    private BigDecimal rentalYieldPct;
    private String sourceType;
    private String sourceLabel;
    private BigDecimal confidenceScore;

    public static MarketStatSnapshotDto from(MarketStatSnapshot s) {
        return MarketStatSnapshotDto.builder()
                .id(s.getId())
                .marketAreaId(s.getMarketArea().getId())
                .snapshotDate(s.getSnapshotDate())
                .granularity(s.getGranularity().name())
                .priceIndex(s.getPriceIndex())
                .avgPricePerSqft(s.getAvgPricePerSqft())
                .yoyGrowthPct(s.getYoyGrowthPct())
                .transactionVolume(s.getTransactionVolume())
                .rentalYieldPct(s.getRentalYieldPct())
                .sourceType(s.getSourceType().name())
                .sourceLabel(s.getSourceLabel())
                .confidenceScore(s.getConfidenceScore())
                .build();
    }
}
