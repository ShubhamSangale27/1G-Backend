package com.realestate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentIntentRequest {

    @NotNull
    private Long planId;

    private Long propertyId;
}
