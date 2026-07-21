package com.realestate.dto;

import com.realestate.entity.MarketArea;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketAreaDto {
    private Long id;
    private Long parentId;
    private String level;
    private String name;
    private String stateName;
    private String cityName;
    private String stateSlug;
    private String citySlug;
    private String locationSlug;
    private boolean active;
    private int sortOrder;

    public static MarketAreaDto from(MarketArea a) {
        return MarketAreaDto.builder()
                .id(a.getId())
                .parentId(a.getParent() != null ? a.getParent().getId() : null)
                .level(a.getLevel().name())
                .name(a.getName())
                .stateName(a.getStateName())
                .cityName(a.getCityName())
                .stateSlug(a.getStateSlug())
                .citySlug(a.getCitySlug())
                .locationSlug(a.getLocationSlug())
                .active(a.isActive())
                .sortOrder(a.getSortOrder())
                .build();
    }
}
