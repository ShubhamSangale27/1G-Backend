package com.realestate.controller;

import com.realestate.dto.FaqAskRequest;
import com.realestate.dto.FaqAskResponse;
import com.realestate.dto.FaqDto;
import com.realestate.security.UserPrincipal;
import com.realestate.service.FaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/faq")
@RequiredArgsConstructor
@Tag(name = "FAQ", description = "Public FAQ chatbot APIs")
public class FaqController {

    private final FaqService faqService;

    @GetMapping
    @Operation(summary = "List active FAQs")
    public ResponseEntity<List<FaqDto>> listActive() {
        return ResponseEntity.ok(faqService.listActive());
    }

    @PostMapping("/ask")
    @Operation(summary = "Ask a question; returns FAQ answer or contact fallback")
    public ResponseEntity<FaqAskResponse> ask(@Valid @RequestBody FaqAskRequest request,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(faqService.ask(request.getQuestion(), principal));
    }
}
