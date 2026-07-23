package com.realestate.controller;

import com.realestate.dto.MarketAreaDto;
import com.realestate.dto.MarketProjectionRequest;
import com.realestate.dto.MarketProjectionResponse;
import com.realestate.dto.MarketStatsResponse;
import com.realestate.service.MarketStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/market-stats")
@RequiredArgsConstructor
@Tag(name = "Market Stats", description = "Public real-estate market statistics for SIP calculator")
public class MarketStatsController {

    private final MarketStatsService marketStatsService;

    @GetMapping("/areas")
    @Operation(summary = "List market areas for cascading selectors")
    public ResponseEntity<List<MarketAreaDto>> listAreas(
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String level) {
        return ResponseEntity.ok(marketStatsService.listAreas(parentId, state, city, level));
    }

    @GetMapping
    @Operation(summary = "Get historical stats and derived CAGR for an area/range")
    public ResponseEntity<MarketStatsResponse> getStats(
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Long localityId,
            @RequestParam(defaultValue = "5Y") String range) {
        if (localityId != null || areaId != null) {
            return ResponseEntity.ok(marketStatsService.getStats(
                    localityId != null ? localityId : areaId, range));
        }
        return ResponseEntity.ok(marketStatsService.getStatsByLocation(state, city, null, range));
    }

    @PostMapping("/projection")
    @Operation(summary = "Project regional + user investment lines for calculator")
    public ResponseEntity<MarketProjectionResponse> project(
            @Valid @RequestBody MarketProjectionRequest request) {
        return ResponseEntity.ok(marketStatsService.project(request));
    }
}
