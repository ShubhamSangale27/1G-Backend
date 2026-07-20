package com.realestate.dto;

import lombok.Data;

@Data
public class CarouselSlideUpdateRequest {

    private String imageUrl;
    private String linkUrl;
    private String altText;
    private Integer displayOrder;
    private Boolean active;
}
