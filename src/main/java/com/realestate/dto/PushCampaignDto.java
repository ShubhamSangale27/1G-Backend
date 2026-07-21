package com.realestate.dto;

import com.realestate.entity.PushCampaign;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class PushCampaignDto {
    Long id;
    String title;
    String body;
    String imageUrl;
    String linkUrl;
    String linkTarget;
    String targetRole;
    int sentCount;
    int failedCount;
    Instant createdAt;

    public static PushCampaignDto from(PushCampaign campaign) {
        return PushCampaignDto.builder()
                .id(campaign.getId())
                .title(campaign.getTitle())
                .body(campaign.getBody())
                .imageUrl(campaign.getImageUrl())
                .linkUrl(campaign.getLinkUrl())
                .linkTarget(campaign.getLinkTarget().name())
                .targetRole(campaign.getTargetRole().name())
                .sentCount(campaign.getSentCount())
                .failedCount(campaign.getFailedCount())
                .createdAt(campaign.getCreatedAt())
                .build();
    }
}
