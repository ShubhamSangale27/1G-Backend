package com.realestate.controller;

import com.realestate.entity.User;
import com.realestate.exception.BadRequestException;
import com.realestate.repository.UserRepository;
import com.realestate.security.JwtUtils;
import com.realestate.service.UserAccountDeletionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminUserDeletionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private UserAccountDeletionService userAccountDeletionService;

    private User admin;
    private User targetUser;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(User.builder()
                .email("admin-delete@test.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .fullName("Admin Delete")
                .mobile("+913333333301")
                .role(User.Role.ADMIN)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());
        targetUser = userRepository.save(User.builder()
                .email("target-delete@test.com")
                .passwordHash(passwordEncoder.encode("user12345"))
                .fullName("Target User")
                .mobile("+913333333302")
                .role(User.Role.USER)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());
        adminToken = jwtUtils.generateAccessToken(admin.getEmail(), admin.getId(), "ADMIN");
        userToken = jwtUtils.generateAccessToken(targetUser.getEmail(), targetUser.getId(), "USER");
    }

    @Test
    void adminDeleteUser_deletesTargetWithoutPassword() throws Exception {
        mockMvc.perform(delete("/admin/users/" + targetUser.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));

        assertFalse(userRepository.findById(targetUser.getId()).isPresent());
    }

    @Test
    void adminDeleteUser_selfDeleteForbidden() throws Exception {
        mockMvc.perform(delete("/admin/users/" + admin.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        assertTrue(userRepository.findById(admin.getId()).isPresent());
    }

    @Test
    void adminDeleteUser_nonAdminForbidden() throws Exception {
        mockMvc.perform(delete("/admin/users/" + admin.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        assertTrue(userRepository.findById(admin.getId()).isPresent());
    }

    @Test
    void adminDeleteUser_lastAdminForbidden() throws Exception {
        User otherAdmin = userRepository.save(User.builder()
                .email("other-admin-delete@test.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .fullName("Other Admin")
                .mobile("+913333333303")
                .role(User.Role.ADMIN)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());

        mockMvc.perform(delete("/admin/users/" + otherAdmin.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        assertThrows(BadRequestException.class,
                () -> userAccountDeletionService.ensureNotLastAdmin(admin));
    }
}
