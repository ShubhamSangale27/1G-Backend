package com.realestate.service;

import com.realestate.entity.OtpVerification;
import com.realestate.entity.User;
import com.realestate.exception.BadRequestException;
import com.realestate.repository.OtpVerificationRepository;
import com.realestate.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.IntStream;

@Service
@Slf4j
public class OtpService {
    public record OtpSendResult(
            String identifier,
            OtpVerification.OtpChannel channel,
            Instant resendAvailableAt,
            int resendAttemptsUsed,
            int resendAttemptsRemaining,
            Instant blockedUntil
    ) {}

    private final OtpVerificationRepository otpRepository;
    private final UserRepository userRepository;
    private JavaMailSender mailSender;

    public OtpService(OtpVerificationRepository otpRepository, UserRepository userRepository) {
        this.otpRepository = otpRepository;
        this.userRepository = userRepository;
    }

    @Autowired(required = false)
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${app.otp.length:6}")
    private int otpLength;

    @Value("${app.otp.expiry-minutes:10}")
    private int expiryMinutes;

    @Value("${app.mail-from:noreply@realestate.com}")
    private String mailFrom;

    @Value("${app.msg91.authkey:}")
    private String msg91Authkey;

    @Value("${app.msg91.sender:}")
    private String msg91Sender;

    @Value("${app.msg91.templateId:}")
    private String msg91templateId;

    /** When set (e.g. 123456), use this OTP for mobile verification and skip MSG91 – for testing without DLT. */
    @Value("${app.otp.test-otp:}")
    private String testOtp;

    /** When true (heroku/dev with Postgres), log OTP to console so it can be read from Heroku logs or local console during signup. */
    @Value("${app.otp.log-otp:false}")
    private boolean logOtp;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String MSG91_SEND_OTP_URL = "https://control.msg91.com/api/v5/otp";
    private static final int MAX_RESEND_ATTEMPTS_PER_DAY = 3;
    private static final int RESEND_BASE_WAIT_MINUTES = 5;

    @Transactional
    public OtpSendResult sendEmailOtp(String email) {
        OtpSendResult policy = validateAndBuildResendPolicy(email, OtpVerification.OtpChannel.EMAIL);
        String otp = generateOtp();
        OtpVerification ov = OtpVerification.builder()
                .identifier(email)
                .channel(OtpVerification.OtpChannel.EMAIL)
                .otpCode(otp)
                .expiresAt(Instant.now().plusSeconds(expiryMinutes * 60L))
                .verified(false)
                .build();
        otpRepository.save(ov);
        sendEmail(email, "Your verification OTP", "Your OTP is: " + otp + ". Valid for " + expiryMinutes + " minutes.");
        log.info("Email OTP sent to {}", email);
        return policy;
    }

    /** Send site-visit completion OTP via SMS (uses same MSG91 template as signup OTP). */
    public void sendVisitCompletionOtp(String mobile, String otpCode) {
        if (mobile == null || mobile.isBlank()) {
            log.warn("Cannot send visit OTP SMS — no mobile on file");
            return;
        }
        if (logOtp) {
            log.info("Site visit OTP for mobile {}: {} (check logs or use test OTP in dev)", mobile, otpCode);
        }
        if (testOtp != null && !testOtp.isBlank()) {
            log.info("Test OTP mode: site visit OTP for {} is {}", mobile, otpCode);
            return;
        }
        sendSmsViaMsg91(mobile, otpCode);
    }

    @Transactional
    public OtpSendResult sendMobileOtp(String mobile) {
        OtpSendResult policy = validateAndBuildResendPolicy(mobile, OtpVerification.OtpChannel.MOBILE);
        String otp = (testOtp != null && !testOtp.isBlank()) ? testOtp.trim() : generateOtp();
        OtpVerification ov = OtpVerification.builder()
                .identifier(mobile)
                .channel(OtpVerification.OtpChannel.MOBILE)
                .otpCode(otp)
                .expiresAt(Instant.now().plusSeconds(expiryMinutes * 60L))
                .verified(false)
                .build();
        otpRepository.save(ov);
        if (logOtp) {
            log.info("Signup OTP for mobile {}: {} (use this in verify step or check Heroku logs)", mobile, otp);
        }
        if (testOtp != null && !testOtp.isBlank()) {
            log.info("Test OTP mode: no SMS sent. Use OTP {} for mobile {}", otp, mobile);
        } else {
            sendSmsViaMsg91(mobile, otp);
            log.info("SMS OTP sent to {} via MSG91", mobile);
        }
        return policy;
    }

    @Transactional
    public boolean verifyEmailOtp(String email, String otp) {
        OtpVerification ov = otpRepository.findTopByIdentifierAndChannelOrderByCreatedAtDesc(email, OtpVerification.OtpChannel.EMAIL)
                .orElseThrow(() -> new BadRequestException("No OTP found for this email"));
        if (ov.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("OTP expired");
        }
        if (!ov.getOtpCode().equals(otp)) {
            throw new BadRequestException("Invalid OTP");
        }
        ov.setVerified(true);
        otpRepository.save(ov);
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setEmailVerified(true);
            userRepository.save(user);
        });
        return true;
    }

    /** Verify OTP without updating user verification flags (e.g. password reset). */
    @Transactional
    public void verifyOtpCode(String identifier, OtpVerification.OtpChannel channel, String otp) {
        OtpVerification ov = otpRepository.findTopByIdentifierAndChannelOrderByCreatedAtDesc(identifier, channel)
                .orElseThrow(() -> new BadRequestException("No OTP found"));
        if (ov.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("OTP expired");
        }
        if (!ov.getOtpCode().equals(otp)) {
            throw new BadRequestException("Invalid OTP");
        }
        ov.setVerified(true);
        otpRepository.save(ov);
    }

    @Transactional
    public boolean verifyMobileOtp(String mobile, String otp) {
        OtpVerification ov = otpRepository.findTopByIdentifierAndChannelOrderByCreatedAtDesc(mobile, OtpVerification.OtpChannel.MOBILE)
                .orElseThrow(() -> new BadRequestException("No OTP found for this mobile"));
        if (ov.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("OTP expired");
        }
        if (!ov.getOtpCode().equals(otp)) {
            throw new BadRequestException("Invalid OTP");
        }
        ov.setVerified(true);
        otpRepository.save(ov);
        userRepository.findByMobile(mobile).ifPresent(user -> {
            user.setMobileVerified(true);
            userRepository.save(user);
        });
        return true;
    }

    private String generateOtp() {
        StringBuilder sb = new StringBuilder(otpLength);
        IntStream.range(0, otpLength).forEach(i -> sb.append(RANDOM.nextInt(10)));
        return sb.toString();
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            msg.setFrom(mailFrom);
            if (mailSender == null) {
                System.out.println("Mail sender not available in local mode.");
                return;
            }else{
            mailSender.send(msg);
            }
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Send OTP SMS via MSG91. Mobile must be E.164 (e.g. 919876543210 for India).
     * If app.msg91.authkey is not set, only logs (no API call).
     */
    private void sendSmsViaMsg91(String mobile, String otp) {
        if (msg91Authkey == null || msg91Authkey.isBlank()) {
            log.warn("MSG91 authkey not configured. Set MSG91_AUTHKEY or app.msg91.authkey. OTP for {} would be: {}", mobile, otp);
            return;
        }
        if (msg91templateId == null || msg91templateId.isBlank()) {
            log.warn("MSG91 templateId not configured. Set MSG91_TEMPLATE_ID or app.msg91.templateId. OTP for {} would be: {}", mobile, otp);
            return;
        }
        String mobileE164 = normalizeMobileE164(mobile);
        try {
            StringBuilder url = new StringBuilder(MSG91_SEND_OTP_URL)
                    .append("?authkey=").append(java.net.URLEncoder.encode(msg91Authkey, java.nio.charset.StandardCharsets.UTF_8))
                    .append("&mobile=").append(mobileE164)
                    .append("&template_id=").append(java.net.URLEncoder.encode(msg91templateId, java.nio.charset.StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("OTP", otp), headers);

            RestTemplate rest = new RestTemplate();
            ResponseEntity<String> response = rest.exchange(
                    URI.create(url.toString()),
                    HttpMethod.POST,
                    request,
                    String.class
            );
            int status = response.getStatusCode().value();
            String responseBody = response.getBody();
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("MSG91 send OTP success for {}. status={}, response={}", mobile, status, responseBody);
            } else {
                log.error("MSG91 send OTP non-success for {}. status={}, response={}", mobile, status, responseBody);
            }
        } catch (HttpStatusCodeException e) {
            log.error("MSG91 send OTP HTTP error for {}. status={}, response={}",
                    mobile, e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("MSG91 send OTP failed for {}: {}", mobile, e.getMessage());
        }
    }

    private static String normalizeMobileE164(String mobile) {
        String digits = mobile.replaceAll("\\D", "");
        if (digits.length() == 10 && !mobile.startsWith("+")) {
            return "91" + digits;
        }
        if (mobile.startsWith("+")) {
            return digits.isEmpty() ? mobile : digits;
        }
        return digits.isEmpty() ? mobile : digits;
    }

    private OtpSendResult validateAndBuildResendPolicy(String identifier, OtpVerification.OtpChannel channel) {
        Instant now = Instant.now();
        Instant lookbackStart = now.minus(Duration.ofHours(24));
        long sendsInLast24Hours = otpRepository.countByIdentifierAndChannelAndCreatedAtAfter(identifier, channel, lookbackStart);

        if (sendsInLast24Hours >= MAX_RESEND_ATTEMPTS_PER_DAY + 1L) {
            Instant blockedUntil = otpRepository
                    .findFirstByIdentifierAndChannelAndCreatedAtAfterOrderByCreatedAtAsc(identifier, channel, lookbackStart)
                    .map(first -> first.getCreatedAt().plus(Duration.ofHours(24)))
                    .orElse(now.plus(Duration.ofHours(24)));
            long mins = Math.max(1, Duration.between(now, blockedUntil).toMinutes());
            throw new BadRequestException("OTP resend limit reached. Please try again after " + mins + " minutes.");
        }

        if (sendsInLast24Hours > 0) {
            OtpVerification last = otpRepository.findTopByIdentifierAndChannelOrderByCreatedAtDesc(identifier, channel).orElse(null);
            if (last != null && last.getCreatedAt() != null) {
                long cooldownMinutes = sendsInLast24Hours * RESEND_BASE_WAIT_MINUTES;
                Instant nextAllowedAt = last.getCreatedAt().plus(Duration.ofMinutes(cooldownMinutes));
                if (nextAllowedAt.isAfter(now)) {
                    long mins = Math.max(1, Duration.between(now, nextAllowedAt).toMinutes());
                    throw new BadRequestException("Please wait " + mins + " minutes before requesting OTP again.");
                }
            }
        }

        int usedAfterThisSend = (int) sendsInLast24Hours;
        int remaining = Math.max(0, MAX_RESEND_ATTEMPTS_PER_DAY - usedAfterThisSend);
        Instant resendAvailableAt = now.plus(Duration.ofMinutes((sendsInLast24Hours + 1) * RESEND_BASE_WAIT_MINUTES));
        return new OtpSendResult(identifier, channel, resendAvailableAt, usedAfterThisSend, remaining, null);
    }
}
