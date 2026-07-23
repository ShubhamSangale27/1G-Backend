package com.realestate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realestate.dto.MarketProjectionRequest;
import com.realestate.entity.MarketArea;
import com.realestate.entity.MarketStatSnapshot;
import com.realestate.repository.MarketAreaRepository;
import com.realestate.repository.MarketStatSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MarketStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MarketAreaRepository areaRepository;
    @Autowired
    private MarketStatSnapshotRepository snapshotRepository;

    private MarketArea locality;

    @BeforeEach
    void setUp() {
        MarketArea state = areaRepository.save(MarketArea.builder()
                .level(MarketArea.Level.STATE)
                .name("Maharashtra")
                .stateName("Maharashtra")
                .stateSlug("maharashtra")
                .active(true)
                .build());
        MarketArea city = areaRepository.save(MarketArea.builder()
                .parent(state)
                .level(MarketArea.Level.CITY)
                .name("Mumbai")
                .stateName("Maharashtra")
                .cityName("Mumbai")
                .stateSlug("maharashtra")
                .citySlug("mumbai")
                .active(true)
                .build());
        locality = areaRepository.save(MarketArea.builder()
                .parent(city)
                .level(MarketArea.Level.LOCALITY)
                .name("Andheri")
                .stateName("Maharashtra")
                .cityName("Mumbai")
                .stateSlug("maharashtra")
                .citySlug("mumbai")
                .locationSlug("andheri")
                .active(true)
                .build());
        snapshotRepository.save(MarketStatSnapshot.builder()
                .marketArea(locality)
                .snapshotDate(LocalDate.of(2021, 3, 31))
                .priceIndex(new BigDecimal("100"))
                .avgPricePerSqft(new BigDecimal("20000"))
                .granularity(MarketStatSnapshot.Granularity.QUARTERLY)
                .sourceType(MarketStatSnapshot.SourceType.ADMIN)
                .build());
        snapshotRepository.save(MarketStatSnapshot.builder()
                .marketArea(locality)
                .snapshotDate(LocalDate.of(2026, 3, 31))
                .priceIndex(new BigDecimal("150"))
                .avgPricePerSqft(new BigDecimal("30000"))
                .granularity(MarketStatSnapshot.Granularity.QUARTERLY)
                .sourceType(MarketStatSnapshot.SourceType.ADMIN)
                .build());
    }

    @Test
    void listAreas_filtersByStateCityLevel() throws Exception {
        mockMvc.perform(get("/market-stats/areas")
                        .param("state", "Maharashtra")
                        .param("city", "Mumbai")
                        .param("level", "LOCALITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Andheri"));
    }

    @Test
    void listAreas_filtersByParentId_cascade() throws Exception {
        MarketArea state = areaRepository.findAll().stream()
                .filter(a -> a.getLevel() == MarketArea.Level.STATE)
                .findFirst().orElseThrow();
        MarketArea city = areaRepository.findAll().stream()
                .filter(a -> a.getLevel() == MarketArea.Level.CITY)
                .findFirst().orElseThrow();

        mockMvc.perform(get("/market-stats/areas")
                        .param("parentId", String.valueOf(state.getId()))
                        .param("level", "CITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Mumbai")));

        mockMvc.perform(get("/market-stats/areas")
                        .param("parentId", String.valueOf(city.getId()))
                        .param("level", "LOCALITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Andheri"));
    }

    @Test
    void listAreas_inactiveAreaExcluded() throws Exception {
        locality.setActive(false);
        areaRepository.save(locality);

        mockMvc.perform(get("/market-stats/areas")
                        .param("state", "Maharashtra")
                        .param("city", "Mumbai")
                        .param("level", "LOCALITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listStates_returnsOnlyActiveStates() throws Exception {
        areaRepository.save(MarketArea.builder()
                .level(MarketArea.Level.STATE)
                .name("Inactive Test State")
                .stateName("Inactive Test State")
                .stateSlug("inactive-test-state")
                .active(false)
                .build());

        mockMvc.perform(get("/market-stats/areas").param("level", "STATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Maharashtra")))
                .andExpect(jsonPath("$[*].name", not(hasItem("Inactive Test State"))));
    }

    @Test
    void getStats_returnsHistoryAndCagr() throws Exception {
        mockMvc.perform(get("/market-stats")
                        .param("areaId", String.valueOf(locality.getId()))
                        .param("range", "MAX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataAvailable").value(true))
                .andExpect(jsonPath("$.history", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.derivedCagrPct").isNumber());
    }

    @Test
    void getStats_unsupportedArea_returnsEmptyState() throws Exception {
        MarketArea empty = areaRepository.save(MarketArea.builder()
                .level(MarketArea.Level.LOCALITY)
                .name("Nowhere")
                .stateName("Goa")
                .cityName("Panaji")
                .parent(areaRepository.findAll().stream()
                        .filter(a -> a.getLevel() == MarketArea.Level.CITY)
                        .findFirst().orElse(null))
                .active(true)
                .build());

        mockMvc.perform(get("/market-stats")
                        .param("areaId", String.valueOf(empty.getId()))
                        .param("range", "1Y"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataAvailable").value(false))
                .andExpect(jsonPath("$.message", containsString("No snapshots")));
    }

    @Test
    void projection_withSnapshots_usesSnapshotCagr() throws Exception {
        MarketProjectionRequest body = MarketProjectionRequest.builder()
                .state("Maharashtra")
                .city("Mumbai")
                .localityId(locality.getId())
                .range("5Y")
                .initialAmount(new BigDecimal("1000000"))
                .monthlyContribution(new BigDecimal("10000"))
                .years(5)
                .expectedRatePct(new BigDecimal("10"))
                .build();

        mockMvc.perform(post("/market-stats/projection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.market.dataAvailable").value(true))
                .andExpect(jsonPath("$.regionalFinal").isNumber());
    }

    @Test
    void projection_withoutLocality_usesCityBenchmark() throws Exception {
        MarketProjectionRequest body = MarketProjectionRequest.builder()
                .state("Maharashtra")
                .city("Mumbai")
                .range("5Y")
                .initialAmount(new BigDecimal("1000000"))
                .monthlyContribution(new BigDecimal("10000"))
                .years(5)
                .build();

        mockMvc.perform(post("/market-stats/projection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regionalRatePct").value(7.2))
                .andExpect(jsonPath("$.market.dataAvailable").value(false))
                .andExpect(jsonPath("$.market.message", containsString("market average")));
    }

    @Test
    void getStatsByLocation_returnsBenchmarkForCity() throws Exception {
        mockMvc.perform(get("/market-stats")
                        .param("state", "Karnataka")
                        .param("city", "Bangalore")
                        .param("range", "5Y"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.derivedCagrPct").value(8.8))
                .andExpect(jsonPath("$.dataAvailable").value(false));
    }

    @Test
    void projection_returnsLines() throws Exception {
        MarketProjectionRequest body = MarketProjectionRequest.builder()
                .state("Maharashtra")
                .city("Mumbai")
                .localityId(locality.getId())
                .range("5Y")
                .initialAmount(new BigDecimal("1000000"))
                .monthlyContribution(new BigDecimal("10000"))
                .years(5)
                .expectedRatePct(new BigDecimal("10"))
                .build();

        mockMvc.perform(post("/market-stats/projection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.userFinal").isNumber())
                .andExpect(jsonPath("$.regionalFinal").isNumber());
    }
}
