package com.realestate.service;

import com.realestate.dto.AdminSiteVisitsResponse;
import com.realestate.dto.AgentAssignedVisitsResponse;
import com.realestate.dto.PageResponse;
import com.realestate.dto.SiteVisitCommentDto;
import com.realestate.dto.SiteVisitDetailDto;
import com.realestate.dto.SiteVisitDto;
import com.realestate.dto.SiteVisitRequest;
import com.realestate.dto.VisitOtpResponse;
import com.realestate.entity.*;
import com.realestate.exception.BadRequestException;
import com.realestate.exception.ResourceNotFoundException;
import com.realestate.entity.SiteVisitComment;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.SiteVisitCommentRepository;
import com.realestate.repository.SiteVisitRepository;
import com.realestate.repository.VisitOTPRepository;
import com.realestate.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class SiteVisitService {

    private final SiteVisitRepository siteVisitRepository;
    private final SiteVisitCommentRepository siteVisitCommentRepository;
    private final VisitOTPRepository visitOTPRepository;
    private final PropertyRepository propertyRepository;
    private final UserService userService;
    private final AlertService alertService;
    private final OtpService otpService;

    @Value("${app.otp.length:6}")
    private int otpLength;

    private static final int VISIT_OTP_EXPIRY_MINUTES = 60 * 24;
    private static final int VISIT_MAX_RESEND_ATTEMPTS_PER_DAY = 3;
    private static final int VISIT_RESEND_BASE_WAIT_MINUTES = 5;

    @Transactional
    public SiteVisitDto book(SiteVisitRequest request, UserPrincipal principal) {
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property", request.getPropertyId()));
        User user = userService.getById(principal.getId());
        List<SiteVisit.SiteVisitStatus> active = List.of(SiteVisit.SiteVisitStatus.PENDING_ASSIGNMENT, SiteVisit.SiteVisitStatus.ASSIGNED);
        if (siteVisitRepository.findFirstByUserIdAndPropertyIdAndStatusInOrderByCreatedAtDesc(principal.getId(), request.getPropertyId(), active).isPresent()) {
            throw new BadRequestException("You already have a site visit request for this property that is not closed yet.");
        }
        SiteVisit sv = SiteVisit.builder()
                .user(user)
                .property(property)
                .scheduledAt(request.getScheduledAt())
                .userNotes(request.getUserNotes())
                .status(SiteVisit.SiteVisitStatus.PENDING_ASSIGNMENT)
                .build();
        sv = siteVisitRepository.save(sv);
        alertService.create(principal.getId(), "Site Visit Booked",
                "Your visit request is submitted. An admin will assign an agent — you'll receive an OTP via SMS when assigned.",
                "SITE_VISIT", sv.getId());
        return SiteVisitDto.from(siteVisitRepository.findById(sv.getId()).orElseThrow());
    }

    public PageResponse<SiteVisitDto> getMyVisits(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SiteVisit> result = siteVisitRepository.findByUserId(userId, pageable);
        List<SiteVisitDto> dtos = result.getContent().stream()
                .map(SiteVisitDto::from)
                .collect(Collectors.toList());
        return PageResponse.<SiteVisitDto>builder()
                .content(dtos)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    public java.util.Optional<SiteVisitDto> getMyVisitForProperty(Long userId, Long propertyId) {
        List<SiteVisit.SiteVisitStatus> active = List.of(SiteVisit.SiteVisitStatus.PENDING_ASSIGNMENT, SiteVisit.SiteVisitStatus.ASSIGNED);
        return siteVisitRepository.findFirstByUserIdAndPropertyIdAndStatusInOrderByCreatedAtDesc(userId, propertyId, active)
                .map(SiteVisitDto::from);
    }

    @Transactional
    public SiteVisitDto assignAgent(Long siteVisitId, Long agentId, UserPrincipal admin) {
        SiteVisit sv = siteVisitRepository.findById(siteVisitId).orElseThrow(() -> new ResourceNotFoundException("SiteVisit", siteVisitId));
        if (sv.getStatus() != SiteVisit.SiteVisitStatus.PENDING_ASSIGNMENT) {
            throw new BadRequestException("Visit is not pending assignment");
        }
        return doAssignOrReassign(sv, agentId);
    }

    @Transactional
    public SiteVisitDto reassignAgent(Long siteVisitId, Long agentId, UserPrincipal admin) {
        SiteVisit sv = siteVisitRepository.findById(siteVisitId).orElseThrow(() -> new ResourceNotFoundException("SiteVisit", siteVisitId));
        if (sv.getStatus() != SiteVisit.SiteVisitStatus.ASSIGNED && sv.getStatus() != SiteVisit.SiteVisitStatus.PENDING_ASSIGNMENT) {
            throw new BadRequestException("Only ASSIGNED or PENDING visits can be reassigned");
        }
        return doAssignOrReassign(sv, agentId);
    }

    private SiteVisitDto doAssignOrReassign(SiteVisit sv, Long agentId) {
        User agent = userService.getById(agentId);
        if (agent.getRole() != User.Role.AGENT && agent.getRole() != User.Role.ADMIN) {
            throw new BadRequestException("User is not an agent");
        }
        sv.setAgent(agent);
        sv.setStatus(SiteVisit.SiteVisitStatus.ASSIGNED);
        siteVisitRepository.save(sv);

        String otpCode = generateOtp();
        Instant now = Instant.now();
        VisitOTP votp = visitOTPRepository.findBySiteVisitId(sv.getId())
                .map(existing -> applyVisitOtpResendPolicy(existing, otpCode, now))
                .orElseGet(() -> VisitOTP.builder()
                        .siteVisit(sv)
                        .otpCode(otpCode)
                        .expiresAt(now.plusSeconds(VISIT_OTP_EXPIRY_MINUTES * 60L))
                        .used(false)
                        .resendCount(0)
                        .firstSentAt(now)
                        .lastSentAt(now)
                        .build());
        visitOTPRepository.save(votp);
        alertService.create(sv.getUser().getId(), "Agent Assigned",
                "Your visit has been assigned. Your completion OTP was sent to your registered mobile — share it with the agent only in person at the end of the visit.",
                "SITE_VISIT", sv.getId());
        alertService.create(agent.getId(), "Site Visit Assigned",
                "Assigned: " + sv.getProperty().getTitle() + " on " + sv.getScheduledAt()
                        + ". Ask the customer to share their visit OTP verbally when the visit is complete.",
                "SITE_VISIT", sv.getId());
        if (sv.getUser().getMobile() != null && !sv.getUser().getMobile().isBlank()) {
            otpService.sendVisitCompletionOtp(sv.getUser().getMobile(), otpCode);
        }
        return SiteVisitDto.from(siteVisitRepository.findById(sv.getId()).orElseThrow());
    }

    @Transactional
    public SiteVisitDto verifyAndComplete(Long siteVisitId, String otp, UserPrincipal agent) {
        SiteVisit sv = siteVisitRepository.findById(siteVisitId).orElseThrow(() -> new ResourceNotFoundException("SiteVisit", siteVisitId));
        if (sv.getStatus() != SiteVisit.SiteVisitStatus.ASSIGNED) {
            throw new BadRequestException("Visit is not in ASSIGNED state");
        }
        if (sv.getAgent() == null || !sv.getAgent().getId().equals(agent.getId())) {
            throw new BadRequestException("You are not assigned to this visit");
        }
        VisitOTP votp = visitOTPRepository.findBySiteVisitId(siteVisitId).orElseThrow(() -> new BadRequestException("No OTP found for this visit"));
        if (votp.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("OTP expired");
        }
        if (votp.isUsed()) {
            throw new BadRequestException("OTP already used");
        }
        if (!votp.getOtpCode().equals(otp)) {
            throw new BadRequestException("Invalid OTP");
        }
        votp.setUsed(true);
        visitOTPRepository.save(votp);
        sv.setStatus(SiteVisit.SiteVisitStatus.COMPLETED);
        siteVisitRepository.save(sv);
        alertService.create(sv.getUser().getId(), "Site Visit Completed", "Your site visit has been marked completed.", "SITE_VISIT", sv.getId());
        return SiteVisitDto.from(sv);
    }

    public VisitOtpResponse getVisitOtpForUser(Long visitId, UserPrincipal principal) {
        SiteVisit sv = siteVisitRepository.findById(visitId).orElseThrow(() -> new ResourceNotFoundException("SiteVisit", visitId));
        if (!sv.getUser().getId().equals(principal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Only the customer who booked this visit can view the OTP");
        }
        if (sv.getStatus() != SiteVisit.SiteVisitStatus.ASSIGNED) {
            throw new BadRequestException("OTP is available only for assigned visits");
        }
        VisitOTP votp = visitOTPRepository.findBySiteVisitId(visitId)
                .orElseThrow(() -> new BadRequestException("OTP not generated yet. Please wait for agent assignment."));
        if (votp.isUsed()) {
            throw new BadRequestException("OTP already used");
        }
        if (votp.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("OTP expired. Use resend to get a new one.");
        }
        return VisitOtpResponse.builder()
                .otp(votp.getOtpCode())
                .expiresAt(votp.getExpiresAt())
                .message("Share this OTP with the agent at the end of your visit.")
                .build();
    }

    @Transactional
    public VisitOtpResponse resendVisitOtpForUser(Long visitId, UserPrincipal principal) {
        SiteVisit sv = siteVisitRepository.findById(visitId).orElseThrow(() -> new ResourceNotFoundException("SiteVisit", visitId));
        if (!sv.getUser().getId().equals(principal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Only the customer who booked this visit can resend the OTP");
        }
        if (sv.getStatus() != SiteVisit.SiteVisitStatus.ASSIGNED) {
            throw new BadRequestException("OTP can be resent only for assigned visits");
        }
        String otpCode = generateOtp();
        Instant now = Instant.now();
        VisitOTP votp = visitOTPRepository.findBySiteVisitId(visitId)
                .map(existing -> applyVisitOtpResendPolicy(existing, otpCode, now))
                .orElseThrow(() -> new BadRequestException("OTP not generated yet"));
        visitOTPRepository.save(votp);
        if (sv.getUser().getMobile() != null && !sv.getUser().getMobile().isBlank()) {
            otpService.sendVisitCompletionOtp(sv.getUser().getMobile(), otpCode);
        }
        alertService.create(sv.getUser().getId(), "Visit OTP Resent",
                "A new completion OTP was sent to your registered mobile. Share it with the agent only in person when the visit is complete.",
                "SITE_VISIT", sv.getId());
        return VisitOtpResponse.builder()
                .otp(otpCode)
                .expiresAt(votp.getExpiresAt())
                .message("OTP resent to your registered mobile and shown here.")
                .build();
    }

    @Transactional
    public SiteVisitDto reschedule(Long siteVisitId, Instant newScheduledAt, UserPrincipal principal) {
        SiteVisit sv = siteVisitRepository.findById(siteVisitId).orElseThrow(() -> new ResourceNotFoundException("SiteVisit", siteVisitId));
        if (!sv.getUser().getId().equals(principal.getId())) {
            throw new BadRequestException("You can only reschedule your own visit");
        }
        if (sv.getStatus() != SiteVisit.SiteVisitStatus.PENDING_ASSIGNMENT && sv.getStatus() != SiteVisit.SiteVisitStatus.ASSIGNED) {
            throw new BadRequestException("Only pending or assigned visits can be rescheduled");
        }
        sv.setScheduledAt(newScheduledAt);
        siteVisitRepository.save(sv);
        return SiteVisitDto.from(sv);
    }

    public List<SiteVisitDto> getPendingAssignment(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "scheduledAt"));
        List<SiteVisit> list = siteVisitRepository.findByStatus(SiteVisit.SiteVisitStatus.PENDING_ASSIGNMENT, pageable);
        return list.stream().map(SiteVisitDto::from).collect(Collectors.toList());
    }

    public PageResponse<SiteVisitDto> getAssignedToAgent(Long agentId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scheduledAt"));
        Page<SiteVisit> result = siteVisitRepository.findByAgentId(agentId, pageable);
        return PageResponse.<SiteVisitDto>builder()
                .content(result.getContent().stream().map(SiteVisitDto::from).collect(Collectors.toList()))
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    public AdminSiteVisitsResponse getAllForAdmin(int page, int size, Instant from, Instant to, Long agentId) {
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime startOfToday = LocalDate.now(zone).atStartOfDay(zone);
        ZonedDateTime endOfToday = startOfToday.plusDays(1);
        Instant startToday = startOfToday.toInstant();
        Instant endToday = endOfToday.toInstant();
        long dueTodayCount = siteVisitRepository.countDueBetween(startToday, endToday);
        Pageable pageable = PageRequest.of(page, size);
        Long agentIdParam = (agentId != null) ? agentId : -1L; // -1 = "all agents" for PostgreSQL type inference
        Page<SiteVisit> result = siteVisitRepository.findAllForAdmin(from, to, agentIdParam, startToday, endToday, pageable);
        List<SiteVisitDto> content = result.getContent().stream().map(SiteVisitDto::from).collect(Collectors.toList());
        return AdminSiteVisitsResponse.builder()
                .content(content)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .dueTodayCount(dueTodayCount)
                .build();
    }

    public AgentAssignedVisitsResponse getAssignedVisitsWithDueToday(Long agentId, int page, int size) {
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime startOfToday = LocalDate.now(zone).atStartOfDay(zone);
        ZonedDateTime endOfToday = startOfToday.plusDays(1);
        Instant startToday = startOfToday.toInstant();
        Instant endToday = endOfToday.toInstant();
        long dueTodayCount = siteVisitRepository.countDueTodayForAgent(agentId, startToday, endToday);
        Pageable pageable = PageRequest.of(page, size);
        Instant now = Instant.now();
        Page<SiteVisit> result = siteVisitRepository.findByAgentIdUpcomingFirst(agentId, now, pageable);
        List<SiteVisitDto> content = result.getContent().stream().map(SiteVisitDto::from).collect(Collectors.toList());
        return AgentAssignedVisitsResponse.builder()
                .content(content)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .dueTodayCount(dueTodayCount)
                .build();
    }

    public SiteVisitDetailDto getDetailForAgent(Long visitId, UserPrincipal principal) {
        SiteVisit sv = siteVisitRepository.findById(visitId).orElseThrow(() -> new ResourceNotFoundException("SiteVisit", visitId));
        if (sv.getAgent() == null || !sv.getAgent().getId().equals(principal.getId())) {
            if (!"ADMIN".equals(principal.getRole())) {
                throw new BadRequestException("You are not assigned to this visit");
            }
        }
        return buildDetailDto(sv);
    }

    @Transactional
    public SiteVisitCommentDto addComment(Long visitId, String commentText, UserPrincipal principal) {
        SiteVisit sv = siteVisitRepository.findById(visitId).orElseThrow(() -> new ResourceNotFoundException("SiteVisit", visitId));
        if (sv.getAgent() == null || !sv.getAgent().getId().equals(principal.getId())) {
            if (!"ADMIN".equals(principal.getRole()) && !sv.getUser().getId().equals(principal.getId())) {
                throw new BadRequestException("Not authorized to comment on this visit");
            }
        }
        User user = userService.getById(principal.getId());
        SiteVisitComment comment = SiteVisitComment.builder()
                .siteVisit(sv)
                .user(user)
                .commentText(commentText)
                .build();
        comment = siteVisitCommentRepository.save(comment);
        return SiteVisitCommentDto.from(comment);
    }

    private SiteVisitDetailDto buildDetailDto(SiteVisit sv) {
        com.realestate.entity.Property p = sv.getProperty();
        com.realestate.entity.User u = sv.getUser();
        String firstImageUrl = null;
        if (p.getImages() != null && !p.getImages().isEmpty()) {
            firstImageUrl = p.getImages().stream().sorted((a, b) -> Integer.compare(a.getDisplayOrder() != null ? a.getDisplayOrder() : 0, b.getDisplayOrder() != null ? b.getDisplayOrder() : 0))
                    .map(com.realestate.entity.PropertyImage::getImageUrl).findFirst().orElse(null);
        }
        SiteVisitDetailDto.PropertySummaryDto prop = SiteVisitDetailDto.PropertySummaryDto.builder()
                .id(p.getId())
                .title(p.getTitle())
                .address(p.getAddress())
                .city(p.getCity())
                .state(p.getState())
                .pincode(p.getPincode())
                .listingType(p.getListingType() != null ? p.getListingType().name() : null)
                .propertyType(p.getPropertyType() != null ? p.getPropertyType().name() : null)
                .price(p.getPrice())
                .bedrooms(p.getBedrooms())
                .bathrooms(p.getBathrooms())
                .areaSqft(p.getAreaSqft())
                .amenities(p.getAmenities())
                .firstImageUrl(firstImageUrl)
                .build();
        List<SiteVisitCommentDto> comments = siteVisitCommentRepository.findBySiteVisitIdOrderByCreatedAtAsc(sv.getId())
                .stream().map(SiteVisitCommentDto::from).collect(Collectors.toList());
        return SiteVisitDetailDto.builder()
                .id(sv.getId())
                .userId(u.getId())
                .userName(u.getFullName())
                .userEmail(u.getEmail())
                .userMobile(u.getMobile())
                .propertyId(p.getId())
                .property(prop)
                .agentId(sv.getAgent() != null ? sv.getAgent().getId() : null)
                .agentName(sv.getAgent() != null ? sv.getAgent().getFullName() : null)
                .scheduledAt(sv.getScheduledAt())
                .userNotes(sv.getUserNotes())
                .status(sv.getStatus())
                .createdAt(sv.getCreatedAt())
                .comments(comments)
                .build();
    }

    private String generateOtp() {
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(otpLength);
        IntStream.range(0, otpLength).forEach(i -> sb.append(r.nextInt(10)));
        return sb.toString();
    }

    private VisitOTP applyVisitOtpResendPolicy(VisitOTP existing, String newOtpCode, Instant now) {
        Instant firstSentAt = existing.getFirstSentAt() != null ? existing.getFirstSentAt() : existing.getCreatedAt();
        Instant lastSentAt = existing.getLastSentAt() != null ? existing.getLastSentAt() : existing.getCreatedAt();
        int resendCount = Math.max(0, existing.getResendCount());

        if (firstSentAt != null && firstSentAt.plus(Duration.ofHours(24)).isBefore(now)) {
            firstSentAt = now;
            lastSentAt = now;
            resendCount = 0;
        } else {
            long cooldownMins = (long) (resendCount + 1) * VISIT_RESEND_BASE_WAIT_MINUTES;
            Instant nextAllowedAt = lastSentAt.plus(Duration.ofMinutes(cooldownMins));
            if (nextAllowedAt.isAfter(now)) {
                long waitMins = Math.max(1, Duration.between(now, nextAllowedAt).toMinutes());
                throw new BadRequestException("Visit OTP can be resent after " + waitMins + " minutes.");
            }
            if (resendCount >= VISIT_MAX_RESEND_ATTEMPTS_PER_DAY) {
                Instant blockedUntil = firstSentAt.plus(Duration.ofHours(24));
                long waitMins = Math.max(1, Duration.between(now, blockedUntil).toMinutes());
                throw new BadRequestException("Visit OTP resend limit reached. Please try again after " + waitMins + " minutes.");
            }
            resendCount++;
            lastSentAt = now;
        }

        existing.setOtpCode(newOtpCode);
        existing.setExpiresAt(now.plusSeconds(VISIT_OTP_EXPIRY_MINUTES * 60L));
        existing.setUsed(false);
        existing.setFirstSentAt(firstSentAt);
        existing.setLastSentAt(lastSentAt);
        existing.setResendCount(resendCount);
        return existing;
    }
}
