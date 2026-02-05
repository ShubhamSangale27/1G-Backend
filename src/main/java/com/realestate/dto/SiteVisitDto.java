package com.realestate.dto;

import com.realestate.entity.SiteVisit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteVisitDto {

    private Long id;
    private Long userId;
    private String userName;
    private Long propertyId;
    private String propertyTitle;
    private Long agentId;
    private String agentName;
    private Instant scheduledAt;
    private String userNotes;
    private SiteVisit.SiteVisitStatus status;
    private Instant createdAt;

    public static SiteVisitDto from(SiteVisit sv) {
        if (sv == null) return null;
        return SiteVisitDto.builder()
                .id(sv.getId())
                .userId(sv.getUser() != null ? sv.getUser().getId() : null)
                .userName(sv.getUser() != null ? sv.getUser().getFullName() : null)
                .propertyId(sv.getProperty() != null ? sv.getProperty().getId() : null)
                .propertyTitle(sv.getProperty() != null ? sv.getProperty().getTitle() : null)
                .agentId(sv.getAgent() != null ? sv.getAgent().getId() : null)
                .agentName(sv.getAgent() != null ? sv.getAgent().getFullName() : null)
                .scheduledAt(sv.getScheduledAt())
                .userNotes(sv.getUserNotes())
                .status(sv.getStatus())
                .createdAt(sv.getCreatedAt())
                .build();
    }
}
