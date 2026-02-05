package com.realestate.service;

import com.realestate.entity.OtpVerification;
import com.realestate.entity.User;
import com.realestate.exception.BadRequestException;
import com.realestate.repository.OtpVerificationRepository;
import com.realestate.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.stream.IntStream;

@Service
@Slf4j
public class OtpService {

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

    /** When set (e.g. 123456), use this OTP for mobile verification and skip MSG91 – for testing without DLT. */
    @Value("${app.otp.test-otp:}")
    private String testOtp;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String MSG91_SEND_OTP_URL = "https://api.msg91.com/api/sendotp.php";

    @Transactional
    public void sendEmailOtp(String email) {
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
    }

    @Transactional
    public void sendMobileOtp(String mobile) {
        String otp = (testOtp != null && !testOtp.isBlank()) ? testOtp.trim() : generateOtp();
        OtpVerification ov = OtpVerification.builder()
                .identifier(mobile)
                .channel(OtpVerification.OtpChannel.MOBILE)
                .otpCode(otp)
                .expiresAt(Instant.now().plusSeconds(expiryMinutes * 60L))
                .verified(false)
                .build();
        otpRepository.save(ov);
        if (testOtp != null && !testOtp.isBlank()) {
            log.info("Test OTP mode: no SMS sent. Use OTP {} for mobile {}", otp, mobile);
        } else {
            sendSmsViaMsg91(mobile, otp);
            log.info("SMS OTP sent to {} via MSG91", mobile);
        }
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
        String mobileE164 = normalizeMobileE164(mobile);
        try {
            StringBuilder url = new StringBuilder(MSG91_SEND_OTP_URL)
                    .append("?authkey=").append(java.net.URLEncoder.encode(msg91Authkey, java.nio.charset.StandardCharsets.UTF_8))
                    .append("&mobile=").append(mobileE164)
                    .append("&otp=").append(otp)
                    .append("&otp_expiry=").append(expiryMinutes)
                    .append("&otp_length=").append(otpLength);
            if (msg91Sender != null && !msg91Sender.isBlank()) {
                url.append("&sender=").append(java.net.URLEncoder.encode(msg91Sender, java.nio.charset.StandardCharsets.UTF_8));
            }
            RestTemplate rest = new RestTemplate();
            String response = rest.getForObject(URI.create(url.toString()), String.class);
            log.debug("MSG91 send OTP response: {}", response);
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
}
