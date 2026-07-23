package com.realestate.service.market;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

/**
 * Default annual real-estate appreciation (CAGR) benchmarks by city and state.
 * Sourced from RBI HPI trends and major Indian market indices (2020–2026 averages).
 * Used when no admin snapshots exist for the selected locality/city.
 */
@Component
public class MarketBenchmarkRates {

    private static final BigDecimal NATIONAL_DEFAULT = new BigDecimal("8.5");

    private static final Map<String, BigDecimal> CITY_RATES = Map.ofEntries(
            Map.entry("mumbai", bd("7.2")),
            Map.entry("pune", bd("8.0")),
            Map.entry("nagpur", bd("7.0")),
            Map.entry("nashik", bd("7.5")),
            Map.entry("thane", bd("7.8")),
            Map.entry("navi mumbai", bd("7.6")),
            Map.entry("bangalore", bd("8.8")),
            Map.entry("bengaluru", bd("8.8")),
            Map.entry("mysore", bd("7.2")),
            Map.entry("mangalore", bd("7.0")),
            Map.entry("hyderabad", bd("9.2")),
            Map.entry("warangal", bd("7.8")),
            Map.entry("chennai", bd("7.5")),
            Map.entry("coimbatore", bd("7.8")),
            Map.entry("madurai", bd("6.8")),
            Map.entry("new delhi", bd("6.8")),
            Map.entry("delhi", bd("6.8")),
            Map.entry("dwarka", bd("6.5")),
            Map.entry("gurgaon", bd("7.5")),
            Map.entry("gurugram", bd("7.5")),
            Map.entry("noida", bd("7.8")),
            Map.entry("ghaziabad", bd("7.2")),
            Map.entry("faridabad", bd("7.0")),
            Map.entry("kolkata", bd("6.5")),
            Map.entry("howrah", bd("6.3")),
            Map.entry("ahmedabad", bd("8.5")),
            Map.entry("surat", bd("8.2")),
            Map.entry("vadodara", bd("7.4")),
            Map.entry("rajkot", bd("7.0")),
            Map.entry("jaipur", bd("7.6")),
            Map.entry("jodhpur", bd("7.0")),
            Map.entry("udaipur", bd("7.2")),
            Map.entry("lucknow", bd("7.4")),
            Map.entry("kanpur", bd("6.8")),
            Map.entry("agra", bd("6.5")),
            Map.entry("varanasi", bd("6.7")),
            Map.entry("patna", bd("7.0")),
            Map.entry("ranchi", bd("7.2")),
            Map.entry("jamshedpur", bd("6.8")),
            Map.entry("bhubaneswar", bd("7.5")),
            Map.entry("cuttack", bd("7.0")),
            Map.entry("chandigarh", bd("7.0")),
            Map.entry("panaji", bd("6.5")),
            Map.entry("margao", bd("6.3")),
            Map.entry("kochi", bd("7.2")),
            Map.entry("thiruvananthapuram", bd("6.8")),
            Map.entry("kozhikode", bd("6.5")),
            Map.entry("indore", bd("8.0")),
            Map.entry("bhopal", bd("7.2")),
            Map.entry("jabalpur", bd("6.8")),
            Map.entry("gwalior", bd("6.5")),
            Map.entry("visakhapatnam", bd("7.8")),
            Map.entry("vijayawada", bd("7.5")),
            Map.entry("guwahati", bd("7.0")),
            Map.entry("shillong", bd("6.5")),
            Map.entry("dehradun", bd("7.2")),
            Map.entry("haridwar", bd("7.0")),
            Map.entry("srinagar", bd("5.5")),
            Map.entry("jammu", bd("6.0")),
            Map.entry("leh", bd("4.5"))
    );

    private static final Map<String, BigDecimal> STATE_RATES = Map.ofEntries(
            Map.entry("maharashtra", bd("7.5")),
            Map.entry("karnataka", bd("8.5")),
            Map.entry("telangana", bd("8.8")),
            Map.entry("tamil nadu", bd("7.2")),
            Map.entry("delhi", bd("6.8")),
            Map.entry("gujarat", bd("8.0")),
            Map.entry("rajasthan", bd("7.2")),
            Map.entry("uttar pradesh", bd("7.0")),
            Map.entry("west bengal", bd("6.5")),
            Map.entry("madhya pradesh", bd("7.2")),
            Map.entry("kerala", bd("6.8")),
            Map.entry("punjab", bd("7.0")),
            Map.entry("haryana", bd("7.4")),
            Map.entry("bihar", bd("6.8")),
            Map.entry("odisha", bd("7.2")),
            Map.entry("andhra pradesh", bd("7.6")),
            Map.entry("goa", bd("6.3")),
            Map.entry("jharkhand", bd("7.0")),
            Map.entry("chhattisgarh", bd("6.8")),
            Map.entry("assam", bd("6.5")),
            Map.entry("uttarakhand", bd("7.0")),
            Map.entry("himachal pradesh", bd("6.2")),
            Map.entry("jammu and kashmir", bd("5.8")),
            Map.entry("ladakh", bd("4.5")),
            Map.entry("puducherry", bd("6.5"))
    );

    public BigDecimal forCity(String state, String city) {
        if (StringUtils.hasText(city)) {
            BigDecimal cityRate = CITY_RATES.get(normalize(city));
            if (cityRate != null) return cityRate;
        }
        return forState(state);
    }

    public BigDecimal forState(String state) {
        if (StringUtils.hasText(state)) {
            BigDecimal stateRate = STATE_RATES.get(normalize(state));
            if (stateRate != null) return stateRate;
        }
        return NATIONAL_DEFAULT;
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }
}
