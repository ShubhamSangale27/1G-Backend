package com.realestate.controller;

import com.realestate.dto.PremiumPlanDto;
import com.realestate.service.PremiumPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = {"http://www.1guntha.com", "https://www.1guntha.com"})
@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
@Tag(name = "Plans", description = "Premium plans (public)")
public class PlansController {

    private final PremiumPlanService planService;

    @GetMapping
    @Operation(summary = "List premium plans")
    public ResponseEntity<List<PremiumPlanDto>> getAll() {
        return ResponseEntity.ok(planService.getAllPlans());
    }
}
