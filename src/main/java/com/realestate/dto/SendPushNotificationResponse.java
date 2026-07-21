package com.realestate.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SendPushNotificationResponse {
    Long campaignId;
    int sentCount;
    int failedCount;
    String message;
}
