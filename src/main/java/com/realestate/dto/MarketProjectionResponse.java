package com.realestate.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketProjectionResponse {

    private MarketStatsResponse market;
    private BigDecimal regionalRatePct;
    private BigDecimal userRatePct;
    private BigDecimal regionalFinal;
    private BigDecimal userFinal;

    @Builder.Default
    private List<ProjectionPoint> points = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjectionPoint {
        private int year;
        private BigDecimal regional;
        private BigDecimal user;
        private boolean forecast;
    }
}
