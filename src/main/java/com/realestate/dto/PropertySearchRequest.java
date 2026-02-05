package com.realestate.dto;

import com.realestate.entity.Property;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PropertySearchRequest {

    private String city;
    private String locality;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Property.ListingType listingType;
    private Property.PropertyType propertyType;
    private Integer bedrooms;
    private BigDecimal minArea;
    private String amenities;
    private int page = 0;
    private int size = 12;
    private String sort = "createdAt";
    private String direction = "desc";
}
