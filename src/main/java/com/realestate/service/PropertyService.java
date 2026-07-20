package com.realestate.service;

import com.realestate.dto.*;
import com.realestate.entity.Property;
import com.realestate.entity.PropertyImage;
import com.realestate.entity.PropertyAnalytics;
import com.realestate.entity.User;
import com.realestate.exception.BadRequestException;
import com.realestate.exception.ResourceNotFoundException;
import com.realestate.repository.PropertyAnalyticsRepository;
import com.realestate.repository.PropertyRepository;
import com.realestate.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyAnalyticsRepository analyticsRepository;
    private final UserService userService;

    public PageResponse<PropertyDto> search(PropertySearchRequest req) {
        Specification<Property> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), Property.PropertyStatus.APPROVED));
            if (req.getCity() != null && !req.getCity().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("city")), "%" + req.getCity().toLowerCase() + "%"));
            }
            if (req.getLocality() != null && !req.getLocality().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("locality")), "%" + req.getLocality().toLowerCase() + "%"));
            }
            if (req.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), req.getMinPrice()));
            }
            if (req.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), req.getMaxPrice()));
            }
            if (req.getListingType() != null) {
                predicates.add(cb.equal(root.get("listingType"), req.getListingType()));
            }
            if (req.getPropertyType() != null) {
                predicates.add(cb.equal(root.get("propertyType"), req.getPropertyType()));
            }
            if (req.getBedrooms() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bedrooms"), req.getBedrooms()));
            }
            if (req.getMinArea() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("areaSqft"), req.getMinArea()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Sort sort = Sort.by(Sort.Direction.fromString(req.getDirection()), req.getSort());
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), sort);
        Page<Property> page = propertyRepository.findAll(spec, pageable);
        List<PropertyDto> dtos = page.getContent().stream()
                .map(p -> PropertyDto.from(p))
                .toList();
        return PageResponse.<PropertyDto>builder()
                .content(dtos)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    public List<PropertyDto> getFeatured() {
        Pageable limit = PageRequest.of(0, 10);
        List<Property> list = propertyRepository.findByFeaturedTrueAndStatusOrderByCreatedAtDesc(Property.PropertyStatus.APPROVED, limit);
        if (list == null || list.isEmpty()) {
            list = propertyRepository.findTop10ByStatusOrderByCreatedAtDesc(Property.PropertyStatus.APPROVED);
        }
        return list.stream().map(PropertyDto::from).toList();
    }

    public PropertyDto getById(Long id, boolean includeAnalytics) {
        Property p = propertyRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Property", id));
        PropertyDto dto = PropertyDto.from(p, includeAnalytics);
        if (includeAnalytics) {
            dto.setViewCount(analyticsRepository.countByPropertyIdAndType(id, PropertyAnalytics.AnalyticsType.VIEW));
            dto.setClickCount(analyticsRepository.countByPropertyIdAndType(id, PropertyAnalytics.AnalyticsType.CLICK));
            dto.setVisitCount(analyticsRepository.countByPropertyIdAndType(id, PropertyAnalytics.AnalyticsType.VISIT));
        }
        return dto;
    }

    @Transactional
    public PropertyDto create(PropertyCreateUpdateRequest request, UserPrincipal principal) {
        User owner = request.getOwnerId() != null && "ADMIN".equals(principal.getRole())
                ? userService.getById(request.getOwnerId())
                : userService.getById(principal.getId());
        Property p = mapToEntity(request, null);
        p.setOwner(owner);
        p.setStatus("ADMIN".equals(principal.getRole()) ? Property.PropertyStatus.APPROVED : Property.PropertyStatus.PENDING_APPROVAL);
        p = propertyRepository.save(p);
        if (request.getImages() != null) {
            int order = 0;
            for (PropertyImageDto img : request.getImages()) {
                PropertyImage pi = PropertyImage.builder()
                        .property(p)
                        .imageUrl(img.getImageUrl())
                        .mediaType(img.getMediaType() != null ? img.getMediaType() : PropertyImage.MediaType.IMAGE)
                        .caption(img.getCaption())
                        .displayOrder(order++)
                        .build();
                p.getImages().add(pi);
            }
            propertyRepository.save(p);
        }
        return PropertyDto.from(p);
    }

    @Transactional
    public PropertyDto update(Long id, PropertyCreateUpdateRequest request, UserPrincipal principal) {
        Property p = propertyRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Property", id));
        if (!p.getOwner().getId().equals(principal.getId()) && !principal.getRole().equals("ADMIN")) {
            throw new org.springframework.security.access.AccessDeniedException("Not owner");
        }
        mapToEntity(request, p);
        p.getImages().clear();
        if (request.getImages() != null) {
            int order = 0;
            for (PropertyImageDto img : request.getImages()) {
                PropertyImage pi = PropertyImage.builder()
                        .property(p)
                        .imageUrl(img.getImageUrl())
                        .mediaType(img.getMediaType() != null ? img.getMediaType() : PropertyImage.MediaType.IMAGE)
                        .caption(img.getCaption())
                        .displayOrder(order++)
                        .build();
                p.getImages().add(pi);
            }
        }
        p = propertyRepository.save(p);
        return PropertyDto.from(p);
    }

    public PageResponse<PropertyDto> getMyProperties(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Property> result = propertyRepository.findByOwnerId(userId, pageable);
        List<PropertyDto> dtos = result.getContent().stream().map(PropertyDto::from).toList();
        return PageResponse.<PropertyDto>builder()
                .content(dtos)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    @Transactional
    public void delete(Long id) {
        Property p = propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property", id));
        propertyRepository.delete(p);
    }

    @Transactional
    public void recordAnalytics(Long propertyId, PropertyAnalytics.AnalyticsType type, Long userId, String ip, String userAgent) {
        Property p = propertyRepository.findById(propertyId).orElse(null);
        if (p == null) return;
        PropertyAnalytics pa = PropertyAnalytics.builder()
                .property(p)
                .type(type)
                .userId(userId)
                .ipAddress(ip)
                .userAgent(userAgent)
                .recordedAt(Instant.now())
                .build();
        analyticsRepository.save(pa);
    }

    private Property mapToEntity(PropertyCreateUpdateRequest req, Property existing) {
        Property p = existing != null ? existing : new Property();
        p.setTitle(req.getTitle());
        p.setDescription(req.getDescription());
        p.setListingType(req.getListingType());
        p.setPropertyType(req.getPropertyType());
        p.setPrice(req.getPrice());
        p.setAddress(req.getAddress());
        p.setCity(req.getCity());
        p.setState(req.getState());
        p.setPincode(req.getPincode());
        p.setLocality(req.getLocality());
        p.setLatitude(req.getLatitude());
        p.setLongitude(req.getLongitude());
        p.setBedrooms(req.getBedrooms());
        p.setBathrooms(req.getBathrooms());
        p.setAreaSqft(req.getAreaSqft());
        p.setAmenities(req.getAmenities());
        return p;
    }
}
