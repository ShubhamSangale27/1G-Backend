package com.realestate.service;

import com.realestate.entity.User;
import com.realestate.exception.BadRequestException;
import com.realestate.exception.ResourceNotFoundException;
import com.realestate.repository.DeviceTokenRepository;
import com.realestate.repository.EmailVerificationTokenRepository;
import com.realestate.repository.RefreshTokenRepository;
import com.realestate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAccountDeletionService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Transactional
    public void deleteUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        deleteUser(user);
    }

    @Transactional
    public void deleteUser(User user) {
        Long userId = user.getId();
        refreshTokenRepository.deleteByUserId(userId);
        deviceTokenRepository.deleteByUserId(userId);
        emailVerificationTokenRepository.deleteByUser_Id(userId);
        userRepository.delete(user);
    }

    public void ensureNotLastAdmin(User user) {
        if (user.getRole() == User.Role.ADMIN
                && userRepository.countByRole(User.Role.ADMIN) <= 1) {
            throw new BadRequestException("Cannot delete the last admin account.");
        }
    }
}
