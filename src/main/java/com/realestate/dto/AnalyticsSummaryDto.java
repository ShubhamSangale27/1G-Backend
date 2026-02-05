package com.realestate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryDto {

    private Long propertyId;
    private Long viewCount;
    private Long visitCount;
    private Long clickCount;
    private Map<String, Long> byType;
}
