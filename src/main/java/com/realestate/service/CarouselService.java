package com.realestate.service;

import com.realestate.dto.CarouselSlideCreateRequest;
import com.realestate.dto.CarouselSlideDto;
import com.realestate.dto.CarouselSlideUpdateRequest;
import com.realestate.entity.CarouselSlide;
import com.realestate.exception.BadRequestException;
import com.realestate.exception.ResourceNotFoundException;
import com.realestate.repository.CarouselSlideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarouselService {

    private final CarouselSlideRepository carouselSlideRepository;

    public List<CarouselSlideDto> getActiveSlides() {
        return carouselSlideRepository.findByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(CarouselSlideDto::from)
                .toList();
    }

    public List<CarouselSlideDto> getAllSlides() {
        return carouselSlideRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(CarouselSlideDto::from)
                .toList();
    }

    @Transactional
    public CarouselSlideDto create(CarouselSlideCreateRequest request) {
        String imageUrl = validateImageUrl(request.getImageUrl());
        String linkUrl = validateOptionalUrl(request.getLinkUrl(), "Link URL");
        int displayOrder = request.getDisplayOrder() != null
                ? request.getDisplayOrder()
                : nextDisplayOrder();
        CarouselSlide slide = CarouselSlide.builder()
                .imageUrl(imageUrl)
                .linkUrl(linkUrl)
                .altText(trimToNull(request.getAltText(), 255))
                .displayOrder(displayOrder)
                .active(request.getActive() == null || request.getActive())
                .build();
        return CarouselSlideDto.from(carouselSlideRepository.save(slide));
    }

    @Transactional
    public CarouselSlideDto update(Long id, CarouselSlideUpdateRequest request) {
        CarouselSlide slide = carouselSlideRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Carousel slide", id));
        if (request.getImageUrl() != null) {
            slide.setImageUrl(validateImageUrl(request.getImageUrl()));
        }
        if (request.getLinkUrl() != null) {
            slide.setLinkUrl(validateOptionalUrl(request.getLinkUrl(), "Link URL"));
        }
        if (request.getAltText() != null) {
            slide.setAltText(trimToNull(request.getAltText(), 255));
        }
        if (request.getDisplayOrder() != null) {
            slide.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getActive() != null) {
            slide.setActive(request.getActive());
        }
        return CarouselSlideDto.from(carouselSlideRepository.save(slide));
    }

    @Transactional
    public void delete(Long id) {
        if (!carouselSlideRepository.existsById(id)) {
            throw new ResourceNotFoundException("Carousel slide", id);
        }
        carouselSlideRepository.deleteById(id);
    }

    private int nextDisplayOrder() {
        return carouselSlideRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .mapToInt(s -> s.getDisplayOrder() != null ? s.getDisplayOrder() : 0)
                .max()
                .orElse(-1) + 1;
    }

    private static String validateImageUrl(String raw) {
        String url = trimRequired(raw, "Image URL");
        if (url.length() > 1000) {
            throw new BadRequestException("Image URL must be at most 1000 characters");
        }
        if (!isValidMediaUrl(url)) {
            throw new BadRequestException("Image URL must start with http://, https://, or /uploads/");
        }
        return url;
    }

    private static String validateOptionalUrl(String raw, String fieldName) {
        if (raw == null) return null;
        String url = raw.trim();
        if (url.isEmpty()) return null;
        if (url.length() > 1000) {
            throw new BadRequestException(fieldName + " must be at most 1000 characters");
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new BadRequestException(fieldName + " must start with http:// or https://");
        }
        return url;
    }

    private static boolean isValidMediaUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://") || url.startsWith("/uploads/");
    }

    private static String trimRequired(String raw, String fieldName) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new BadRequestException(fieldName + " is required");
        }
        return raw.trim();
    }

    private static String trimToNull(String raw, int maxLen) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() > maxLen ? trimmed.substring(0, maxLen) : trimmed;
    }
}
