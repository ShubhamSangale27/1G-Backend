package com.realestate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.dto.PropertyCreateUpdateRequest;
import com.realestate.dto.SiteVisitRequest;
import com.realestate.entity.Property;
import com.realestate.entity.User;
import com.realestate.repository.PropertyRepository;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminPropertySoldOutControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;

    private String adminToken;
    private String userToken;
    private Property property;

    @BeforeEach
    void setUp() {
        User admin = userRepository.save(User.builder()
                .email("admin-sold@test.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .fullName("Admin Sold")
                .mobile("+913333334001")
                .role(User.Role.ADMIN)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());
        User user = userRepository.save(User.builder()
                .email("user-sold@test.com")
                .passwordHash(passwordEncoder.encode("user123"))
                .fullName("User Sold")
                .mobile("+913333334002")
                .role(User.Role.USER)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());
        adminToken = jwtUtils.generateAccessToken(admin.getEmail(), admin.getId(), "ADMIN");
        userToken = jwtUtils.generateAccessToken(user.getEmail(), user.getId(), "USER");

        property = propertyRepository.save(Property.builder()
                .title("Sold Out Candidate")
                .description("Desc")
                .listingType(Property.ListingType.SALE)
                .propertyType(Property.PropertyType.FLAT)
                .price(BigDecimal.valueOf(4500000))
                .address("1 Test Lane")
                .city("Mumbai")
                .state("Maharashtra")
                .status(Property.PropertyStatus.APPROVED)
                .owner(user)
                .featured(false)
                .soldOut(false)
                .build());
    }

    @Test
    void admin_canMarkSoldOut_andPublicDtoReflectsFlag() throws Exception {
        mockMvc.perform(put("/admin/properties/" + property.getId() + "/sold-out")
                        .param("soldOut", "true")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.soldOut").value(true))
                .andExpect(jsonPath("$.propertyType").value("FLAT"));

        mockMvc.perform(get("/properties/" + property.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.soldOut").value(true));
    }

    @Test
    void admin_canClearSoldOut() throws Exception {
        property.setSoldOut(true);
        propertyRepository.save(property);

        mockMvc.perform(put("/admin/properties/" + property.getId() + "/sold-out")
                        .param("soldOut", "false")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.soldOut").value(false));
    }

    @Test
    void nonAdmin_cannotMarkSoldOut() throws Exception {
        mockMvc.perform(put("/admin/properties/" + property.getId() + "/sold-out")
                        .param("soldOut", "true")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void soldOut_unknownProperty_returnsNotFound() throws Exception {
        mockMvc.perform(put("/admin/properties/999999/sold-out")
                        .param("soldOut", "true")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void bookSiteVisit_onSoldOut_returnsBadRequest() throws Exception {
        property.setSoldOut(true);
        propertyRepository.save(property);

        SiteVisitRequest req = new SiteVisitRequest();
        req.setPropertyId(property.getId());
        req.setScheduledAt(Instant.now().plus(2, ChronoUnit.DAYS));

        mockMvc.perform(post("/sitevisits")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("sold out")));
    }

    @Test
    void createProperty_withFlatAndPg_succeeds() throws Exception {
        PropertyCreateUpdateRequest flatReq = baseCreateRequest(Property.PropertyType.FLAT, "New Flat");
        mockMvc.perform(post("/admin/properties")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(flatReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertyType").value("FLAT"))
                .andExpect(jsonPath("$.soldOut").value(false));

        PropertyCreateUpdateRequest pgReq = baseCreateRequest(Property.PropertyType.PG, "New PG");
        mockMvc.perform(post("/admin/properties")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pgReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertyType").value("PG"));
    }

    @Test
    void createProperty_legacyApartmentAlias_mapsToFlat() throws Exception {
        String body = """
                {
                  "title": "Legacy Apt",
                  "listingType": "SALE",
                  "propertyType": "APARTMENT",
                  "price": 1000000,
                  "address": "2 Legacy Rd",
                  "city": "Pune",
                  "state": "Maharashtra"
                }
                """;
        mockMvc.perform(post("/admin/properties")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertyType").value("FLAT"));
    }

    @Test
    void search_byFlatAndLegacyApartment_returnsMatches() throws Exception {
        mockMvc.perform(get("/properties/search")
                        .param("propertyType", "FLAT")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].propertyType", everyItem(is("FLAT"))));

        mockMvc.perform(get("/properties/search")
                        .param("propertyType", "APARTMENT")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].propertyType", everyItem(is("FLAT"))));
    }

    @Test
    void search_byPg_filtersCorrectly() throws Exception {
        propertyRepository.save(Property.builder()
                .title("PG Listing")
                .listingType(Property.ListingType.RENT)
                .propertyType(Property.PropertyType.PG)
                .price(BigDecimal.valueOf(8000))
                .address("PG Street")
                .city("Mumbai")
                .status(Property.PropertyStatus.APPROVED)
                .owner(property.getOwner())
                .soldOut(false)
                .featured(false)
                .build());

        mockMvc.perform(get("/properties/search")
                        .param("propertyType", "PG")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].propertyType", everyItem(is("PG"))))
                .andExpect(jsonPath("$.content[?(@.title=='PG Listing')]").exists());
    }

    private PropertyCreateUpdateRequest baseCreateRequest(Property.PropertyType type, String title) {
        PropertyCreateUpdateRequest req = new PropertyCreateUpdateRequest();
        req.setTitle(title);
        req.setListingType(Property.ListingType.SALE);
        req.setPropertyType(type);
        req.setPrice(BigDecimal.valueOf(2000000));
        req.setAddress("Create Addr");
        req.setCity("Mumbai");
        req.setState("Maharashtra");
        return req;
    }
}
