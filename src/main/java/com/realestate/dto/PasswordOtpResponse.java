package com.realestate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordOtpResponse {
    private String message;
    private String maskedMobile;
    private Instant resendAvailableAt;
    private int resendAttemptsUsed;
    private int resendAttemptsRemaining;
    private int maxResendAttemptsPerDay;
}
