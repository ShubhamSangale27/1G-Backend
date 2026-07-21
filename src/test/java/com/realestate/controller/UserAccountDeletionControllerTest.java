package com.realestate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.dto.DeleteAccountRequest;
import com.realestate.entity.User;
import com.realestate.repository.UserRepository;
import com.realestate.security.JwtUtils;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserAccountDeletionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;

    private User user;
    private String userToken;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email("delete-me@test.com")
                .passwordHash(passwordEncoder.encode("correctPass"))
                .fullName("Delete Me")
                .mobile("+919999999901")
                .role(User.Role.USER)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());
        userToken = jwtUtils.generateAccessToken(user.getEmail(), user.getId(), "USER");
    }

    @Test
    void deleteMyAccount_withCorrectPassword_deletesUser() throws Exception {
        DeleteAccountRequest request = new DeleteAccountRequest();
        request.setPassword("correctPass");

        mockMvc.perform(delete("/users/me")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        assertFalse(userRepository.findById(user.getId()).isPresent());
    }

    @Test
    void deleteMyAccount_withWrongPassword_returnsBadRequest() throws Exception {
        DeleteAccountRequest request = new DeleteAccountRequest();
        request.setPassword("wrongPass");

        mockMvc.perform(delete("/users/me")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertTrue(userRepository.findById(user.getId()).isPresent());
    }

    @Test
    void deleteMyAccount_withoutAuth_returnsUnauthorized() throws Exception {
        DeleteAccountRequest request = new DeleteAccountRequest();
        request.setPassword("correctPass");

        mockMvc.perform(delete("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteMyAccount_adminSelfDelete_returnsBadRequest() throws Exception {
        User admin = userRepository.save(User.builder()
                .email("admin-self-delete@test.com")
                .passwordHash(passwordEncoder.encode("adminPass"))
                .fullName("Admin Self")
                .mobile("+919999999902")
                .role(User.Role.ADMIN)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());
        String adminToken = jwtUtils.generateAccessToken(admin.getEmail(), admin.getId(), "ADMIN");

        DeleteAccountRequest request = new DeleteAccountRequest();
        request.setPassword("adminPass");

        mockMvc.perform(delete("/users/me")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertFalse(userRepository.findById(admin.getId()).isEmpty());
    }
}
