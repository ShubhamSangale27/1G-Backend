package com.realestate.service;

import com.realestate.dto.AlertDto;
import com.realestate.dto.PageResponse;
import com.realestate.entity.Alert;
import com.realestate.entity.User;
import com.realestate.exception.ResourceNotFoundException;
import com.realestate.repository.AlertRepository;
import com.realestate.repository.UserRepository;
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
public class AlertService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    @Transactional
    public Alert create(Long userId, String title, String message, String type, Long referenceId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Alert a = Alert.builder()
                .user(user)
                .title(title)
                .message(message)
                .read(false)
                .type(type)
                .referenceId(referenceId)
                .build();
        return alertRepository.save(a);
    }

    public PageResponse<AlertDto> getByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Alert> result = alertRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<AlertDto> dtos = result.getContent().stream().map(AlertDto::from).collect(Collectors.toList());
        return PageResponse.<AlertDto>builder()
                .content(dtos)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    public long getUnreadCount(Long userId) {
        return alertRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public AlertDto markAsRead(Long alertId, Long userId) {
        Alert a = alertRepository.findById(alertId).orElseThrow(() -> new ResourceNotFoundException("Alert", alertId));
        if (!a.getUser().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Not your alert");
        }
        a.setRead(true);
        return AlertDto.from(alertRepository.save(a));
    }
}
