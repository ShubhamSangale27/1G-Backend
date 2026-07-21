package com.realestate.service;

import com.realestate.dto.DeleteAccountRequest;
import com.realestate.dto.ProfileUpdateRequest;
import com.realestate.dto.UserDto;
import com.realestate.entity.User;
import com.realestate.exception.BadRequestException;
import com.realestate.exception.ResourceNotFoundException;
import com.realestate.repository.UserRepository;
import com.realestate.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAccountDeletionService userAccountDeletionService;

    public UserDto getProfile(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return UserDto.from(user);
    }

    @Transactional
    public UserDto updateProfile(Long userId, ProfileUpdateRequest request, UserPrincipal current) {
        if (!current.getId().equals(userId) && !current.getRole().equals("ADMIN")) {
            throw new org.springframework.security.access.AccessDeniedException("Not allowed");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String email = request.getEmail().trim().toLowerCase();
            if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailAndIdNot(email, user.getId())) {
                throw new BadRequestException("Email is already registered");
            }
            if (!email.equalsIgnoreCase(user.getEmail())) {
                user.setEmail(email);
                user.setEmailVerified(false);
            }
        }
        if (request.getProfileImageUrl() != null) {
            String url = request.getProfileImageUrl().isBlank() ? null : request.getProfileImageUrl().trim();
            user.setProfileImageUrl(url);
        }
        return UserDto.from(userRepository.save(user));
    }

    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional
    public void deleteMyAccount(UserPrincipal principal, DeleteAccountRequest request) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));
        if (user.getRole() == User.Role.ADMIN) {
            throw new BadRequestException("Admin accounts cannot be self-deleted. Contact another admin.");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Incorrect password");
        }
        userAccountDeletionService.deleteUser(user);
    }
}
