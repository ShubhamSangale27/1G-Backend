package com.realestate.dto;

import com.realestate.entity.CarouselSlide;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarouselSlideDto {

    private Long id;
    private String imageUrl;
    private String linkUrl;
    private String altText;
    private Integer displayOrder;
    private Boolean active;

    public static CarouselSlideDto from(CarouselSlide slide) {
        if (slide == null) return null;
        return CarouselSlideDto.builder()
                .id(slide.getId())
                .imageUrl(slide.getImageUrl())
                .linkUrl(slide.getLinkUrl())
                .altText(slide.getAltText())
                .displayOrder(slide.getDisplayOrder())
                .active(slide.getActive())
                .build();
    }
}
