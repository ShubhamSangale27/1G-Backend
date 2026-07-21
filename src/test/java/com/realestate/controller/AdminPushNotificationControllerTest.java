package com.realestate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminPushNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtils jwtUtils;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        User admin = userRepository.save(User.builder()
                .email("admin-push@test.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .fullName("Admin Push")
                .mobile("+913333333331")
                .role(User.Role.ADMIN)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());
        User buyer = userRepository.save(User.builder()
                .email("buyer-push@test.com")
                .passwordHash(passwordEncoder.encode("user12345"))
                .fullName("Buyer Push")
                .mobile("+913333333332")
                .role(User.Role.USER)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());
        adminToken = jwtUtils.generateAccessToken(admin.getEmail(), admin.getId(), "ADMIN");
        userToken = jwtUtils.generateAccessToken(buyer.getEmail(), buyer.getId(), "USER");
    }

    @Test
    void sendPush_requiresAdmin() throws Exception {
        mockMvc.perform(post("/admin/push-notifications/send")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Hello",
                                "body", "World"
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void sendPush_adminCanCreateCampaign() throws Exception {
        mockMvc.perform(post("/admin/push-notifications/send")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "New listing",
                                "body", "Check property 42",
                                "linkUrl", "/property/42",
                                "linkTarget", "APP",
                                "targetRole", "ALL"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId", notNullValue()))
                .andExpect(jsonPath("$.message", not(emptyString())));
    }

    @Test
    void listPushCampaigns_adminOnly() throws Exception {
        mockMvc.perform(get("/admin/push-notifications")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    void registerFcmToken_authenticatedUser() throws Exception {
        mockMvc.perform(post("/devices/fcm-token")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", "test-fcm-token-abc",
                                "platform", "ANDROID"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered", is(true)));
    }
}
