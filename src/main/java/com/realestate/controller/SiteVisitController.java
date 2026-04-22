package com.realestate.controller;

import com.realestate.dto.PageResponse;
import com.realestate.dto.SiteVisitDto;
import com.realestate.dto.SiteVisitRequest;
import com.realestate.security.UserPrincipal;
import com.realestate.service.SiteVisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/sitevisits")
@RequiredArgsConstructor
@Tag(name = "Site Visits", description = "Site visit booking APIs")
@SecurityRequirement(name = "bearerAuth")
public class SiteVisitController {

    private final SiteVisitService siteVisitService;

    @PostMapping
    @Operation(summary = "Book site visit")
    public ResponseEntity<SiteVisitDto> book(@Valid @RequestBody SiteVisitRequest request,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(siteVisitService.book(request, principal));
    }

    @GetMapping("/my")
    @Operation(summary = "My site visits")
    public ResponseEntity<PageResponse<SiteVisitDto>> getMyVisits(@AuthenticationPrincipal UserPrincipal principal,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(siteVisitService.getMyVisits(principal.getId(), page, size));
    }

    @GetMapping("/my/for-property/{propertyId}")
    @Operation(summary = "My active site visit for this property (if any)")
    public ResponseEntity<SiteVisitDto> getMyVisitForProperty(@PathVariable Long propertyId,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        // 204 when none — avoids browser "404 Not Found" noise for the normal "no active visit" case.
        return siteVisitService.getMyVisitForProperty(principal.getId(), propertyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Verify OTP and complete visit (agent)")
    public ResponseEntity<SiteVisitDto> verifyAndComplete(@PathVariable Long id,
                                                          @RequestParam String otp,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(siteVisitService.verifyAndComplete(id, otp, principal));
    }

    @PutMapping("/{id}/reschedule")
    @Operation(summary = "Reschedule site visit (OTP unchanged)")
    public ResponseEntity<SiteVisitDto> reschedule(@PathVariable Long id,
                                                   @RequestBody Map<String, String> body,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        String scheduledAtStr = body.get("scheduledAt");
        if (scheduledAtStr == null || scheduledAtStr.isBlank()) {
            throw new com.realestate.exception.BadRequestException("scheduledAt is required");
        }
        java.time.Instant scheduledAt = java.time.Instant.parse(scheduledAtStr);
        return ResponseEntity.ok(siteVisitService.reschedule(id, scheduledAt, principal));
    }
}
