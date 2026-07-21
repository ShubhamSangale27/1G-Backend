package com.realestate.repository;

import com.realestate.entity.DeviceToken;
import com.realestate.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByFcmToken(String fcmToken);

    List<DeviceToken> findByUserIdAndActiveTrue(Long userId);

    void deleteByUserId(Long userId);

    @Query("""
            SELECT dt FROM DeviceToken dt
            JOIN FETCH dt.user u
            WHERE dt.active = true
            AND (:role IS NULL OR u.role = :role)
            """)
    List<DeviceToken> findActiveTokensForRole(@Param("role") User.Role role);
}
