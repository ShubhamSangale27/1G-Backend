package com.realestate.controller;

import com.realestate.dto.CarouselSlideDto;
import com.realestate.service.CarouselService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/carousel")
@RequiredArgsConstructor
@Tag(name = "Carousel", description = "Public homepage carousel slides")
public class CarouselController {

    private final CarouselService carouselService;

    @GetMapping("/slides")
    @Operation(summary = "List active homepage carousel slides")
    public ResponseEntity<List<CarouselSlideDto>> getActiveSlides() {
        return ResponseEntity.ok(carouselService.getActiveSlides());
    }
}
