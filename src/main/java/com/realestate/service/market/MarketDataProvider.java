package com.realestate.service.market;

import com.realestate.entity.MarketArea;

import java.time.LocalDate;
import java.util.List;

/**
 * Pluggable provider for public/open market history.
 * Implementations may load RBI HPI seed files or other free datasets.
 */
public interface MarketDataProvider {

    String getName();

    List<SupportedArea> listSupportedAreas();

    List<SeriesPoint> fetchAreaSeries(String areaKey, LocalDate from, LocalDate to);

    record SupportedArea(String areaKey, String stateName, String cityName, String locationName) {}

    record SeriesPoint(LocalDate date, java.math.BigDecimal priceIndex,
                       java.math.BigDecimal avgPricePerSqft, java.math.BigDecimal yoyGrowthPct) {}
}
