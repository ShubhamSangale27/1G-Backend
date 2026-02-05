package com.realestate.dto;

import com.realestate.entity.Property;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyDto {

    private Long id;
    private String title;
    private String description;
    private Property.ListingType listingType;
    private Property.PropertyType propertyType;
    private BigDecimal price;
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
    private Property.PropertyStatus status;
    private Long ownerId;
    private String ownerName;
    private Boolean isPremium;
    private Instant premiumExpiresAt;
    private List<PropertyImageDto> images;
    private Long viewCount;
    private Long clickCount;
    private Long visitCount;
    private Instant createdAt;
    private Boolean featured;

    public static PropertyDto from(Property p) {
        return from(p, false);
    }

    public static PropertyDto from(Property p, boolean includeAnalytics) {
        if (p == null) return null;
        List<PropertyImageDto> imgDtos = p.getImages() != null
                ? p.getImages().stream().map(PropertyImageDto::from).collect(Collectors.toList())
                : null;
        return PropertyDto.builder()
                .id(p.getId())
                .title(p.getTitle())
                .description(p.getDescription())
                .listingType(p.getListingType())
                .propertyType(p.getPropertyType())
                .price(p.getPrice())
                .address(p.getAddress())
                .city(p.getCity())
                .state(p.getState())
                .pincode(p.getPincode())
                .locality(p.getLocality())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .bedrooms(p.getBedrooms())
                .bathrooms(p.getBathrooms())
                .areaSqft(p.getAreaSqft())
                .amenities(p.getAmenities())
                .status(p.getStatus())
                .ownerId(p.getOwner() != null ? p.getOwner().getId() : null)
                .ownerName(p.getOwner() != null ? p.getOwner().getFullName() : null)
                .isPremium(p.getIsPremium() != null && p.getIsPremium())
                .premiumExpiresAt(p.getPremiumExpiresAt())
                .images(imgDtos)
                .createdAt(p.getCreatedAt())
                .featured(p.getFeatured() != null && p.getFeatured())
                .build();
    }
}
