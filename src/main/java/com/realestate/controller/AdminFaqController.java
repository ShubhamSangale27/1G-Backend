package com.realestate.controller;

import com.realestate.dto.*;
import com.realestate.entity.UnmatchedFaqQuestion;
import com.realestate.service.FaqService;
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
@RequestMapping("/admin/faqs")
@RequiredArgsConstructor
@Tag(name = "Admin FAQ", description = "Admin FAQ management")
@SecurityRequirement(name = "bearerAuth")
public class AdminFaqController {

    private final FaqService faqService;

    @GetMapping
    @Operation(summary = "List all FAQs")
    public ResponseEntity<List<FaqDto>> listAll() {
        return ResponseEntity.ok(faqService.listAll());
    }

    @PostMapping
    @Operation(summary = "Create FAQ")
    public ResponseEntity<FaqDto> create(@Valid @RequestBody FaqCreateUpdateRequest request) {
        return ResponseEntity.ok(faqService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update FAQ")
    public ResponseEntity<FaqDto> update(@PathVariable Long id,
                                         @Valid @RequestBody FaqCreateUpdateRequest request) {
        return ResponseEntity.ok(faqService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete FAQ")
    public ResponseEntity<Map<String, Boolean>> delete(@PathVariable Long id) {
        faqService.delete(id);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @GetMapping("/unmatched")
    @Operation(summary = "List unmatched (out-of-scope) questions")
    public ResponseEntity<List<UnmatchedFaqQuestionDto>> listUnmatched(
            @RequestParam(defaultValue = "PENDING") String status) {
        UnmatchedFaqQuestion.Status st;
        try {
            st = UnmatchedFaqQuestion.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            st = UnmatchedFaqQuestion.Status.PENDING;
        }
        return ResponseEntity.ok(faqService.listUnmatched(st));
    }

    @PutMapping("/unmatched/{id}/resolve")
    @Operation(summary = "Mark unmatched question as resolved")
    public ResponseEntity<UnmatchedFaqQuestionDto> resolve(@PathVariable Long id) {
        return ResponseEntity.ok(faqService.resolve(id));
    }

    @PostMapping("/unmatched/{id}/promote")
    @Operation(summary = "Promote unmatched question to FAQ")
    public ResponseEntity<UnmatchedFaqQuestionDto> promote(@PathVariable Long id,
                                                           @Valid @RequestBody FaqPromoteRequest request) {
        return ResponseEntity.ok(faqService.promote(id, request));
    }
}
