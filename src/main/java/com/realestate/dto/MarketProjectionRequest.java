package com.realestate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketProjectionRequest {

    @NotNull
    private Long areaId;

    private String range;

    @NotNull
    private BigDecimal initialAmount;

    @Builder.Default
    private BigDecimal monthlyContribution = BigDecimal.ZERO;

    @NotNull
    private Integer years;

    /** Optional override; when null, use area-derived CAGR. */
    private BigDecimal expectedRatePct;
}
