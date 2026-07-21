package com.realestate.controller;

import com.realestate.dto.RegisterFcmTokenRequest;
import com.realestate.security.UserPrincipal;
import com.realestate.service.DeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
@Tag(name = "Devices", description = "Mobile device token registration")
@SecurityRequirement(name = "bearerAuth")
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping("/fcm-token")
    @Operation(summary = "Register or refresh FCM token for current user")
    public ResponseEntity<Map<String, Boolean>> registerToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RegisterFcmTokenRequest request) {
        deviceTokenService.registerToken(principal.getId(), request.getToken(), request.getPlatform());
        return ResponseEntity.ok(Map.of("registered", true));
    }

    @DeleteMapping("/fcm-token")
    @Operation(summary = "Deactivate FCM token on logout")
    public ResponseEntity<Map<String, Boolean>> deactivateToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String token) {
        deviceTokenService.deactivateToken(principal.getId(), token);
        return ResponseEntity.ok(Map.of("deactivated", true));
    }
}
