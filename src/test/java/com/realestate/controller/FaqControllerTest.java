package com.realestate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.entity.Faq;
import com.realestate.entity.UnmatchedFaqQuestion;
import com.realestate.entity.User;
import com.realestate.repository.FaqRepository;
import com.realestate.repository.UnmatchedFaqQuestionRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FaqControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FaqRepository faqRepository;
    @Autowired
    private UnmatchedFaqQuestionRepository unmatchedRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtils jwtUtils;

    private User admin;
    private User buyer;
    private String adminToken;
    private String buyerToken;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(User.builder()
                .email("admin-faq@test.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .fullName("Admin FAQ")
                .mobile("+911111111111")
                .role(User.Role.ADMIN)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());
        buyer = userRepository.save(User.builder()
                .email("buyer-faq@test.com")
                .passwordHash(passwordEncoder.encode("user12345"))
                .fullName("Buyer FAQ")
                .mobile("+912222222222")
                .role(User.Role.USER)
                .emailVerified(true)
                .mobileVerified(true)
                .active(true)
                .build());
        adminToken = jwtUtils.generateAccessToken(admin.getEmail(), admin.getId(), "ADMIN");
        buyerToken = jwtUtils.generateAccessToken(buyer.getEmail(), buyer.getId(), "USER");

        faqRepository.save(Faq.builder()
                .question("How do I schedule a site visit?")
                .answer("Open a property and click Book Site Visit.")
                .keywords("visit, schedule, book")
                .active(true)
                .sortOrder(1)
                .build());
    }

    @Test
    void listActiveFaqs_isPublic() throws Exception {
        mockMvc.perform(get("/faq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].question", containsString("site visit")))
                .andExpect(jsonPath("$[0].answer", containsString("Book Site Visit")));
    }

    @Test
    void ask_returnsMatchedAnswer_public() throws Exception {
        mockMvc.perform(post("/faq/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "question", "How do I schedule a site visit?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(true))
                .andExpect(jsonPath("$.answer", containsString("Book Site Visit")))
                .andExpect(jsonPath("$.faqId").isNumber());
    }

    @Test
    void ask_outOfScope_anonymous_storesUnknownUser() throws Exception {
        mockMvc.perform(post("/faq/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "question", "What is the capital of France unrelated?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(false))
                .andExpect(jsonPath("$.answer").value("Just Missedcall on 9134913491, will help you!"))
                .andExpect(jsonPath("$.contactPhone").value("9134913491"));

        assertThat(unmatchedRepository.findAll()).hasSize(1);
        UnmatchedFaqQuestion u = unmatchedRepository.findAll().get(0);
        assertThat(u.getUser()).isNull();
        assertThat(u.getStatus()).isEqualTo(UnmatchedFaqQuestion.Status.PENDING);
    }

    @Test
    void ask_outOfScope_loggedIn_storesUser() throws Exception {
        mockMvc.perform(post("/faq/ask")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "question", "Completely unrelated astronomy question about quasars"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(false));

        UnmatchedFaqQuestion u = unmatchedRepository.findAll().get(0);
        assertThat(u.getUser()).isNotNull();
        assertThat(u.getUser().getId()).isEqualTo(buyer.getId());
    }

    @Test
    void adminCrud_requiresAdmin() throws Exception {
        mockMvc.perform(get("/admin/faqs"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/admin/faqs")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertThat(status).as("non-admin must be denied").isIn(401, 403);
                });

        mockMvc.perform(post("/admin/faqs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "question", "What are the fees?",
                                "answer", "Listing is free for basic plans.",
                                "keywords", "fees, cost",
                                "active", true,
                                "sortOrder", 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.question").value("What are the fees?"));
    }

    @Test
    void admin_listUnmatched_resolve_and_promote() throws Exception {
        unmatchedRepository.save(UnmatchedFaqQuestion.builder()
                .questionText("Do you support commercial plots?")
                .user(buyer)
                .status(UnmatchedFaqQuestion.Status.PENDING)
                .build());

        mockMvc.perform(get("/admin/faqs/unmatched")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].questionText", containsString("commercial")))
                .andExpect(jsonPath("$[0].userFullName").value("Buyer FAQ"))
                .andExpect(jsonPath("$[0].userEmail").value("buyer-faq@test.com"));

        Long id = unmatchedRepository.findAll().get(0).getId();

        mockMvc.perform(post("/admin/faqs/unmatched/" + id + "/promote")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "answer", "Yes, you can list commercial plots.",
                                "keywords", "commercial, plot"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.promotedFaqId").isNumber());

        mockMvc.perform(post("/faq/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "question", "Do you support commercial plots?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(true))
                .andExpect(jsonPath("$.answer", containsString("commercial plots")));
    }

    @Test
    void metrics_includesUnmatchedFaqPending() throws Exception {
        unmatchedRepository.save(UnmatchedFaqQuestion.builder()
                .questionText("Pending q")
                .status(UnmatchedFaqQuestion.Status.PENDING)
                .build());

        mockMvc.perform(get("/admin/metrics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unmatchedFaqPending", greaterThanOrEqualTo(1)));
    }
}
