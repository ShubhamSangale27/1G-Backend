package com.realestate.dto;

import com.realestate.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private Long id;
    private String email;
    private String fullName;
    private String mobile;
    private User.Role role;
    private boolean emailVerified;
    private boolean mobileVerified;
    private boolean active;
    private Instant createdAt;

    public static UserDto from(User u) {
        if (u == null) return null;
        return UserDto.builder()
                .id(u.getId())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .mobile(u.getMobile())
                .role(u.getRole())
                .emailVerified(u.isEmailVerified())
                .mobileVerified(u.isMobileVerified())
                .active(u.isActive())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
