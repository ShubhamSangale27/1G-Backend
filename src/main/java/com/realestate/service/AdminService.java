package com.realestate.service;

import com.realestate.dto.*;
import com.realestate.entity.Property;
import com.realestate.entity.SiteVisit;
import com.realestate.entity.User;
import com.realestate.exception.BadRequestException;
import com.realestate.exception.ResourceNotFoundException;
import com.realestate.security.UserPrincipal;
import com.realestate.repository.PropertyAnalyticsRepository;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.SiteVisitRepository;
import com.realestate.repository.UserRepository;
import com.realestate.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final PropertyRepository propertyRepository;
    private final PropertyAnalyticsRepository analyticsRepository;
    private final SiteVisitRepository siteVisitRepository;
    private final UserRepository userRepository;
    private final WatchlistRepository watchlistRepository;
    private final PaymentService paymentService;
    private final FaqService faqService;
    private final UserAccountDeletionService userAccountDeletionService;

    public List<PropertyDto> getPendingProperties(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<Property> result = propertyRepository.findByStatus(Property.PropertyStatus.PENDING_APPROVAL, pageable);
        return result.getContent().stream().map(PropertyDto::from).collect(Collectors.toList());
    }

    @Transactional
    public PropertyDto approveProperty(Long propertyId) {
        Property p = propertyRepository.findById(propertyId).orElseThrow(() -> new ResourceNotFoundException("Property", propertyId));
        p.setStatus(Property.PropertyStatus.APPROVED);
        return PropertyDto.from(propertyRepository.save(p));
    }

    @Transactional
    public PropertyDto rejectProperty(Long propertyId) {
        Property p = propertyRepository.findById(propertyId).orElseThrow(() -> new ResourceNotFoundException("Property", propertyId));
        p.setStatus(Property.PropertyStatus.REJECTED);
        return PropertyDto.from(propertyRepository.save(p));
    }

    public AnalyticsSummaryDto getPropertyAnalytics(Long propertyId) {
        Property p = propertyRepository.findById(propertyId).orElseThrow(() -> new ResourceNotFoundException("Property", propertyId));
        long views = analyticsRepository.countByPropertyIdAndType(propertyId, com.realestate.entity.PropertyAnalytics.AnalyticsType.VIEW);
        long visits = analyticsRepository.countByPropertyIdAndType(propertyId, com.realestate.entity.PropertyAnalytics.AnalyticsType.VISIT);
        long clicks = analyticsRepository.countByPropertyIdAndType(propertyId, com.realestate.entity.PropertyAnalytics.AnalyticsType.CLICK);
        List<Object[]> byType = analyticsRepository.countByPropertyIdGroupByType(propertyId);
        Map<String, Long> byTypeMap = new HashMap<>();
        for (Object[] row : byType) {
            byTypeMap.put(row[0].toString(), (Long) row[1]);
        }
        return AnalyticsSummaryDto.builder()
                .propertyId(propertyId)
                .viewCount(views)
                .visitCount(visits)
                .clickCount(clicks)
                .byType(byTypeMap)
                .build();
    }

    public List<Map<String, Object>> getPropertiesAnalytics(Instant from, Instant to) {
        List<Object[]> rows = analyticsRepository.aggregateByPropertyAndType(from, to);
        Map<Long, Map<String, Long>> byProperty = new HashMap<>();
        for (Object[] row : rows) {
            Long propId = (Long) row[0];
            String type = row[1].toString();
            Long count = (Long) row[2];
            byProperty.computeIfAbsent(propId, k -> new HashMap<>()).put(type, count);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Map<String, Long>> e : byProperty.entrySet()) {
            Map<String, Object> m = new HashMap<>();
            m.put("propertyId", e.getKey());
            m.put("analytics", e.getValue());
            result.add(m);
        }
        return result;
    }

    public Map<String, Object> getGlobalMetrics() {
        Instant now = Instant.now();
        Instant startOfMonth = now.minus(30, ChronoUnit.DAYS);
        long totalProperties = propertyRepository.count();
        long approvedProperties = propertyRepository.findByStatus(Property.PropertyStatus.APPROVED, Pageable.unpaged()).getTotalElements();
        long pendingProperties = propertyRepository.findByStatus(Property.PropertyStatus.PENDING_APPROVAL, Pageable.unpaged()).getTotalElements();
        long pendingVisits = siteVisitRepository.countByStatus(SiteVisit.SiteVisitStatus.PENDING_ASSIGNMENT);
        long completedVisits = siteVisitRepository.countByStatus(SiteVisit.SiteVisitStatus.COMPLETED);
        long totalViews = analyticsRepository.countByType(com.realestate.entity.PropertyAnalytics.AnalyticsType.VIEW);
        java.math.BigDecimal revenue = paymentService.getRevenueBetween(startOfMonth, now);
        Map<String, Object> m = new HashMap<>();
        m.put("totalProperties", totalProperties);
        m.put("approvedProperties", approvedProperties);
        m.put("pendingProperties", pendingProperties);
        m.put("pendingSiteVisits", pendingVisits);
        m.put("completedSiteVisits", completedVisits);
        m.put("totalViews", totalViews);
        m.put("revenueLast30Days", revenue);
        m.put("unmatchedFaqPending", faqService.countPendingUnmatched());
        return m;
    }

    public List<UserDto> getAgents() {
        List<User> agents = userRepository.findByRole(User.Role.AGENT);
        List<User> admins = userRepository.findByRole(User.Role.ADMIN);
        List<UserDto> result = agents.stream().map(UserDto::from).collect(Collectors.toList());
        admins.stream().map(UserDto::from).forEach(result::add);
        return result;
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(UserDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDto setUserActive(Long userId, boolean active, UserPrincipal principal) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (user.getId().equals(principal.getId()) && !active) {
            throw new BadRequestException("You cannot suspend your own admin account.");
        }
        user.setActive(active);
        return UserDto.from(userRepository.save(user));
    }

    @Transactional
    public UserDto setUserRole(Long userId, User.Role role, UserPrincipal principal) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (role != User.Role.ADMIN && role != User.Role.AGENT && role != User.Role.BLOG && role != User.Role.USER) {
            throw new BadRequestException("Role must be one of ADMIN, AGENT, BLOG, USER.");
        }
        if (user.getId().equals(principal.getId()) && role != User.Role.ADMIN) {
            throw new BadRequestException("You cannot change your own role from ADMIN.");
        }
        user.setRole(role);
        return UserDto.from(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId, UserPrincipal principal) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (user.getId().equals(principal.getId())) {
            throw new BadRequestException("You cannot delete your own admin account.");
        }
        userAccountDeletionService.ensureNotLastAdmin(user);
        userAccountDeletionService.deleteUser(user);
    }

    public com.realestate.dto.PageResponse<PropertyDto> getAllProperties(int page, int size, Boolean featuredOnly, Boolean newOnly) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Property> pageResult;
        if (Boolean.TRUE.equals(newOnly)) {
            Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
            pageResult = propertyRepository.findByCreatedAtAfterOrderByCreatedAtDesc(cutoff, pageable);
        } else if (Boolean.TRUE.equals(featuredOnly)) {
            pageResult = propertyRepository.findByFeatured(true, pageable);
        } else {
            pageResult = propertyRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        List<PropertyDto> dtos = pageResult.getContent().stream().map(PropertyDto::from).collect(Collectors.toList());
        return com.realestate.dto.PageResponse.<PropertyDto>builder()
                .content(dtos)
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .first(pageResult.isFirst())
                .last(pageResult.isLast())
                .build();
    }

    @Transactional
    public PropertyDto setFeatured(Long propertyId, boolean featured) {
        Property p = propertyRepository.findById(propertyId).orElseThrow(() -> new ResourceNotFoundException("Property", propertyId));
        p.setFeatured(featured);
        return PropertyDto.from(propertyRepository.save(p));
    }

    @Transactional(readOnly = true)
    public List<UserDto> getPropertyViewers(Long propertyId) {
        propertyRepository.findById(propertyId).orElseThrow(() -> new ResourceNotFoundException("Property", propertyId));
        List<com.realestate.entity.PropertyAnalytics> analytics = analyticsRepository.findByPropertyIdAndTypeOrderByRecordedAtDesc(
                propertyId, com.realestate.entity.PropertyAnalytics.AnalyticsType.VIEW, PageRequest.of(0, 500));
        Set<Long> userIds = new LinkedHashSet<>();
        for (com.realestate.entity.PropertyAnalytics a : analytics) {
            if (a.getUserId() != null) userIds.add(a.getUserId());
        }
        List<UserDto> result = new ArrayList<>();
        for (Long uid : userIds) {
            userRepository.findById(uid).ifPresent(u -> result.add(UserDto.from(u)));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<UserDto> getPropertyLikers(Long propertyId) {
        propertyRepository.findById(propertyId).orElseThrow(() -> new ResourceNotFoundException("Property", propertyId));
        List<com.realestate.entity.Watchlist> list = watchlistRepository.findByPropertyId(propertyId);
        return list.stream()
                .map(w -> w.getUser())
                .filter(Objects::nonNull)
                .map(UserDto::from)
                .collect(Collectors.toList());
    }
}
