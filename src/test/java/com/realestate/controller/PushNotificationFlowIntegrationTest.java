package com.realestate.controller;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PushNotificationFlowIntegrationTest {

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
                .email("admin-flow-push@test.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .fullName("Admin Flow Push")
                .mobile("+913333333341")
                .role(User.Role.ADMIN)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());
        User buyer = userRepository.save(User.builder()
                .email("buyer-flow-push@test.com")
                .passwordHash(passwordEncoder.encode("user12345"))
                .fullName("Buyer Flow Push")
                .mobile("+913333333342")
                .role(User.Role.USER)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());
        adminToken = jwtUtils.generateAccessToken(admin.getEmail(), admin.getId(), "ADMIN");
        userToken = jwtUtils.generateAccessToken(buyer.getEmail(), buyer.getId(), "USER");
    }

    @Test
    void endToEnd_registerToken_sendCampaign_listHistory_whenFirebaseNotConfigured() throws Exception {
        mockMvc.perform(post("/devices/fcm-token")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", "e2e-fcm-token-flow",
                                "platform", "ANDROID"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered", is(true)));

        MvcResult sendResult = mockMvc.perform(post("/admin/push-notifications/send")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "E2E push",
                                "body", "Firebase env credential flow test",
                                "linkUrl", "/property/99",
                                "linkTarget", "APP",
                                "targetRole", "ALL"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId", notNullValue()))
                .andExpect(jsonPath("$.sentCount", is(0)))
                .andExpect(jsonPath("$.failedCount", is(1)))
                .andExpect(jsonPath("$.message", containsString("FIREBASE_CREDENTIALS_JSON")))
                .andExpect(jsonPath("$.message", containsString("FIREBASE_CREDENTIALS_BASE64")))
                .andExpect(jsonPath("$.message", containsString("FIREBASE_CREDENTIALS_PATH")))
                .andReturn();

        JsonNode sendBody = objectMapper.readTree(sendResult.getResponse().getContentAsString());
        long campaignId = sendBody.get("campaignId").asLong();

        MvcResult listResult = mockMvc.perform(get("/admin/push-notifications")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andReturn();

        JsonNode campaigns = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .get("content");
        assertThat(campaigns.isArray()).isTrue();
        assertThat(campaigns)
                .anySatisfy(node -> {
                    assertThat(node.get("id").asLong()).isEqualTo(campaignId);
                    assertThat(node.get("title").asText()).isEqualTo("E2E push");
                    assertThat(node.get("body").asText()).isEqualTo("Firebase env credential flow test");
                });
    }
}
