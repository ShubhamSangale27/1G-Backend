package com.realestate.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.entity.Property;
import com.realestate.entity.User;
import com.realestate.repository.AlertRepository;
import com.realestate.repository.UserRepository;
import com.realestate.repository.VisitOTPRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SiteVisitOtpFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private VisitOTPRepository visitOTPRepository;
    @Autowired private AlertRepository alertRepository;
    @Autowired private com.realestate.repository.PropertyRepository propertyRepository;

    private User customer;
    private User agent;
    private User admin;
    private Property property;
    private String customerToken;
    private String agentToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        User owner = userRepository.save(User.builder()
                .email("owner-flow@test.com")
                .passwordHash(passwordEncoder.encode("password"))
                .fullName("Owner")
                .mobile("919999999001")
                .role(User.Role.USER)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());

        customer = userRepository.save(User.builder()
                .email("customer-flow@test.com")
                .passwordHash(passwordEncoder.encode("password"))
                .fullName("Customer")
                .mobile("919999999002")
                .role(User.Role.USER)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());

        agent = userRepository.save(User.builder()
                .email("agent-flow@test.com")
                .passwordHash(passwordEncoder.encode("password"))
                .fullName("Agent")
                .mobile("919999999003")
                .role(User.Role.AGENT)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());

        admin = userRepository.save(User.builder()
                .email("admin-flow@test.com")
                .passwordHash(passwordEncoder.encode("password"))
                .fullName("Admin")
                .mobile("919999999004")
                .role(User.Role.ADMIN)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());

        property = propertyRepository.save(Property.builder()
                .title("Flow Test Property")
                .description("Test")
                .listingType(Property.ListingType.SALE)
                .propertyType(Property.PropertyType.APARTMENT)
                .price(BigDecimal.valueOf(1000000))
                .address("1 Test Lane")
                .city("Mumbai")
                .state("MH")
                .status(Property.PropertyStatus.APPROVED)
                .owner(owner)
                .build());

        customerToken = bearer(customer);
        agentToken = bearer(agent);
        adminToken = bearer(admin);
    }

    @Test
    void siteVisitOtpFlow_agentNeverSeesOtp_customerSharesVerbally() throws Exception {
        Instant scheduledAt = Instant.now().plus(2, ChronoUnit.DAYS);

        MvcResult bookResult = mockMvc.perform(post("/sitevisits")
                        .header("Authorization", customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"propertyId":%d,"scheduledAt":"%s","userNotes":"Please call first"}
                                """.formatted(property.getId(), scheduledAt.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_ASSIGNMENT"))
                .andReturn();

        long visitId = objectMapper.readTree(bookResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/admin/sitevisits/" + visitId + "/assign?agentId=" + agent.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        String otp = visitOTPRepository.findBySiteVisitId(visitId)
                .orElseThrow()
                .getOtpCode();
        assertThat(otp).hasSize(6);

        // Agent cannot fetch OTP via customer endpoint
        mockMvc.perform(get("/sitevisits/" + visitId + "/otp")
                        .header("Authorization", agentToken))
                .andExpect(status().isForbidden());

        // Customer can fetch OTP for their visit
        mockMvc.perform(get("/sitevisits/" + visitId + "/otp")
                        .header("Authorization", customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otp").value(otp));

        // Agent detail never includes OTP
        MvcResult detailResult = mockMvc.perform(get("/agent/sitevisits/" + visitId)
                        .header("Authorization", agentToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode detail = objectMapper.readTree(detailResult.getResponse().getContentAsString());
        assertThat(detail.has("otp")).isFalse();
        assertThat(detail.toString()).doesNotContain(otp);

        // Agent notifications must not contain OTP
        MvcResult agentAlerts = mockMvc.perform(get("/alerts")
                        .header("Authorization", agentToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(agentAlerts.getResponse().getContentAsString()).doesNotContain(otp);

        // Customer notifications must not embed OTP in message text
        MvcResult customerAlerts = mockMvc.perform(get("/alerts")
                        .header("Authorization", customerToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(customerAlerts.getResponse().getContentAsString()).doesNotContain(otp);

        // Agent completes visit only after customer shares OTP verbally
        mockMvc.perform(post("/agent/sitevisits/" + visitId + "/complete?otp=000000")
                        .header("Authorization", agentToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/agent/sitevisits/" + visitId + "/complete?otp=" + otp)
                        .header("Authorization", agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    private String bearer(User user) {
        return "Bearer " + jwtUtils.generateAccessToken(user.getEmail(), user.getId(), user.getRole().name());
    }
}
