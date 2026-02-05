package com.realestate.dto;

import com.realestate.entity.SiteVisit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteVisitDetailDto {

    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String userMobile;
    private Long propertyId;
    private PropertySummaryDto property;
    private Long agentId;
    private String agentName;
    private Instant scheduledAt;
    private String userNotes;
    private SiteVisit.SiteVisitStatus status;
    private Instant createdAt;
    private List<SiteVisitCommentDto> comments;

    /** Minimal property info for agent view */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertySummaryDto {
        private Long id;
        private String title;
        private String address;
        private String city;
        private String state;
        private String pincode;
        private String listingType;
        private String propertyType;
        private java.math.BigDecimal price;
        private Integer bedrooms;
        private Integer bathrooms;
        private java.math.BigDecimal areaSqft;
        private String amenities;
        private String firstImageUrl;
    }
}
