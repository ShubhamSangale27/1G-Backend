package com.realestate.service;

import com.realestate.config.FirebaseInitializer;
import com.realestate.dto.SendPushNotificationRequest;
import com.realestate.dto.SendPushNotificationResponse;
import com.realestate.entity.DeviceToken;
import com.realestate.entity.PushCampaign;
import com.realestate.entity.User;
import com.realestate.repository.PushCampaignRepository;
import com.realestate.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock
    private PushCampaignRepository pushCampaignRepository;
    @Mock
    private DeviceTokenService deviceTokenService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FirebaseInitializer firebaseInitializer;

    @InjectMocks
    private PushNotificationService pushNotificationService;

    private User admin;
    private SendPushNotificationRequest request;

    @BeforeEach
    void setUp() {
        admin = User.builder()
                .id(1L)
                .email("admin@test.com")
                .role(User.Role.ADMIN)
                .build();
        request = new SendPushNotificationRequest();
        request.setTitle("Test title");
        request.setBody("Test body");
        request.setTargetRole("ALL");
        request.setLinkTarget("APP");
    }

    @Test
    void sendCampaign_whenFirebaseNotReady_savesCampaignAndReturnsCredentialHint() {
        DeviceToken token = DeviceToken.builder()
                .id(10L)
                .fcmToken("fcm-token-1")
                .active(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(pushCampaignRepository.save(any(PushCampaign.class))).thenAnswer(invocation -> {
            PushCampaign campaign = invocation.getArgument(0);
            campaign.setId(99L);
            return campaign;
        });
        when(deviceTokenService.findActiveTokensForTarget(null)).thenReturn(List.of(token));
        when(firebaseInitializer.isReady()).thenReturn(false);

        SendPushNotificationResponse response = pushNotificationService.sendCampaign(1L, request);

        assertThat(response.getCampaignId()).isEqualTo(99L);
        assertThat(response.getSentCount()).isZero();
        assertThat(response.getFailedCount()).isEqualTo(1);
        assertThat(response.getMessage())
                .contains("FIREBASE_ENABLED=true")
                .contains("FIREBASE_CREDENTIALS_JSON")
                .contains("FIREBASE_CREDENTIALS_BASE64")
                .contains("FIREBASE_CREDENTIALS_PATH");

        ArgumentCaptor<PushCampaign> captor = ArgumentCaptor.forClass(PushCampaign.class);
        verify(pushCampaignRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        PushCampaign saved = captor.getAllValues().get(1);
        assertThat(saved.getSentCount()).isZero();
        assertThat(saved.getFailedCount()).isEqualTo(1);
    }

    @Test
    void sendCampaign_whenFirebaseNotReadyAndNoTokens_returnsZeroCounts() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(pushCampaignRepository.save(any(PushCampaign.class))).thenAnswer(invocation -> {
            PushCampaign campaign = invocation.getArgument(0);
            campaign.setId(100L);
            return campaign;
        });
        when(deviceTokenService.findActiveTokensForTarget(null)).thenReturn(List.of());
        when(firebaseInitializer.isReady()).thenReturn(false);

        SendPushNotificationResponse response = pushNotificationService.sendCampaign(1L, request);

        assertThat(response.getSentCount()).isZero();
        assertThat(response.getFailedCount()).isZero();
        verify(deviceTokenService).findActiveTokensForTarget(null);
    }

    @Test
    void sendCampaign_whenFirebaseNotReady_doesNotAttemptFcmSend() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(pushCampaignRepository.save(any(PushCampaign.class))).thenAnswer(invocation -> {
            PushCampaign campaign = invocation.getArgument(0);
            campaign.setId(300L);
            return campaign;
        });
        when(deviceTokenService.findActiveTokensForTarget(null)).thenReturn(List.of());
        when(firebaseInitializer.isReady()).thenReturn(false);

        pushNotificationService.sendCampaign(1L, request);

        verify(deviceTokenService, never()).deactivateByFcmToken(any());
    }
}
