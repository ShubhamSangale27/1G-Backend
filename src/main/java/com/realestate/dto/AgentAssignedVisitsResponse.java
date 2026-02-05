package com.realestate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentAssignedVisitsResponse {

    private List<SiteVisitDto> content;
    private long totalElements;
    private int totalPages;
    private long dueTodayCount;
}
