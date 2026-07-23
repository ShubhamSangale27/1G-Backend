package com.realestate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.dto.MarketAreaCreateUpdateRequest;
import com.realestate.dto.MarketProjectionRequest;
import com.realestate.entity.MarketArea;
import com.realestate.entity.MarketStatSnapshot;
import com.realestate.entity.User;
import com.realestate.repository.MarketAreaRepository;
import com.realestate.repository.MarketStatSnapshotRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminMarketStatsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private MarketAreaRepository areaRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        User admin = userRepository.save(User.builder()
                .email("admin-market@test.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .fullName("Admin Market")
                .mobile("+913333333401")
                .role(User.Role.ADMIN)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());
        User user = userRepository.save(User.builder()
                .email("user-market@test.com")
                .passwordHash(passwordEncoder.encode("user123"))
                .fullName("User Market")
                .mobile("+913333333402")
                .role(User.Role.USER)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());
        adminToken = jwtUtils.generateAccessToken(admin.getEmail(), admin.getId(), "ADMIN");
        userToken = jwtUtils.generateAccessToken(user.getEmail(), user.getId(), "USER");
    }

    @Test
    void createLocality_withStaticStateCity_visibleOnPublicApi() throws Exception {
        MarketAreaCreateUpdateRequest locReq = MarketAreaCreateUpdateRequest.builder()
                .level("LOCALITY")
                .name("Altinho")
                .stateName("Goa")
                .cityName("Panaji")
                .active(true)
                .build();
        mockMvc.perform(post("/admin/market-stats/areas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Altinho"))
                .andExpect(jsonPath("$.level").value("LOCALITY"))
                .andExpect(jsonPath("$.cityName").value("Panaji"))
                .andExpect(jsonPath("$.stateName").value("Goa"));

        mockMvc.perform(get("/market-stats/areas")
                        .param("state", "Goa")
                        .param("city", "Panaji")
                        .param("level", "LOCALITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Altinho"));
    }

    @Test
    void createState_rejected() throws Exception {
        MarketAreaCreateUpdateRequest stateReq = MarketAreaCreateUpdateRequest.builder()
                .level("STATE")
                .name("Forbidden")
                .active(true)
                .build();

        mockMvc.perform(post("/admin/market-stats/areas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stateReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createLocality_withoutStateCity_returnsBadRequest() throws Exception {
        MarketAreaCreateUpdateRequest req = MarketAreaCreateUpdateRequest.builder()
                .level("LOCALITY")
                .name("Orphan")
                .active(true)
                .build();

        mockMvc.perform(post("/admin/market-stats/areas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonAdmin_cannotCreateLocality() throws Exception {
        MarketAreaCreateUpdateRequest req = MarketAreaCreateUpdateRequest.builder()
                .level("LOCALITY")
                .name("Forbidden")
                .stateName("Goa")
                .cityName("Panaji")
                .active(true)
                .build();

        mockMvc.perform(post("/admin/market-stats/areas")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
