package com.realestate.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketStatsResponse {

    private MarketAreaDto area;
    private String range;
    private boolean dataAvailable;
    private String message;
    private BigDecimal latestIndex;
    private BigDecimal latestAvgPricePerSqft;
    private BigDecimal latestRentalYieldPct;
    private BigDecimal rangeReturnPct;
    private BigDecimal derivedCagrPct;
    private LocalDate coverageFrom;
    private LocalDate coverageTo;

    @Builder.Default
    private List<HistoryPoint> history = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HistoryPoint {
        private LocalDate date;
        private BigDecimal index;
        private BigDecimal avgPricePerSqft;
        private BigDecimal yoyGrowthPct;
    }
}
