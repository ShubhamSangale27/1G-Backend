package com.realestate.service;

import com.realestate.entity.DeviceToken;
import com.realestate.entity.User;
import com.realestate.repository.DeviceTokenRepository;
import com.realestate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerToken(Long userId, String token, String platform) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        DeviceToken.Platform platformEnum = DeviceToken.Platform.valueOf(platform.toUpperCase());

        deviceTokenRepository.findByFcmToken(token).ifPresentOrElse(existing -> {
            existing.setUser(user);
            existing.setPlatform(platformEnum);
            existing.setActive(true);
            deviceTokenRepository.save(existing);
        }, () -> deviceTokenRepository.save(DeviceToken.builder()
                .user(user)
                .fcmToken(token)
                .platform(platformEnum)
                .active(true)
                .build()));
    }

    @Transactional
    public void deactivateToken(Long userId, String token) {
        deviceTokenRepository.findByFcmToken(token).ifPresent(existing -> {
            if (existing.getUser().getId().equals(userId)) {
                existing.setActive(false);
                deviceTokenRepository.save(existing);
            }
        });
    }

    @Transactional
    public void deactivateByFcmToken(String token) {
        deviceTokenRepository.findByFcmToken(token).ifPresent(existing -> {
            existing.setActive(false);
            deviceTokenRepository.save(existing);
        });
    }

    @Transactional
    public void deactivateAllForUser(Long userId) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(userId);
        for (DeviceToken token : tokens) {
            token.setActive(false);
        }
        deviceTokenRepository.saveAll(tokens);
    }

    public List<DeviceToken> findActiveTokensForTarget(User.Role roleFilter) {
        return deviceTokenRepository.findActiveTokensForRole(roleFilter);
    }
}
