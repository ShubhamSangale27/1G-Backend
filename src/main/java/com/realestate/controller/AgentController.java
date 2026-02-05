package com.realestate.controller;

import com.realestate.dto.AgentAssignedVisitsResponse;
import com.realestate.dto.SiteVisitCommentDto;
import com.realestate.dto.SiteVisitDetailDto;
import com.realestate.dto.SiteVisitDto;
import com.realestate.security.UserPrincipal;
import com.realestate.service.SiteVisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
@Tag(name = "Agent", description = "Agent site visit APIs")
@SecurityRequirement(name = "bearerAuth")
public class AgentController {

    private final SiteVisitService siteVisitService;

    @GetMapping("/sitevisits")
    @Operation(summary = "List assigned site visits (due today first)")
    public ResponseEntity<AgentAssignedVisitsResponse> getAssignedVisits(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(siteVisitService.getAssignedVisitsWithDueToday(principal.getId(), page, size));
    }

    @GetMapping("/sitevisits/{id}")
    @Operation(summary = "Get site visit detail (property, user, comments)")
    public ResponseEntity<SiteVisitDetailDto> getVisitDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(siteVisitService.getDetailForAgent(id, principal));
    }

    @PostMapping("/sitevisits/{id}/complete")
    @Operation(summary = "Verify OTP and mark visit as done")
    public ResponseEntity<SiteVisitDto> completeVisit(
            @PathVariable Long id,
            @RequestParam @NotBlank String otp,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(siteVisitService.verifyAndComplete(id, otp, principal));
    }

    @PostMapping("/sitevisits/{id}/comments")
    @Operation(summary = "Add comment (visible to admin)")
    public ResponseEntity<SiteVisitCommentDto> addComment(
            @PathVariable Long id,
            @Valid @RequestBody AddCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(siteVisitService.addComment(id, request.getCommentText(), principal));
    }

    @lombok.Data
    public static class AddCommentRequest {
        @NotBlank(message = "Comment text is required")
        private String commentText;
    }
}
