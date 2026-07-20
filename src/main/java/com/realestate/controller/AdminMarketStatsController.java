package com.realestate.controller;

import com.realestate.dto.*;
import com.realestate.service.MarketStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/market-stats")
@RequiredArgsConstructor
@Tag(name = "Admin Market Stats", description = "Admin-managed market areas and snapshots")
@SecurityRequirement(name = "bearerAuth")
public class AdminMarketStatsController {

    private final MarketStatsService marketStatsService;

    @GetMapping("/areas")
    @Operation(summary = "List all market areas")
    public ResponseEntity<List<MarketAreaDto>> listAreas() {
        return ResponseEntity.ok(marketStatsService.listAllAreasAdmin());
    }

    @PostMapping("/areas")
    @Operation(summary = "Create market area")
    public ResponseEntity<MarketAreaDto> createArea(@Valid @RequestBody MarketAreaCreateUpdateRequest request) {
        return ResponseEntity.ok(marketStatsService.createArea(request));
    }

    @PutMapping("/areas/{id}")
    @Operation(summary = "Update market area")
    public ResponseEntity<MarketAreaDto> updateArea(@PathVariable Long id,
                                                    @Valid @RequestBody MarketAreaCreateUpdateRequest request) {
        return ResponseEntity.ok(marketStatsService.updateArea(id, request));
    }

    @DeleteMapping("/areas/{id}")
    @Operation(summary = "Delete market area")
    public ResponseEntity<Map<String, Boolean>> deleteArea(@PathVariable Long id) {
        marketStatsService.deleteArea(id);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @GetMapping("/snapshots")
    @Operation(summary = "List snapshots for an area")
    public ResponseEntity<List<MarketStatSnapshotDto>> listSnapshots(@RequestParam Long areaId) {
        return ResponseEntity.ok(marketStatsService.listSnapshots(areaId));
    }

    @PostMapping("/snapshots")
    @Operation(summary = "Create snapshot")
    public ResponseEntity<MarketStatSnapshotDto> createSnapshot(
            @Valid @RequestBody MarketStatSnapshotCreateUpdateRequest request) {
        return ResponseEntity.ok(marketStatsService.createSnapshot(request));
    }

    @PutMapping("/snapshots/{id}")
    @Operation(summary = "Update snapshot")
    public ResponseEntity<MarketStatSnapshotDto> updateSnapshot(
            @PathVariable Long id,
            @Valid @RequestBody MarketStatSnapshotCreateUpdateRequest request) {
        return ResponseEntity.ok(marketStatsService.updateSnapshot(id, request));
    }

    @DeleteMapping("/snapshots/{id}")
    @Operation(summary = "Delete snapshot")
    public ResponseEntity<Map<String, Boolean>> deleteSnapshot(@PathVariable Long id) {
        marketStatsService.deleteSnapshot(id);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @PostMapping("/import/rbi")
    @Operation(summary = "Import free/open RBI-style seed data for major cities")
    public ResponseEntity<Map<String, Object>> importRbi() {
        int upserted = marketStatsService.importRbiSeed();
        return ResponseEntity.ok(Map.of("upserted", upserted, "source", "RBI_HPI_SEED"));
    }
}
