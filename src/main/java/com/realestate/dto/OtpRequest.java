package com.realestate.dto;

import com.realestate.entity.OtpVerification;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpRequest {

    @NotBlank
    private String identifier;

    private OtpVerification.OtpChannel channel;
}
