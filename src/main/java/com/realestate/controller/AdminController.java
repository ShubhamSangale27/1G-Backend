package com.realestate.controller;

import com.realestate.dto.*;
import com.realestate.security.UserPrincipal;
import com.realestate.service.AdminService;
import com.realestate.service.PropertyService;
import com.realestate.service.SiteVisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin APIs")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;
    private final SiteVisitService siteVisitService;
    private final PropertyService propertyService;

    @GetMapping("/properties")
    @Operation(summary = "Get all properties (admin) with optional filters")
    public ResponseEntity<PageResponse<PropertyDto>> getAllProperties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean featuredOnly,
            @RequestParam(required = false) Boolean newOnly) {
        return ResponseEntity.ok(adminService.getAllProperties(page, size, featuredOnly, newOnly));
    }

    @PostMapping("/properties")
    @Operation(summary = "Create property (admin); optional ownerId in body")
    public ResponseEntity<PropertyDto> createProperty(@RequestBody PropertyCreateUpdateRequest request,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(propertyService.create(request, principal));
    }

    @PutMapping("/properties/{id}")
    @Operation(summary = "Update any property (admin)")
    public ResponseEntity<PropertyDto> updateProperty(@PathVariable Long id,
                                                      @RequestBody PropertyCreateUpdateRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(propertyService.update(id, request, principal));
    }

    @DeleteMapping("/properties/{id}")
    @Operation(summary = "Delete any property (admin)")
    public ResponseEntity<Void> deleteProperty(@PathVariable Long id) {
        propertyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/properties/{id}/featured")
    @Operation(summary = "Mark property as featured or not")
    public ResponseEntity<PropertyDto> setFeatured(@PathVariable Long id, @RequestParam boolean featured) {
        return ResponseEntity.ok(adminService.setFeatured(id, featured));
    }

    @GetMapping("/properties/{id}/viewers")
    @Operation(summary = "List users who viewed this property")
    public ResponseEntity<List<UserDto>> getPropertyViewers(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getPropertyViewers(id));
    }

    @GetMapping("/properties/{id}/likers")
    @Operation(summary = "List users who liked (watchlist) this property")
    public ResponseEntity<List<UserDto>> getPropertyLikers(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getPropertyLikers(id));
    }

    @GetMapping("/properties/pending")
    @Operation(summary = "Get pending properties")
    public ResponseEntity<List<PropertyDto>> getPendingProperties(@RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getPendingProperties(page, size));
    }

    @PutMapping("/properties/{id}/approve")
    @Operation(summary = "Approve property")
    public ResponseEntity<PropertyDto> approveProperty(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.approveProperty(id));
    }

    @PutMapping("/properties/{id}/reject")
    @Operation(summary = "Reject property")
    public ResponseEntity<PropertyDto> rejectProperty(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.rejectProperty(id));
    }

    @GetMapping("/properties/{id}/analytics")
    @Operation(summary = "Get property analytics")
    public ResponseEntity<AnalyticsSummaryDto> getPropertyAnalytics(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getPropertyAnalytics(id));
    }

    @GetMapping("/analytics/properties")
    @Operation(summary = "Get analytics by property and date range")
    public ResponseEntity<List<Map<String, Object>>> getPropertiesAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(adminService.getPropertiesAnalytics(from, to));
    }

    @GetMapping("/metrics")
    @Operation(summary = "Global site metrics")
    public ResponseEntity<Map<String, Object>> getGlobalMetrics() {
        return ResponseEntity.ok(adminService.getGlobalMetrics());
    }

    @GetMapping("/sitevisits/pending")
    @Operation(summary = "Pending site visit assignments")
    public ResponseEntity<List<SiteVisitDto>> getPendingSiteVisits(@RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(siteVisitService.getPendingAssignment(page, size));
    }

    @GetMapping("/sitevisits")
    @Operation(summary = "All site visits with filters (date range, assigned to). Due today shown first.")
    public ResponseEntity<AdminSiteVisitsResponse> getAllSiteVisits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long agentId) {
        return ResponseEntity.ok(siteVisitService.getAllForAdmin(page, size, from, to, agentId));
    }

    @GetMapping("/agents")
    @Operation(summary = "List agents (for assigning to site visits)")
    public ResponseEntity<List<UserDto>> getAgents() {
        return ResponseEntity.ok(adminService.getAgents());
    }

    @PutMapping("/sitevisits/{id}/assign")
    @Operation(summary = "Assign agent to site visit (pending only)")
    public ResponseEntity<SiteVisitDto> assignAgent(@PathVariable Long id, @RequestParam Long agentId,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(siteVisitService.assignAgent(id, agentId, principal));
    }

    @PutMapping("/sitevisits/{id}/reassign")
    @Operation(summary = "Reassign visit to another agent (ASSIGNED or PENDING)")
    public ResponseEntity<SiteVisitDto> reassignAgent(@PathVariable Long id, @RequestParam Long agentId,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(siteVisitService.reassignAgent(id, agentId, principal));
    }
}
