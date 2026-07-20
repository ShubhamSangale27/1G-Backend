package com.realestate.controller;

import com.realestate.dto.PageResponse;
import com.realestate.dto.PaymentDto;
import com.realestate.dto.PaymentIntentRequest;
import com.realestate.security.UserPrincipal;
import com.realestate.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment APIs")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/my")
    @Operation(summary = "My payments")
    public ResponseEntity<PageResponse<PaymentDto>> getMyPayments(@AuthenticationPrincipal UserPrincipal principal,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(paymentService.getMyPayments(principal.getId(), page, size));
    }

    @PostMapping("/create-intent")
    @Operation(summary = "Create payment intent (Stripe)")
    public ResponseEntity<PaymentDto> createPaymentIntent(@Valid @RequestBody PaymentIntentRequest request,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(paymentService.createPaymentIntent(request, principal));
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm payment (webhook or callback)")
    public ResponseEntity<Void> confirmPayment(@RequestBody Map<String, String> body) {
        String externalId = body.get("externalId");
        String receiptUrl = body.get("receiptUrl");
        if (externalId != null) {
            paymentService.confirmPayment(externalId, receiptUrl != null ? receiptUrl : "");
        }
        return ResponseEntity.ok().build();
    }
}
