package com.realestate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.dto.MarketAreaCreateUpdateRequest;
import com.realestate.entity.MarketArea;
import com.realestate.entity.User;
import com.realestate.repository.MarketAreaRepository;
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
    void createStateCityLocality_hierarchyVisibleOnPublicApi() throws Exception {
        MarketAreaCreateUpdateRequest stateReq = MarketAreaCreateUpdateRequest.builder()
                .level("STATE")
                .name("Goa")
                .active(true)
                .sortOrder(1)
                .build();
        String stateJson = mockMvc.perform(post("/admin/market-stats/areas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Goa"))
                .andExpect(jsonPath("$.level").value("STATE"))
                .andReturn().getResponse().getContentAsString();

        long stateId = objectMapper.readTree(stateJson).get("id").asLong();

        MarketAreaCreateUpdateRequest cityReq = MarketAreaCreateUpdateRequest.builder()
                .level("CITY")
                .name("Panaji")
                .parentId(stateId)
                .active(true)
                .build();
        String cityJson = mockMvc.perform(post("/admin/market-stats/areas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cityReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Panaji"))
                .andExpect(jsonPath("$.stateName").value("Goa"))
                .andReturn().getResponse().getContentAsString();

        long cityId = objectMapper.readTree(cityJson).get("id").asLong();

        MarketAreaCreateUpdateRequest locReq = MarketAreaCreateUpdateRequest.builder()
                .level("LOCALITY")
                .name("Altinho")
                .parentId(cityId)
                .active(true)
                .build();
        mockMvc.perform(post("/admin/market-stats/areas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(locReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Altinho"))
                .andExpect(jsonPath("$.cityName").value("Panaji"))
                .andExpect(jsonPath("$.stateName").value("Goa"));

        mockMvc.perform(get("/market-stats/areas").param("level", "STATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Goa")));

        mockMvc.perform(get("/market-stats/areas")
                        .param("parentId", String.valueOf(stateId))
                        .param("level", "CITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Panaji"));

        mockMvc.perform(get("/market-stats/areas")
                        .param("parentId", String.valueOf(cityId))
                        .param("level", "LOCALITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Altinho"));
    }

    @Test
    void createCity_withoutStateParent_returnsBadRequest() throws Exception {
        MarketAreaCreateUpdateRequest cityReq = MarketAreaCreateUpdateRequest.builder()
                .level("CITY")
                .name("Orphan City")
                .active(true)
                .build();

        mockMvc.perform(post("/admin/market-stats/areas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cityReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inactiveArea_notListedOnPublicApi() throws Exception {
        MarketArea state = areaRepository.save(MarketArea.builder()
                .level(MarketArea.Level.STATE)
                .name("Hidden State")
                .stateName("Hidden State")
                .stateSlug("hidden-state")
                .active(false)
                .build());

        mockMvc.perform(get("/market-stats/areas").param("level", "STATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", not(hasItem("Hidden State"))));

        MarketAreaCreateUpdateRequest update = MarketAreaCreateUpdateRequest.builder()
                .level("STATE")
                .name("Hidden State")
                .active(true)
                .build();
        mockMvc.perform(put("/admin/market-stats/areas/" + state.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/market-stats/areas").param("level", "STATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Hidden State")));
    }

    @Test
    void nonAdmin_cannotCreateArea() throws Exception {
        MarketAreaCreateUpdateRequest stateReq = MarketAreaCreateUpdateRequest.builder()
                .level("STATE")
                .name("Forbidden")
                .active(true)
                .build();

        mockMvc.perform(post("/admin/market-stats/areas")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stateReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteArea_removesFromPublicList() throws Exception {
        MarketArea state = areaRepository.save(MarketArea.builder()
                .level(MarketArea.Level.STATE)
                .name("Temp State")
                .stateName("Temp State")
                .stateSlug("temp-state")
                .active(true)
                .build());

        mockMvc.perform(delete("/admin/market-stats/areas/" + state.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(true));

        mockMvc.perform(get("/market-stats/areas").param("level", "STATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", not(hasItem("Temp State"))));
    }
}
