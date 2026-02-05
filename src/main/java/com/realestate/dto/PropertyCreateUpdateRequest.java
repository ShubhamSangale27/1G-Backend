package com.realestate.dto;

import com.realestate.entity.Property;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PropertyCreateUpdateRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Property.ListingType listingType;

    @NotNull
    private Property.PropertyType propertyType;

    @NotNull
    @DecimalMin("0")
    private BigDecimal price;

    @NotBlank
    private String address;

    private String city;
    private String state;
    private String pincode;
    private String locality;

    private Double latitude;
    private Double longitude;

    private Integer bedrooms;
    private Integer bathrooms;
    private BigDecimal areaSqft;
    private String amenities;

    private List<PropertyImageDto> images;

    /** Optional: when admin creates property on behalf of another user */
    private Long ownerId;
}
