package com.realestate.service;

import com.realestate.dto.*;
import com.realestate.entity.EmailVerificationToken;
import com.realestate.entity.OtpVerification;
import com.realestate.entity.PendingSignup;
import com.realestate.entity.RefreshToken;
import com.realestate.entity.User;
import com.realestate.exception.BadRequestException;
import com.realestate.exception.ResourceNotFoundException;
import com.realestate.repository.EmailVerificationTokenRepository;
import com.realestate.repository.PendingSignupRepository;
import com.realestate.repository.RefreshTokenRepository;
import com.realestate.repository.UserRepository;
import com.realestate.security.JwtProperties;
import com.realestate.security.JwtUtils;
import com.realestate.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PendingSignupRepository pendingSignupRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;
    @Value("${app.mail-from:noreply@realestate.com}")
    private String mailFrom;

    private static final int PENDING_SIGNUP_EXPIRY_MINUTES = 15;
    private static final int EMAIL_VERIFICATION_LINK_EXPIRY_HOURS = 24;
    private static final int MAX_RESEND_ATTEMPTS_PER_DAY = 3;

    public AuthService(UserRepository userRepository, PendingSignupRepository pendingSignupRepository,
                       RefreshTokenRepository refreshTokenRepository, EmailVerificationTokenRepository emailVerificationTokenRepository,
                       PasswordEncoder passwordEncoder, JwtUtils jwtUtils, JwtProperties jwtProperties,
                       AuthenticationManager authenticationManager, OtpService otpService) {
        this.userRepository = userRepository;
        this.pendingSignupRepository = pendingSignupRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.jwtProperties = jwtProperties;
        this.authenticationManager = authenticationManager;
        this.otpService = otpService;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }
        if (userRepository.existsByMobile(request.getMobile())) {
            throw new BadRequestException("Mobile number already registered");
        }
        java.time.Instant expiresAt = java.time.Instant.now().plusSeconds(PENDING_SIGNUP_EXPIRY_MINUTES * 60L);
        PendingSignup pending = pendingSignupRepository.findByEmail(request.getEmail())
                .map(existing -> {
                    existing.setMobile(request.getMobile());
                    existing.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                    existing.setFullName(request.getFullName());
                    existing.setExpiresAt(expiresAt);
                    return existing;
                })
                .orElseGet(() -> PendingSignup.builder()
                        .email(request.getEmail())
                        .mobile(request.getMobile())
                        .passwordHash(passwordEncoder.encode(request.getPassword()))
                        .fullName(request.getFullName())
                        .expiresAt(expiresAt)
                        .build());
        pending = pendingSignupRepository.save(pending);
        OtpService.OtpSendResult otpSendResult = otpService.sendMobileOtp(pending.getMobile());
        return SignupResponse.builder()
                .message("OTP sent to your mobile. Enter it on the next screen to complete registration.")
                .email(pending.getEmail())
                .mobile(pending.getMobile())
                .resendAttemptsUsed(otpSendResult.resendAttemptsUsed())
                .resendAttemptsRemaining(otpSendResult.resendAttemptsRemaining())
                .resendAvailableAt(otpSendResult.resendAvailableAt())
                .maxResendAttemptsPerDay(MAX_RESEND_ATTEMPTS_PER_DAY)
                .build();
    }

    @Transactional
    public SignupResponse resendSignupOtp(ResendSignupOtpRequest request) {
        PendingSignup pending = pendingSignupRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("No pending signup found. Please sign up again."));
        if (pending.getExpiresAt().isBefore(Instant.now())) {
            pendingSignupRepository.delete(pending);
            throw new BadRequestException("Verification window expired. Please sign up again.");
        }
        if (!pending.getMobile().equals(request.getMobile())) {
            throw new BadRequestException("Mobile does not match pending signup.");
        }
        OtpService.OtpSendResult otpSendResult = otpService.sendMobileOtp(request.getMobile());
        return SignupResponse.builder()
                .message("OTP resent successfully.")
                .email(request.getEmail())
                .mobile(request.getMobile())
                .resendAttemptsUsed(otpSendResult.resendAttemptsUsed())
                .resendAttemptsRemaining(otpSendResult.resendAttemptsRemaining())
                .resendAvailableAt(otpSendResult.resendAvailableAt())
                .maxResendAttemptsPerDay(MAX_RESEND_ATTEMPTS_PER_DAY)
                .build();
    }

    @Transactional
    public AuthResponse verifySignup(VerifySignupRequest request) {
        if (request.getMobileOtp() == null || request.getMobileOtp().isBlank()) {
            throw new BadRequestException("Enter the 6-digit OTP sent to your mobile.");
        }
        otpService.verifyMobileOtp(request.getMobile(), request.getMobileOtp());
        PendingSignup pending = pendingSignupRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("No pending signup found. Please sign up again."));
        if (pending.getExpiresAt().isBefore(Instant.now())) {
            pendingSignupRepository.delete(pending);
            throw new BadRequestException("Verification window expired. Please sign up again.");
        }
        if (!pending.getMobile().equals(request.getMobile())) {
            throw new BadRequestException("Mobile does not match pending signup.");
        }
        User user = User.builder()
                .email(pending.getEmail())
                .passwordHash(pending.getPasswordHash())
                .fullName(pending.getFullName())
                .mobile(pending.getMobile())
                .role(User.Role.USER)
                .emailVerified(false)
                .mobileVerified(true)
                .active(true)
                .build();
        user = userRepository.save(user);
        pendingSignupRepository.delete(pending);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));
        if (!user.isActive()) {
            throw new BadRequestException("Suspended user: your account has been deactivated. Please contact admin.");
        }
        if (!user.isMobileVerified()) {
            throw new BadRequestException("Please verify your mobile number before logging in");
        }
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenValue) {
        RefreshToken rt = refreshTokenRepository.findByTokenHash(hashToken(refreshTokenValue))
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));
        if (rt.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(rt);
            throw new BadRequestException("Refresh token expired");
        }
        User user = rt.getUser();
        refreshTokenRepository.delete(rt);
        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(UserPrincipal principal) {
        if (principal != null) {
            refreshTokenRepository.deleteByUserId(principal.getId());
        }
    }

    public AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtils.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name());
        String refreshToken = jwtUtils.generateRefreshToken(user.getEmail(), user.getId());
        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(refreshToken))
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTtlMs()))
                .build();
        refreshTokenRepository.save(rt);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getAccessTtlMs() / 1000)
                .user(UserDto.from(user))
                .build();
    }

    @Transactional
    public void verifyAndActivateUser(String email, String mobileOtp) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        otpService.verifyMobileOtp(user.getMobile(), mobileOtp);
        user.setMobileVerified(true);
        userRepository.save(user);
    }

    /** Request password reset OTP sent to the user's registered mobile. */
    @Transactional
    public PasswordOtpResponse forgotPassword(ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail().trim().toLowerCase());
        if (userOpt.isEmpty()) {
            return PasswordOtpResponse.builder()
                    .message("If an account exists for this email, an OTP has been sent to the registered mobile number.")
                    .build();
        }
        User user = userOpt.get();
        if (user.getMobile() == null || user.getMobile().isBlank()) {
            throw new BadRequestException("No mobile number on file. Please contact support.");
        }
        OtpService.OtpSendResult result = otpService.sendMobileOtp(user.getMobile());
        return PasswordOtpResponse.builder()
                .message("OTP sent to your registered mobile number ending in " + maskMobile(user.getMobile()) + ".")
                .maskedMobile(maskMobile(user.getMobile()))
                .resendAvailableAt(result.resendAvailableAt())
                .resendAttemptsUsed(result.resendAttemptsUsed())
                .resendAttemptsRemaining(result.resendAttemptsRemaining())
                .maxResendAttemptsPerDay(MAX_RESEND_ATTEMPTS_PER_DAY)
                .build();
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new BadRequestException("Invalid email or OTP"));
        if (user.getMobile() == null || user.getMobile().isBlank()) {
            throw new BadRequestException("Cannot reset password for this account");
        }
        otpService.verifyOtpCode(user.getMobile(), OtpVerification.OtpChannel.MOBILE, request.getOtp());
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenRepository.deleteByUserId(user.getId());
    }

    /** Send OTP to authenticated user's mobile for password change. */
    @Transactional
    public PasswordOtpResponse sendChangePasswordOtp(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getMobile() == null || user.getMobile().isBlank()) {
            throw new BadRequestException("No mobile number on file. Please contact support.");
        }
        OtpService.OtpSendResult result = otpService.sendMobileOtp(user.getMobile());
        return PasswordOtpResponse.builder()
                .message("OTP sent to your mobile ending in " + maskMobile(user.getMobile()) + ".")
                .maskedMobile(maskMobile(user.getMobile()))
                .resendAvailableAt(result.resendAvailableAt())
                .resendAttemptsUsed(result.resendAttemptsUsed())
                .resendAttemptsRemaining(result.resendAttemptsRemaining())
                .maxResendAttemptsPerDay(MAX_RESEND_ATTEMPTS_PER_DAY)
                .build();
    }

    @Transactional
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getMobile() == null || user.getMobile().isBlank()) {
            throw new BadRequestException("No mobile number on file");
        }
        otpService.verifyOtpCode(user.getMobile(), OtpVerification.OtpChannel.MOBILE, request.getOtp());
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenRepository.deleteByUserId(user.getId());
    }

    /** Send email verification link to the current user's email. User can verify later via the link. */
    @Transactional
    public void sendEmailVerificationLink(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.isEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }
        emailVerificationTokenRepository.deleteByUser_Id(user.getId());
        String token = UUID.randomUUID().toString().replace("-", "");
        EmailVerificationToken evt = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .expiresAt(Instant.now().plus(EMAIL_VERIFICATION_LINK_EXPIRY_HOURS, ChronoUnit.HOURS))
                .build();
        emailVerificationTokenRepository.save(evt);
        String link = frontendUrl.replaceAll("/$", "") + "/verify-email?token=" + token;
        String subject = "Verify your email - 1Guntha";
        String body = "Click the link below to verify your email:\n\n" + link + "\n\nThis link expires in " + EMAIL_VERIFICATION_LINK_EXPIRY_HOURS + " hours.";
        sendEmail(user.getEmail(), subject, body);
        log.info("Email verification link sent to {}", user.getEmail());
    }

    /** Verify email by token (from link in email). */
    @Transactional
    public void verifyEmailByToken(String token) {
        EmailVerificationToken evt = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification link"));
        if (evt.getExpiresAt().isBefore(Instant.now())) {
            emailVerificationTokenRepository.delete(evt);
            throw new BadRequestException("Verification link has expired");
        }
        User user = evt.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        emailVerificationTokenRepository.delete(evt);
        log.info("Email verified for user {}", user.getEmail());
    }

    private void sendEmail(String to, String subject, String body) {
        if (mailSender == null) {
            int httpStart = body != null ? body.indexOf("http") : -1;
            String linkSnippet = (httpStart >= 0 && httpStart < body.length()) ? body.substring(httpStart).split("\n")[0] : "(configure spring.mail to send)";
            log.warn("Mail sender not configured. Verification link: {}", linkSnippet);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            msg.setFrom(mailFrom);
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "****";
        return mobile.substring(mobile.length() - 4);
    }

    private static String normalizePhone(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("\\s", "").replaceAll("^\\+", "");
    }

    private String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
