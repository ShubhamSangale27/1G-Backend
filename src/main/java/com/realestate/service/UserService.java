package com.realestate.service;

import com.realestate.dto.UserDto;
import com.realestate.entity.User;
import com.realestate.exception.ResourceNotFoundException;
import com.realestate.repository.UserRepository;
import com.realestate.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserDto getProfile(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return UserDto.from(user);
    }

    @Transactional
    public UserDto updateProfile(Long userId, String fullName, String mobile, UserPrincipal current) {
        if (!current.getId().equals(userId) && !current.getRole().equals("ADMIN")) {
            throw new org.springframework.security.access.AccessDeniedException("Not allowed");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (fullName != null) user.setFullName(fullName);
        if (mobile != null) user.setMobile(mobile);
        return UserDto.from(userRepository.save(user));
    }

    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
