package com.realestate.service;

import com.realestate.dto.PageResponse;
import com.realestate.dto.PaymentDto;
import com.realestate.dto.PaymentIntentRequest;
import com.realestate.entity.*;
import com.realestate.exception.BadRequestException;
import com.realestate.exception.ResourceNotFoundException;
import com.realestate.repository.PaymentRepository;
import com.realestate.repository.PremiumPlanRepository;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.UserRepository;
import com.realestate.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PremiumPlanRepository planRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final AlertService alertService;

    @Value("${app.stripe.api-key:}")
    private String stripeApiKey;

    public PageResponse<PaymentDto> getMyPayments(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Payment> result = paymentRepository.findByUserId(userId, pageable);
        List<PaymentDto> dtos = result.getContent().stream().map(PaymentDto::from).collect(Collectors.toList());
        return PageResponse.<PaymentDto>builder()
                .content(dtos)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    @Transactional
    public PaymentDto createPaymentIntent(PaymentIntentRequest request, UserPrincipal principal) {
        PremiumPlan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", request.getPlanId()));
        User user = userRepository.findById(principal.getId()).orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));
        Property property = request.getPropertyId() != null
                ? propertyRepository.findById(request.getPropertyId()).orElse(null)
                : null;
        Payment payment = Payment.builder()
                .user(user)
                .property(property)
                .plan(plan)
                .amount(plan.getPrice())
                .status(Payment.PaymentStatus.PENDING)
                .provider(Payment.PaymentProvider.STRIPE)
                .externalId(null)
                .build();
        payment = paymentRepository.save(payment);
        if (stripeApiKey != null && !stripeApiKey.isBlank()) {
            try {
                com.stripe.Stripe.apiKey = stripeApiKey;
                var params = new com.stripe.param.PaymentIntentCreateParams.Builder()
                        .setAmount(plan.getPrice().multiply(BigDecimal.valueOf(100)).longValue())
                        .setCurrency("inr")
                        .putMetadata("paymentId", payment.getId().toString())
                        .putMetadata("userId", user.getId().toString())
                        .build();
                var intent = com.stripe.model.PaymentIntent.create(params);
                payment.setExternalId(intent.getId());
                payment = paymentRepository.save(payment);
            } catch (Exception e) {
                log.warn("Stripe error: {}", e.getMessage());
            }
        }
        return PaymentDto.from(payment);
    }

    @Transactional
    public void confirmPayment(String externalId, String receiptUrl) {
        Payment payment = paymentRepository.findAll().stream()
                .filter(p -> externalId.equals(p.getExternalId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
        payment.setReceiptUrl(receiptUrl);
        paymentRepository.save(payment);
        if (payment.getProperty() != null) {
            Property prop = payment.getProperty();
            prop.setIsPremium(true);
            prop.setPremiumExpiresAt(Instant.now().plusSeconds(payment.getPlan().getDurationDays() != null ? payment.getPlan().getDurationDays() * 86400L : 30 * 86400L));
            propertyRepository.save(prop);
        }
        alertService.create(payment.getUser().getId(), "Payment Successful", "Your premium purchase was successful.", "PAYMENT", payment.getId());
    }

    public BigDecimal getRevenueBetween(Instant from, Instant to) {
        BigDecimal sum = paymentRepository.sumRevenueBetween(from, to);
        return sum != null ? sum : BigDecimal.ZERO;
    }
}
