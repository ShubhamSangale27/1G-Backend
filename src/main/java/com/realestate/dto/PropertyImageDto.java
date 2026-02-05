package com.realestate.dto;

import com.realestate.entity.PropertyImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyImageDto {

    private Long id;
    private String imageUrl;
    private String caption;
    private Integer displayOrder;

    public static PropertyImageDto from(PropertyImage img) {
        if (img == null) return null;
        return PropertyImageDto.builder()
                .id(img.getId())
                .imageUrl(img.getImageUrl())
                .caption(img.getCaption())
                .displayOrder(img.getDisplayOrder())
                .build();
    }
}
