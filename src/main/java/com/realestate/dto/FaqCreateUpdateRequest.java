package com.realestate.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FaqCreateUpdateRequest {

    @NotBlank
    private String question;

    @NotBlank
    private String answer;

    private String keywords;

    private Boolean active;

    private Integer sortOrder;
}
