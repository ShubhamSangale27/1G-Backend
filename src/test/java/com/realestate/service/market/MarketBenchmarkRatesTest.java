package com.realestate.service.market;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MarketBenchmarkRatesTest {

    private final MarketBenchmarkRates rates = new MarketBenchmarkRates();

    @Test
    void forCity_returnsKnownMetroRate() {
        assertEquals(new BigDecimal("7.20"), rates.forCity("Maharashtra", "Mumbai"));
        assertEquals(new BigDecimal("8.80"), rates.forCity("Karnataka", "Bangalore"));
    }

    @Test
    void forCity_fallsBackToStateWhenCityUnknown() {
        assertEquals(new BigDecimal("7.50"), rates.forCity("Maharashtra", "Unknown Town"));
    }

    @Test
    void forState_returnsNationalDefaultForUnknown() {
        assertEquals(0, new BigDecimal("8.5").compareTo(rates.forState("Unknown State")));
    }
}
