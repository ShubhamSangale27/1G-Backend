package com.realestate.controller;

import com.realestate.dto.DeleteAccountRequest;
import com.realestate.dto.ProfileUpdateRequest;
import com.realestate.dto.UserDto;
import com.realestate.security.UserPrincipal;
import com.realestate.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile APIs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserDto> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.getProfile(principal.getId()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile (email and picture editable; mobile is read-only)")
    public ResponseEntity<UserDto> updateMyProfile(@Valid @RequestBody ProfileUpdateRequest request,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.updateProfile(principal.getId(), request, principal));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete the authenticated user's own account (password required)")
    public ResponseEntity<Void> deleteMyAccount(@Valid @RequestBody DeleteAccountRequest request,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        userService.deleteMyAccount(principal, request);
        return ResponseEntity.noContent().build();
    }
}
