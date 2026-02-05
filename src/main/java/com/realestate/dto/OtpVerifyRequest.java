package com.realestate.dto;

import com.realestate.entity.OtpVerification;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpVerifyRequest {

    @NotBlank
    private String identifier;

    @NotBlank
    private String otpCode;

    private OtpVerification.OtpChannel channel;
}
