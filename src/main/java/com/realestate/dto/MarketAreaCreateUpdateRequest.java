package com.realestate.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketAreaCreateUpdateRequest {

    private Long parentId;

    @NotBlank
    private String level;

    @NotBlank
    private String name;

    private String stateName;
    private String cityName;
    private Boolean active;
    private Integer sortOrder;
}
