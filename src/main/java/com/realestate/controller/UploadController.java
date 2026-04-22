package com.realestate.controller;

import com.realestate.service.ImageUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@CrossOrigin(origins = {"http://www.1guntha.com", "https://www.1guntha.com"})
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
@Tag(name = "Upload", description = "Image upload for properties")
@SecurityRequirement(name = "bearerAuth")
public class UploadController {

    private final ImageUploadService imageUploadService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload property image (resized + thumbnail)")
    public ResponseEntity<ImageUploadService.ImageUploadResult> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(imageUploadService.upload(file));
    }
}
