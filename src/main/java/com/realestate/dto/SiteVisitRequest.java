package com.realestate.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class SiteVisitRequest {

    @NotNull
    private Long propertyId;

    @NotNull
    private Instant scheduledAt;

    private String userNotes;
}
