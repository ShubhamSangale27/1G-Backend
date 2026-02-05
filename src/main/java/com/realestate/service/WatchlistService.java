package com.realestate.service;

import com.realestate.dto.PageResponse;
import com.realestate.dto.PropertyDto;
import com.realestate.entity.Property;
import com.realestate.entity.Watchlist;
import com.realestate.exception.BadRequestException;
import com.realestate.exception.ResourceNotFoundException;
import com.realestate.entity.User;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.UserRepository;
import com.realestate.repository.WatchlistRepository;
import com.realestate.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    @Transactional
    public void add(Long userId, Long propertyId) {
        if (watchlistRepository.existsByUserIdAndPropertyId(userId, propertyId)) {
            throw new BadRequestException("Already in watchlist");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Property p = propertyRepository.findById(propertyId).orElseThrow(() -> new ResourceNotFoundException("Property", propertyId));
        Watchlist wl = Watchlist.builder()
                .user(user)
                .property(p)
                .build();
        watchlistRepository.save(wl);
    }

    @Transactional
    public void remove(Long userId, Long propertyId) {
        watchlistRepository.deleteByUserIdAndPropertyId(userId, propertyId);
    }

    public boolean isInWatchlist(Long userId, Long propertyId) {
        return watchlistRepository.existsByUserIdAndPropertyId(userId, propertyId);
    }

    public PageResponse<PropertyDto> getWatchlist(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Watchlist> result = watchlistRepository.findByUserId(userId, pageable);
        List<PropertyDto> dtos = result.getContent().stream()
                .map(w -> PropertyDto.from(w.getProperty()))
                .collect(Collectors.toList());
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
}
