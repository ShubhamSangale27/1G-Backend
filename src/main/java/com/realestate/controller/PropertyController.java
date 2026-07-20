package com.realestate.controller;

import com.realestate.dto.*;
import com.realestate.entity.PropertyAnalytics;
import com.realestate.security.UserPrincipal;
import com.realestate.service.PropertyService;
import com.realestate.service.WatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
@Tag(name = "Properties", description = "Property CRUD and search APIs")
public class PropertyController {

    private final PropertyService propertyService;
    private final WatchlistService watchlistService;

    @GetMapping("/public/featured")
    @Operation(summary = "Featured properties (public)")
    public ResponseEntity<List<PropertyDto>> getFeatured() {
        return ResponseEntity.ok(propertyService.getFeatured());
    }

    @GetMapping("/search")
    @Operation(summary = "Search properties (public)")
    public ResponseEntity<PageResponse<PropertyDto>> search(@ModelAttribute PropertySearchRequest request) {
        return ResponseEntity.ok(propertyService.search(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get property by ID (public)")
    public ResponseEntity<PropertyDto> getById(@PathVariable Long id,
                                               @RequestParam(required = false, defaultValue = "false") String includeAnalytics,
                                               @AuthenticationPrincipal UserPrincipal principal,
                                               HttpServletRequest request) {
        boolean include = "true".equalsIgnoreCase(includeAnalytics) || Boolean.parseBoolean(includeAnalytics);
        PropertyDto dto = propertyService.getById(id, include);
        if (principal != null) {
            propertyService.recordAnalytics(id, PropertyAnalytics.AnalyticsType.VIEW, principal.getId(),
                    request.getRemoteAddr(), request.getHeader("User-Agent"));
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/analytics/view")
    @Operation(summary = "Record view")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> recordView(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, HttpServletRequest req) {
        propertyService.recordAnalytics(id, PropertyAnalytics.AnalyticsType.VIEW, principal.getId(), req.getRemoteAddr(), req.getHeader("User-Agent"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/analytics/click")
    @Operation(summary = "Record click")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> recordClick(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, HttpServletRequest req) {
        propertyService.recordAnalytics(id, PropertyAnalytics.AnalyticsType.CLICK, principal.getId(), req.getRemoteAddr(), req.getHeader("User-Agent"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/analytics/visit")
    @Operation(summary = "Record visit")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> recordVisit(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, HttpServletRequest req) {
        propertyService.recordAnalytics(id, PropertyAnalytics.AnalyticsType.VISIT, principal.getId(), req.getRemoteAddr(), req.getHeader("User-Agent"));
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @Operation(summary = "Create property")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PropertyDto> create(@Valid @RequestBody PropertyCreateUpdateRequest request,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(propertyService.create(request, principal));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update property")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PropertyDto> update(@PathVariable Long id,
                                             @Valid @RequestBody PropertyCreateUpdateRequest request,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(propertyService.update(id, request, principal));
    }

    @GetMapping("/my")
    @Operation(summary = "My properties")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PageResponse<PropertyDto>> getMyProperties(@AuthenticationPrincipal UserPrincipal principal,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(propertyService.getMyProperties(principal.getId(), page, size));
    }

    @GetMapping("/{id}/watchlist")
    @Operation(summary = "Check if in watchlist")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Boolean>> isInWatchlist(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of("inWatchlist", watchlistService.isInWatchlist(principal.getId(), id)));
    }

    @PostMapping("/{id}/watchlist")
    @Operation(summary = "Add to watchlist")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> addToWatchlist(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        watchlistService.add(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/watchlist")
    @Operation(summary = "Remove from watchlist")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Boolean>> removeFromWatchlist(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        watchlistService.remove(principal.getId(), id);
        return ResponseEntity.ok(Map.of("removed", true));
    }

    @GetMapping("/watchlist")
    @Operation(summary = "Get watchlist")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PageResponse<PropertyDto>> getWatchlist(@AuthenticationPrincipal UserPrincipal principal,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(watchlistService.getWatchlist(principal.getId(), page, size));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete property")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Boolean>> deleteProperty(@PathVariable Long id,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        PropertyDto property = propertyService.getById(id, false);
        if (!property.getOwnerId().equals(principal.getId()) && !principal.getRole().equals("ADMIN")) {
            throw new org.springframework.security.access.AccessDeniedException("Not authorized to delete this property");
        }
        propertyService.delete(id);
        return ResponseEntity.ok(Map.of("deleted", true));
    }
}
