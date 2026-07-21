package com.realestate.controller;

import com.realestate.dto.PageResponse;
import com.realestate.dto.PushCampaignDto;
import com.realestate.dto.SendPushNotificationRequest;
import com.realestate.dto.SendPushNotificationResponse;
import com.realestate.security.UserPrincipal;
import com.realestate.service.PushNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/push-notifications")
@RequiredArgsConstructor
@Tag(name = "Admin Push Notifications", description = "Send push notifications to mobile app users")
@SecurityRequirement(name = "bearerAuth")
public class AdminPushNotificationController {

    private final PushNotificationService pushNotificationService;

    @PostMapping("/send")
    @Operation(summary = "Send push notification to mobile users")
    public ResponseEntity<SendPushNotificationResponse> send(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SendPushNotificationRequest request) {
        return ResponseEntity.ok(pushNotificationService.sendCampaign(principal.getId(), request));
    }

    @GetMapping
    @Operation(summary = "List push notification campaigns")
    public ResponseEntity<PageResponse<PushCampaignDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(pushNotificationService.listCampaigns(page, size));
    }
}
