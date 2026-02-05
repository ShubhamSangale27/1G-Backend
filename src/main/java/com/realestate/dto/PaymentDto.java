package com.realestate.dto;

import com.realestate.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {

    private Long id;
    private Long userId;
    private Long propertyId;
    private Long planId;
    private BigDecimal amount;
    private Payment.PaymentStatus status;
    private Payment.PaymentProvider provider;
    private String receiptUrl;
    private Instant createdAt;

    public static PaymentDto from(Payment p) {
        if (p == null) return null;
        return PaymentDto.builder()
                .id(p.getId())
                .userId(p.getUser() != null ? p.getUser().getId() : null)
                .propertyId(p.getProperty() != null ? p.getProperty().getId() : null)
                .planId(p.getPlan() != null ? p.getPlan().getId() : null)
                .amount(p.getAmount())
                .status(p.getStatus())
                .provider(p.getProvider())
                .receiptUrl(p.getReceiptUrl())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
