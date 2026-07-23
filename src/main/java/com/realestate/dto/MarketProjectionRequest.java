package com.realestate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketProjectionRequest {

    /** Legacy: direct area id (locality). Prefer state+city+localityId. */
    private Long areaId;

    @NotBlank
    private String state;

    @NotBlank
    private String city;

    /** Optional admin-configured locality. When null, city/state benchmark applies. */
    private Long localityId;

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
