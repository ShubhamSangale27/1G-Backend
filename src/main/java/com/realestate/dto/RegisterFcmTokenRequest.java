package com.realestate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterFcmTokenRequest {

    @NotBlank
    @Size(max = 512)
    private String token;

    @NotBlank
    @Pattern(regexp = "ANDROID|IOS")
    private String platform;
}
