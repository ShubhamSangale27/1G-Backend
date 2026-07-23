package com.realestate.service;

import com.realestate.dto.MarketProjectionRequest;
import com.realestate.dto.MarketProjectionResponse;
import com.realestate.dto.MarketStatsResponse;
import com.realestate.entity.MarketArea;
import com.realestate.entity.MarketStatSnapshot;
import com.realestate.repository.MarketAreaRepository;
import com.realestate.repository.MarketStatSnapshotRepository;
import com.realestate.service.market.MarketBenchmarkRates;
import com.realestate.service.market.RbiHpiSeedProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketStatsServiceTest {

    @Mock
    MarketAreaRepository areaRepository;
    @Mock
    MarketStatSnapshotRepository snapshotRepository;
    @Mock
    RbiHpiSeedProvider rbiHpiSeedProvider;
    @Mock
    MarketBenchmarkRates benchmarkRates;

    @InjectMocks
    MarketStatsService service;

    MarketArea locality;

    @BeforeEach
    void setUp() {
        locality = MarketArea.builder()
                .id(3L)
                .level(MarketArea.Level.LOCALITY)
                .name("Andheri")
                .stateName("Maharashtra")
                .cityName("Mumbai")
                .active(true)
                .build();
    }

    @Test
    void getStats_emptyHistory_returnsUnavailableWithBenchmark() {
        when(areaRepository.findById(3L)).thenReturn(Optional.of(locality));
        when(snapshotRepository.findByAreaAndFromDate(eq(3L), ArgumentMatchers.any()))
                .thenReturn(List.of());
        when(benchmarkRates.forCity("Maharashtra", "Mumbai")).thenReturn(new BigDecimal("7.2"));

        MarketStatsResponse res = service.getStats(3L, "5Y");

        assertFalse(res.isDataAvailable());
        assertEquals(new BigDecimal("7.2"), res.getDerivedCagrPct());
        assertNotNull(res.getMessage());
    }

    @Test
    void getStats_computesCagrAndRangeReturn() {
        when(areaRepository.findById(3L)).thenReturn(Optional.of(locality));
        MarketStatSnapshot s1 = MarketStatSnapshot.builder()
                .id(1L)
                .marketArea(locality)
                .snapshotDate(LocalDate.of(2021, 3, 31))
                .priceIndex(new BigDecimal("100"))
                .avgPricePerSqft(new BigDecimal("20000"))
                .granularity(MarketStatSnapshot.Granularity.QUARTERLY)
                .sourceType(MarketStatSnapshot.SourceType.ADMIN)
                .build();
        MarketStatSnapshot s2 = MarketStatSnapshot.builder()
                .id(2L)
                .marketArea(locality)
                .snapshotDate(LocalDate.of(2026, 3, 31))
                .priceIndex(new BigDecimal("150"))
                .avgPricePerSqft(new BigDecimal("30000"))
                .rentalYieldPct(new BigDecimal("3.2"))
                .granularity(MarketStatSnapshot.Granularity.QUARTERLY)
                .sourceType(MarketStatSnapshot.SourceType.ADMIN)
                .build();
        when(snapshotRepository.findByAreaAndFromDate(eq(3L), ArgumentMatchers.any()))
                .thenReturn(List.of(s1, s2));

        MarketStatsResponse res = service.getStats(3L, "MAX");

        assertTrue(res.isDataAvailable());
        assertEquals(2, res.getHistory().size());
        assertEquals(new BigDecimal("50.00"), res.getRangeReturnPct());
        assertNotNull(res.getDerivedCagrPct());
        assertTrue(res.getDerivedCagrPct().doubleValue() > 0);
    }

    @Test
    void project_buildsUserAndRegionalLines() {
        when(areaRepository.findById(3L)).thenReturn(Optional.of(locality));
        when(snapshotRepository.findByAreaAndFromDate(eq(3L), ArgumentMatchers.any()))
                .thenReturn(List.of());
        when(benchmarkRates.forCity("Maharashtra", "Mumbai")).thenReturn(new BigDecimal("7.2"));

        MarketProjectionResponse res = service.project(MarketProjectionRequest.builder()
                .state("Maharashtra")
                .city("Mumbai")
                .localityId(3L)
                .range("5Y")
                .initialAmount(new BigDecimal("1000000"))
                .monthlyContribution(new BigDecimal("10000"))
                .years(5)
                .expectedRatePct(new BigDecimal("10"))
                .build());

        assertFalse(res.getPoints().isEmpty());
        assertEquals(0, res.getPoints().get(0).getYear());
        assertEquals(5, res.getPoints().get(res.getPoints().size() - 1).getYear());
        assertTrue(res.getUserFinal().compareTo(res.getPoints().get(0).getUser()) > 0);
    }

    @Test
    void project_withoutLocality_usesBenchmarkRate() {
        when(benchmarkRates.forCity("Maharashtra", "Mumbai")).thenReturn(new BigDecimal("7.2"));

        MarketProjectionResponse res = service.project(MarketProjectionRequest.builder()
                .state("Maharashtra")
                .city("Mumbai")
                .range("5Y")
                .initialAmount(new BigDecimal("1000000"))
                .monthlyContribution(BigDecimal.ZERO)
                .years(5)
                .build());

        assertEquals(new BigDecimal("7.2"), res.getRegionalRatePct());
        assertFalse(res.getMarket().isDataAvailable());
    }

    @Test
    void project_withFlatIndexAndPsf_usesPsfGrowthNotBenchmark() {
        when(areaRepository.findById(3L)).thenReturn(Optional.of(locality));
        MarketStatSnapshot s1 = MarketStatSnapshot.builder()
                .marketArea(locality)
                .snapshotDate(LocalDate.of(2021, 1, 1))
                .priceIndex(new BigDecimal("100"))
                .avgPricePerSqft(new BigDecimal("70"))
                .yoyGrowthPct(new BigDecimal("10"))
                .granularity(MarketStatSnapshot.Granularity.QUARTERLY)
                .sourceType(MarketStatSnapshot.SourceType.ADMIN)
                .build();
        MarketStatSnapshot s2 = MarketStatSnapshot.builder()
                .marketArea(locality)
                .snapshotDate(LocalDate.of(2023, 1, 1))
                .priceIndex(new BigDecimal("100"))
                .avgPricePerSqft(new BigDecimal("100"))
                .yoyGrowthPct(new BigDecimal("30"))
                .granularity(MarketStatSnapshot.Granularity.QUARTERLY)
                .sourceType(MarketStatSnapshot.SourceType.ADMIN)
                .build();
        MarketStatSnapshot s3 = MarketStatSnapshot.builder()
                .marketArea(locality)
                .snapshotDate(LocalDate.of(2025, 1, 1))
                .priceIndex(new BigDecimal("100"))
                .avgPricePerSqft(new BigDecimal("130"))
                .yoyGrowthPct(new BigDecimal("30"))
                .granularity(MarketStatSnapshot.Granularity.QUARTERLY)
                .sourceType(MarketStatSnapshot.SourceType.ADMIN)
                .build();
        when(snapshotRepository.findByAreaAndFromDate(eq(3L), ArgumentMatchers.any()))
                .thenReturn(List.of(s1, s2, s3));
        when(benchmarkRates.forCity("Maharashtra", "Mumbai")).thenReturn(new BigDecimal("7.2"));

        MarketProjectionResponse res = service.project(MarketProjectionRequest.builder()
                .state("Maharashtra")
                .city("Mumbai")
                .localityId(3L)
                .range("MAX")
                .initialAmount(new BigDecimal("1000000"))
                .monthlyContribution(BigDecimal.ZERO)
                .years(10)
                .build());

        assertTrue(res.getRegionalRatePct().doubleValue() > 14.0);
        assertTrue(res.getRegionalRatePct().doubleValue() < 18.0);
        assertNotEquals(new BigDecimal("8.5"), res.getRegionalRatePct());
        assertTrue(res.getMarket().isDataAvailable());
        assertTrue(res.getRegionalFinal().compareTo(new BigDecimal("1000000")) > 0);
        assertTrue(res.getPoints().stream().anyMatch(p -> !p.isForecast()
                && p.getRegional().compareTo(new BigDecimal("1000000")) > 0));
    }

    @Test
    void computeCagr_knownValues() {
        BigDecimal cagr = MarketStatsService.computeCagr(
                new BigDecimal("100"),
                new BigDecimal("121"),
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2022, 1, 1));
        assertEquals(10.0, cagr.doubleValue(), 0.2);
    }

    @Test
    void normalizeRange_defaultsInvalidTo5Y() {
        when(areaRepository.findById(3L)).thenReturn(Optional.of(locality));
        when(snapshotRepository.findByAreaAndFromDate(eq(3L), ArgumentMatchers.any()))
                .thenReturn(List.of());
        MarketStatsResponse res = service.getStats(3L, "bogus");
        assertEquals("5Y", res.getRange());
    }

    @Test
    void listAreas_delegatesToRepositoryWithParentId() {
        MarketArea city = MarketArea.builder()
                .id(2L)
                .level(MarketArea.Level.CITY)
                .name("Mumbai")
                .stateName("Maharashtra")
                .cityName("Mumbai")
                .active(true)
                .build();
        when(areaRepository.findFiltered(1L, null, null, MarketArea.Level.CITY))
                .thenReturn(List.of(city));

        var result = service.listAreas(1L, null, null, "CITY");

        assertEquals(1, result.size());
        assertEquals("Mumbai", result.get(0).getName());
        verify(areaRepository).findFiltered(1L, null, null, MarketArea.Level.CITY);
    }

    @Test
    void createArea_localityInheritsStateAndCityFromParent() {
        MarketArea state = MarketArea.builder()
                .id(1L)
                .level(MarketArea.Level.STATE)
                .name("Goa")
                .stateName("Goa")
                .active(true)
                .build();
        MarketArea city = MarketArea.builder()
                .id(2L)
                .parent(state)
                .level(MarketArea.Level.CITY)
                .name("Panaji")
                .stateName("Goa")
                .cityName("Panaji")
                .active(true)
                .build();
        when(areaRepository.findFirstByActiveTrueAndLevelAndStateNameIgnoreCaseAndNameIgnoreCase(
                eq(MarketArea.Level.CITY), eq("Goa"), eq("Panaji")))
                .thenReturn(Optional.of(city));
        when(areaRepository.save(any(MarketArea.class))).thenAnswer(inv -> {
            MarketArea saved = inv.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        var req = com.realestate.dto.MarketAreaCreateUpdateRequest.builder()
                .level("LOCALITY")
                .name("Altinho")
                .stateName("Goa")
                .cityName("Panaji")
                .active(true)
                .build();
        var dto = service.createArea(req);

        assertEquals("Altinho", dto.getName());
        assertEquals("Goa", dto.getStateName());
        assertEquals("Panaji", dto.getCityName());
    }

    @Test
    void createArea_cityRejected() {
        var req = com.realestate.dto.MarketAreaCreateUpdateRequest.builder()
                .level("CITY")
                .name("Orphan")
                .active(true)
                .build();
        assertThrows(com.realestate.exception.BadRequestException.class, () -> service.createArea(req));
    }
}
