package com.realestate.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FaqPromoteRequest {

    @NotBlank
    private String answer;

    private String keywords;

    private Boolean active;

    private Integer sortOrder;
}
