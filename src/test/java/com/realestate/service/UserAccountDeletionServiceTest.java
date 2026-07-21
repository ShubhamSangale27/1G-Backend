package com.realestate.service;

import com.realestate.entity.User;
import com.realestate.exception.BadRequestException;
import com.realestate.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserAccountDeletionServiceTest {

    @Autowired private UserAccountDeletionService userAccountDeletionService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void ensureNotLastAdmin_blocksWhenOnlyOneAdminExists() {
        User onlyAdmin = userRepository.save(User.builder()
                .email("sole-admin@test.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .fullName("Sole Admin")
                .mobile("+913333333399")
                .role(User.Role.ADMIN)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());

        assertThrows(BadRequestException.class,
                () -> userAccountDeletionService.ensureNotLastAdmin(onlyAdmin));
    }
}
