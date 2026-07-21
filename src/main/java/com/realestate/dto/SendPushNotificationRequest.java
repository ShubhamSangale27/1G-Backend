package com.realestate.dto;

import com.realestate.entity.PushCampaign;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendPushNotificationRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 2000)
    private String body;

    @Size(max = 2048)
    private String imageUrl;

    @Size(max = 2048)
    private String linkUrl;

    @Pattern(regexp = "APP|EXTERNAL")
    private String linkTarget = "APP";

    @Pattern(regexp = "ALL|USER|AGENT")
    private String targetRole = "ALL";

    public PushCampaign.LinkTarget linkTargetEnum() {
        return PushCampaign.LinkTarget.valueOf(linkTarget == null ? "APP" : linkTarget.toUpperCase());
    }

    public PushCampaign.TargetRole targetRoleEnum() {
        return PushCampaign.TargetRole.valueOf(targetRole == null ? "ALL" : targetRole.toUpperCase());
    }
}
