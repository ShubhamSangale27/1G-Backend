package com.realestate.service;

import com.realestate.dto.MarketProjectionRequest;
import com.realestate.dto.MarketProjectionResponse;
import com.realestate.dto.MarketStatsResponse;
import com.realestate.entity.MarketArea;
import com.realestate.entity.MarketStatSnapshot;
import com.realestate.repository.MarketAreaRepository;
import com.realestate.repository.MarketStatSnapshotRepository;
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
    void getStats_emptyHistory_returnsUnavailableWithDefaultCagr() {
        when(areaRepository.findById(3L)).thenReturn(Optional.of(locality));
        when(snapshotRepository.findByAreaAndFromDate(eq(3L), ArgumentMatchers.any()))
                .thenReturn(List.of());

        MarketStatsResponse res = service.getStats(3L, "5Y");

        assertFalse(res.isDataAvailable());
        assertEquals(0, res.getHistory().size());
        assertEquals(new BigDecimal("8.5"), res.getDerivedCagrPct());
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

        MarketProjectionResponse res = service.project(MarketProjectionRequest.builder()
                .areaId(3L)
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
}
