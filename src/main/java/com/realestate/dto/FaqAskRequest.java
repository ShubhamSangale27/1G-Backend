package com.realestate.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FaqAskRequest {

    @NotBlank
    private String question;
}
