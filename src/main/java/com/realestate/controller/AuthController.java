package com.realestate.controller;

import com.realestate.dto.*;
import com.realestate.security.UserPrincipal;
import com.realestate.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "Sign up (sends OTP; user is created only after OTP verification)")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

    @PostMapping("/verify-signup")
    @Operation(summary = "Verify OTP and complete signup (creates user and returns tokens)")
    public ResponseEntity<AuthResponse> verifySignup(@Valid @RequestBody VerifySignupRequest request) {
        return ResponseEntity.ok(authService.verifySignup(request));
    }

    @PostMapping("/resend-signup-otp")
    @Operation(summary = "Resend signup OTP with throttling limits")
    public ResponseEntity<SignupResponse> resendSignupOtp(@Valid @RequestBody ResendSignupOtpRequest request) {
        return ResponseEntity.ok(authService.resendSignupOtp(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/login")
    @Operation(summary = "Login endpoint info (POST required)")
    public ResponseEntity<Map<String, String>> loginGet() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Map.of("error", "Method Not Allowed", "message", "Use POST with JSON body: { \"email\", \"password\" }"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token")
    public ResponseEntity<AuthResponse> refresh(@RequestBody MapWrapper body) {
        String refreshToken = body.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send OTP to registered mobile for password reset")
    public ResponseEntity<PasswordOtpResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with OTP sent to registered mobile")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully. You can now log in."));
    }

    @PostMapping("/change-password/send-otp")
    @Operation(summary = "Send OTP to change password (authenticated)")
    public ResponseEntity<PasswordOtpResponse> sendChangePasswordOtp(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(authService.sendChangePasswordOtp(principal));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password with OTP (authenticated)")
    public ResponseEntity<Map<String, String>> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                                               @Valid @RequestBody ChangePasswordRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        authService.changePassword(principal, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully. Please log in again."));
    }

    @PostMapping("/send-email-verification")
    @Operation(summary = "Send email verification link to current user's email (authenticated)")
    public ResponseEntity<Void> sendEmailVerification(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        authService.sendEmailVerificationLink(principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify email by token (from link in email)")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        authService.verifyEmailByToken(token);
        return ResponseEntity.ok(java.util.Map.of("message", "Email verified successfully"));
    }

    @lombok.Data
    public static class MapWrapper {
        private String refreshToken;
    }
}
