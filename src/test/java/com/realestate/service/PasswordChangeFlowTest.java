package com.realestate.service;

import com.realestate.dto.ChangePasswordRequest;
import com.realestate.entity.OtpVerification;
import com.realestate.entity.User;
import com.realestate.repository.OtpVerificationRepository;
import com.realestate.repository.UserRepository;
import com.realestate.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PasswordChangeFlowTest {

    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private OtpVerificationRepository otpRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("pwdtest@example.com")
                .passwordHash(passwordEncoder.encode("oldPass123"))
                .fullName("Pwd Test")
                .mobile("9999999901")
                .role(User.Role.USER)
                .mobileVerified(true)
                .emailVerified(true)
                .active(true)
                .build());

        otpRepository.save(OtpVerification.builder()
                .identifier(user.getMobile())
                .channel(OtpVerification.OtpChannel.MOBILE)
                .otpCode("654321")
                .expiresAt(Instant.now().plusSeconds(600))
                .verified(false)
                .build());
    }

    @Test
    void changePassword_updatesHashAndAllowsNewLoginPassword() {
        UserPrincipal principal = new UserPrincipal(user.getId(), user.getEmail(), user.getRole().name());
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOtp("654321");
        req.setNewPassword("newPass456");

        authService.changePassword(principal, req);

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("newPass456", reloaded.getPasswordHash()));
        assertFalse(passwordEncoder.matches("oldPass123", reloaded.getPasswordHash()));
    }
}
