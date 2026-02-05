package com.realestate.dto;

import com.realestate.entity.PremiumPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumPlanDto {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationDays;
    private String stripePriceId;

    public static PremiumPlanDto from(PremiumPlan p) {
        if (p == null) return null;
        return PremiumPlanDto.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .durationDays(p.getDurationDays())
                .stripePriceId(p.getStripePriceId())
                .build();
    }
}
