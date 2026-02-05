package com.realestate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Returned after signup. No tokens until email and mobile OTP are verified. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupResponse {

    private String message;
    private String email;
    private String mobile;
}
