package com.realestate.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Mobile verification via OTP (sent by backend via MSG91). mobileOtp is required.
 */
@Data
public class VerifySignupRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 10, max = 15)
    private String mobile;

    /** 6-digit OTP sent to mobile via MSG91. */
    @NotBlank
    @Size(min = 6, max = 6)
    private String mobileOtp;
}
