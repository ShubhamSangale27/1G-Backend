package com.realestate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank
    private String otp;
    @NotBlank
    @Size(min = 6, max = 100)
    private String newPassword;
}
