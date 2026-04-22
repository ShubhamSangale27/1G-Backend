package com.realestate.controller;

import com.realestate.dto.OtpRequest;
import com.realestate.dto.OtpVerifyRequest;
import com.realestate.entity.OtpVerification;
import com.realestate.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = {"http://www.1guntha.com", "https://www.1guntha.com"})
@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
@Tag(name = "OTP", description = "Email/Mobile OTP APIs")
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/send")
    @Operation(summary = "Send OTP")
    public ResponseEntity<Map<String, String>> sendOtp(@Valid @RequestBody OtpRequest request) {
        if (request.getChannel() == OtpVerification.OtpChannel.EMAIL) {
            otpService.sendEmailOtp(request.getIdentifier());
        } else {
            otpService.sendMobileOtp(request.getIdentifier());
        }
        return ResponseEntity.ok(Map.of("message", "OTP sent"));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify OTP")
    public ResponseEntity<Map<String, Boolean>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        boolean valid = false;
        if (request.getChannel() == OtpVerification.OtpChannel.EMAIL) {
            valid = otpService.verifyEmailOtp(request.getIdentifier(), request.getOtpCode());
        } else {
            valid = otpService.verifyMobileOtp(request.getIdentifier(), request.getOtpCode());
        }
        return ResponseEntity.ok(Map.of("verified", valid));
    }
}
