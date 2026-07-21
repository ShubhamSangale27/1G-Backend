package com.realestate.service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.realestate.config.FirebaseInitializer;
import com.realestate.dto.PageResponse;
import com.realestate.dto.PushCampaignDto;
import com.realestate.dto.SendPushNotificationRequest;
import com.realestate.dto.SendPushNotificationResponse;
import com.realestate.entity.DeviceToken;
import com.realestate.entity.PushCampaign;
import com.realestate.entity.User;
import com.realestate.repository.PushCampaignRepository;
import com.realestate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final PushCampaignRepository pushCampaignRepository;
    private final DeviceTokenService deviceTokenService;
    private final UserRepository userRepository;
    private final FirebaseInitializer firebaseInitializer;

    @Transactional
    public SendPushNotificationResponse sendCampaign(Long adminUserId, SendPushNotificationRequest request) {
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        PushCampaign campaign = pushCampaignRepository.save(PushCampaign.builder()
                .title(request.getTitle().trim())
                .body(request.getBody().trim())
                .imageUrl(blankToNull(request.getImageUrl()))
                .linkUrl(blankToNull(request.getLinkUrl()))
                .linkTarget(request.linkTargetEnum())
                .targetRole(request.targetRoleEnum())
                .sentBy(admin)
                .build());

        User.Role roleFilter = switch (campaign.getTargetRole()) {
            case USER -> User.Role.USER;
            case AGENT -> User.Role.AGENT;
            case ALL -> null;
        };

        List<DeviceToken> tokens = deviceTokenService.findActiveTokensForTarget(roleFilter);
        int sent = 0;
        int failed = 0;

        if (!firebaseInitializer.isReady()) {
            log.warn("Push campaign {} saved but FCM is not configured — {} tokens would have been targeted",
                    campaign.getId(), tokens.size());
            campaign.setSentCount(0);
            campaign.setFailedCount(tokens.size());
            pushCampaignRepository.save(campaign);
            return SendPushNotificationResponse.builder()
                    .campaignId(campaign.getId())
                    .sentCount(0)
                    .failedCount(tokens.size())
                    .message("Campaign saved. Configure Firebase (FIREBASE_ENABLED=true + FIREBASE_CREDENTIALS_PATH) to deliver push notifications.")
                    .build();
        }

        for (DeviceToken deviceToken : tokens) {
            try {
                sendToToken(campaign, deviceToken.getFcmToken());
                sent++;
            } catch (FirebaseMessagingException e) {
                failed++;
                log.warn("FCM send failed for token {}: {}", deviceToken.getId(), e.getMessage());
                if (isInvalidToken(e)) {
                    deviceTokenService.deactivateByFcmToken(deviceToken.getFcmToken());
                }
            }
        }

        campaign.setSentCount(sent);
        campaign.setFailedCount(failed);
        pushCampaignRepository.save(campaign);

        return SendPushNotificationResponse.builder()
                .campaignId(campaign.getId())
                .sentCount(sent)
                .failedCount(failed)
                .message(sent > 0
                        ? "Push notification sent to " + sent + " device(s)."
                        : "No devices received the notification. Ensure users have logged in on mobile and granted notification permission.")
                .build();
    }

    public PageResponse<PushCampaignDto> listCampaigns(int page, int size) {
        Page<PushCampaign> result = pushCampaignRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return PageResponse.of(result.map(PushCampaignDto::from));
    }

    private void sendToToken(PushCampaign campaign, String token) throws FirebaseMessagingException {
        Notification.Builder notificationBuilder = Notification.builder()
                .setTitle(campaign.getTitle())
                .setBody(campaign.getBody());
        if (campaign.getImageUrl() != null) {
            notificationBuilder.setImage(campaign.getImageUrl());
        }

        Map<String, String> data = new HashMap<>();
        data.put("campaignId", String.valueOf(campaign.getId()));
        if (campaign.getLinkUrl() != null) {
            data.put("linkUrl", campaign.getLinkUrl());
        }
        data.put("linkTarget", campaign.getLinkTarget().name());

        Message message = Message.builder()
                .setToken(token)
                .setNotification(notificationBuilder.build())
                .putAllData(data)
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .build();

        FirebaseMessaging.getInstance().send(message);
    }

    private static boolean isInvalidToken(FirebaseMessagingException e) {
        String code = e.getMessagingErrorCode() != null ? e.getMessagingErrorCode().name() : "";
        return code.contains("UNREGISTERED") || code.contains("INVALID_ARGUMENT");
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
