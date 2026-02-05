package com.realestate.controller;

import com.realestate.dto.AlertDto;
import com.realestate.dto.PageResponse;
import com.realestate.security.UserPrincipal;
import com.realestate.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "Notification APIs")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "Get my alerts")
    public ResponseEntity<PageResponse<AlertDto>> getAlerts(@AuthenticationPrincipal UserPrincipal principal,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(alertService.getByUserId(principal.getId(), page, size));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Unread count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of("count", alertService.getUnreadCount(principal.getId())));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark as read")
    public ResponseEntity<AlertDto> markAsRead(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(alertService.markAsRead(id, principal.getId()));
    }
}
