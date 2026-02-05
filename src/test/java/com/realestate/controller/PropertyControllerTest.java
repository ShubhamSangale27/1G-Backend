package com.realestate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.dto.PropertySearchRequest;
import com.realestate.entity.Property;
import com.realestate.repository.PropertyRepository;
import com.realestate.repository.UserRepository;
import com.realestate.entity.User;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PropertyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .email("owner@test.com")
                .passwordHash(passwordEncoder.encode("password"))
                .fullName("Test Owner")
                .mobile("+911234567890")
                .role(User.Role.USER)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build();
        user = userRepository.save(user);
        Property p = Property.builder()
                .title("Test Property")
                .description("Desc")
                .listingType(Property.ListingType.SALE)
                .propertyType(Property.PropertyType.APARTMENT)
                .price(BigDecimal.valueOf(5000000))
                .address("123 Test St")
                .city("Mumbai")
                .status(Property.PropertyStatus.APPROVED)
                .owner(user)
                .build();
        propertyRepository.save(p);
    }

    @Test
    void getFeatured_returnsOk() throws Exception {
        mockMvc.perform(get("/properties/public/featured")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void search_returnsOk() throws Exception {
        mockMvc.perform(get("/properties/search")
                        .param("page", "0")
                        .param("size", "12")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void getById_returnsOk() throws Exception {
        Long id = propertyRepository.findAll().get(0).getId();
        mockMvc.perform(get("/properties/" + id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
