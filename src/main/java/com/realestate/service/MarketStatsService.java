package com.realestate.service;

import com.realestate.dto.*;
import com.realestate.entity.MarketArea;
import com.realestate.entity.MarketStatSnapshot;
import com.realestate.exception.BadRequestException;
import com.realestate.exception.ResourceNotFoundException;
import com.realestate.repository.MarketAreaRepository;
import com.realestate.repository.MarketStatSnapshotRepository;
import com.realestate.service.market.MarketBenchmarkRates;
import com.realestate.service.market.MarketDataProvider;
import com.realestate.service.market.RbiHpiSeedProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarketStatsService {

    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);

    private final MarketAreaRepository areaRepository;
    private final MarketStatSnapshotRepository snapshotRepository;
    private final RbiHpiSeedProvider rbiHpiSeedProvider;
    private final MarketBenchmarkRates benchmarkRates;

    public List<MarketAreaDto> listAreas(Long parentId, String state, String city, String level) {
        MarketArea.Level lvl = parseLevelOrNull(level);
        String st = blankToNull(state);
        String ct = blankToNull(city);
        if (lvl == MarketArea.Level.LOCALITY && st != null && ct != null) {
            return findLocalitiesForCity(st, ct).stream()
                    .map(MarketAreaDto::from)
                    .collect(Collectors.toList());
        }
        return areaRepository.findFiltered(parentId, st, ct, lvl).stream()
                .map(MarketAreaDto::from)
                .collect(Collectors.toList());
    }

    public List<MarketAreaDto> listLocalitiesAdmin(String state, String city) {
        String st = blankToNull(state);
        String ct = blankToNull(city);
        if (st == null || ct == null) {
            return areaRepository.findAllByOrderByLevelAscSortOrderAscNameAsc().stream()
                    .filter(a -> a.getLevel() == MarketArea.Level.LOCALITY)
                    .map(MarketAreaDto::from)
                    .collect(Collectors.toList());
        }
        return findLocalitiesForCity(st, ct).stream()
                .map(MarketAreaDto::from)
                .collect(Collectors.toList());
    }

    /** Resolves admin localities by state/city fields, falling back to city parent hierarchy. */
    private List<MarketArea> findLocalitiesForCity(String state, String city) {
        List<MarketArea> byFields = areaRepository
                .findByActiveTrueAndLevelAndStateNameIgnoreCaseAndCityNameIgnoreCase(
                        MarketArea.Level.LOCALITY, state, city);
        if (!byFields.isEmpty()) {
            return byFields;
        }
        return areaRepository
                .findFirstByActiveTrueAndLevelAndStateNameIgnoreCaseAndNameIgnoreCase(
                        MarketArea.Level.CITY, state, city)
                .map(cityArea -> areaRepository.findByActiveTrueAndParentIdOrderBySortOrderAscNameAsc(cityArea.getId()))
                .orElse(List.of());
    }

    public List<MarketAreaDto> listAllAreasAdmin() {
        return areaRepository.findAllByOrderByLevelAscSortOrderAscNameAsc().stream()
                .filter(a -> a.getLevel() == MarketArea.Level.LOCALITY)
                .map(MarketAreaDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public MarketAreaDto createArea(MarketAreaCreateUpdateRequest req) {
        MarketArea.Level level = parseLevel(req.getLevel());
        if (level != MarketArea.Level.LOCALITY) {
            throw new BadRequestException("Only LOCALITY areas can be created by admin. States and cities are static.");
        }

        MarketArea parent = resolveLocalityParent(req);
        validateHierarchy(level, parent);

        String name = req.getName().trim();
        String stateName = resolveStateName(level, parent, req.getStateName(), name);
        String cityName = resolveCityName(level, parent, req.getCityName(), name);

        MarketArea area = MarketArea.builder()
                .parent(parent)
                .level(level)
                .name(name)
                .stateName(stateName)
                .cityName(cityName)
                .stateSlug(slugify(stateName))
                .citySlug(slugify(cityName))
                .locationSlug(slugify(name))
                .active(req.getActive() == null || req.getActive())
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                .build();
        return MarketAreaDto.from(areaRepository.save(area));
    }

    @Transactional
    public MarketAreaDto updateArea(Long id, MarketAreaCreateUpdateRequest req) {
        MarketArea area = areaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MarketArea", id));
        if (area.getLevel() != MarketArea.Level.LOCALITY) {
            throw new BadRequestException("Only LOCALITY areas can be edited. States and cities are static.");
        }
        MarketArea.Level level = MarketArea.Level.LOCALITY;
        MarketArea parent = resolveLocalityParent(req);
        validateHierarchy(level, parent);

        String name = req.getName().trim();
        String stateName = resolveStateName(level, parent, req.getStateName(), name);
        String cityName = resolveCityName(level, parent, req.getCityName(), name);

        area.setParent(parent);
        area.setLevel(level);
        area.setName(name);
        area.setStateName(stateName);
        area.setCityName(cityName);
        area.setStateSlug(slugify(stateName));
        area.setCitySlug(slugify(cityName));
        area.setLocationSlug(slugify(name));
        if (req.getActive() != null) area.setActive(req.getActive());
        if (req.getSortOrder() != null) area.setSortOrder(req.getSortOrder());
        return MarketAreaDto.from(areaRepository.save(area));
    }

    @Transactional
    public void deleteArea(Long id) {
        if (!areaRepository.existsById(id)) {
            throw new ResourceNotFoundException("MarketArea", id);
        }
        areaRepository.deleteById(id);
    }

    public List<MarketStatSnapshotDto> listSnapshots(Long areaId) {
        return snapshotRepository.findByMarketAreaIdOrderBySnapshotDateAsc(areaId).stream()
                .map(MarketStatSnapshotDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public MarketStatSnapshotDto createSnapshot(MarketStatSnapshotCreateUpdateRequest req) {
        validateSnapshotDate(req.getSnapshotDate());
        MarketArea area = areaRepository.findById(req.getMarketAreaId())
                .orElseThrow(() -> new ResourceNotFoundException("MarketArea", req.getMarketAreaId()));
        MarketStatSnapshot.Granularity granularity = parseGranularity(req.getGranularity());
        ensureUniqueSnapshot(req.getMarketAreaId(), req.getSnapshotDate(), granularity, null);
        BigDecimal priceIndex = resolvePriceIndexForSave(
                req.getMarketAreaId(), req.getSnapshotDate(), req.getPriceIndex(), req.getAvgPricePerSqft(), null);
        MarketStatSnapshot snap = MarketStatSnapshot.builder()
                .marketArea(area)
                .snapshotDate(req.getSnapshotDate())
                .granularity(granularity)
                .priceIndex(priceIndex)
                .avgPricePerSqft(req.getAvgPricePerSqft())
                .yoyGrowthPct(req.getYoyGrowthPct())
                .transactionVolume(req.getTransactionVolume())
                .rentalYieldPct(req.getRentalYieldPct())
                .sourceType(parseSourceType(req.getSourceType()))
                .sourceLabel(blankToNull(req.getSourceLabel()))
                .confidenceScore(req.getConfidenceScore())
                .build();
        return MarketStatSnapshotDto.from(snapshotRepository.save(snap));
    }

    @Transactional
    public MarketStatSnapshotDto updateSnapshot(Long id, MarketStatSnapshotCreateUpdateRequest req) {
        validateSnapshotDate(req.getSnapshotDate());
        MarketStatSnapshot snap = snapshotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MarketStatSnapshot", id));
        MarketArea area = areaRepository.findById(req.getMarketAreaId())
                .orElseThrow(() -> new ResourceNotFoundException("MarketArea", req.getMarketAreaId()));
        MarketStatSnapshot.Granularity granularity = parseGranularity(req.getGranularity());
        ensureUniqueSnapshot(req.getMarketAreaId(), req.getSnapshotDate(), granularity, id);
        BigDecimal priceIndex = resolvePriceIndexForSave(
                req.getMarketAreaId(), req.getSnapshotDate(), req.getPriceIndex(), req.getAvgPricePerSqft(), id);
        snap.setMarketArea(area);
        snap.setSnapshotDate(req.getSnapshotDate());
        snap.setGranularity(granularity);
        snap.setPriceIndex(priceIndex);
        snap.setAvgPricePerSqft(req.getAvgPricePerSqft());
        snap.setYoyGrowthPct(req.getYoyGrowthPct());
        snap.setTransactionVolume(req.getTransactionVolume());
        snap.setRentalYieldPct(req.getRentalYieldPct());
        snap.setSourceType(parseSourceType(req.getSourceType()));
        snap.setSourceLabel(blankToNull(req.getSourceLabel()));
        snap.setConfidenceScore(req.getConfidenceScore());
        return MarketStatSnapshotDto.from(snapshotRepository.save(snap));
    }

    @Transactional
    public void deleteSnapshot(Long id) {
        if (!snapshotRepository.existsById(id)) {
            throw new ResourceNotFoundException("MarketStatSnapshot", id);
        }
        snapshotRepository.deleteById(id);
    }

    @Transactional
    public int importRbiSeed() {
        int upserted = 0;
        for (MarketDataProvider.SupportedArea sa : rbiHpiSeedProvider.listSupportedAreas()) {
            MarketArea state = upsertArea(null, MarketArea.Level.STATE, sa.stateName(), sa.stateName(), null);
            MarketArea city = upsertArea(state, MarketArea.Level.CITY, sa.cityName(), sa.stateName(), sa.cityName());
            MarketArea locality = upsertArea(city, MarketArea.Level.LOCALITY, sa.locationName(), sa.stateName(), sa.cityName());

            for (MarketDataProvider.SeriesPoint p : rbiHpiSeedProvider.fetchAreaSeries(sa.areaKey(), null, null)) {
                var existing = snapshotRepository.findByMarketAreaIdAndSnapshotDateAndGranularity(
                        locality.getId(), p.date(), MarketStatSnapshot.Granularity.QUARTERLY);
                if (existing.isPresent()) {
                    MarketStatSnapshot snap = existing.get();
                    snap.setPriceIndex(p.priceIndex());
                    snap.setAvgPricePerSqft(p.avgPricePerSqft());
                    snap.setYoyGrowthPct(p.yoyGrowthPct());
                    snap.setSourceType(MarketStatSnapshot.SourceType.RBI_SEED);
                    snap.setSourceLabel(rbiHpiSeedProvider.getName());
                    snapshotRepository.save(snap);
                } else {
                    snapshotRepository.save(MarketStatSnapshot.builder()
                            .marketArea(locality)
                            .snapshotDate(p.date())
                            .granularity(MarketStatSnapshot.Granularity.QUARTERLY)
                            .priceIndex(p.priceIndex())
                            .avgPricePerSqft(p.avgPricePerSqft())
                            .yoyGrowthPct(p.yoyGrowthPct())
                            .sourceType(MarketStatSnapshot.SourceType.RBI_SEED)
                            .sourceLabel(rbiHpiSeedProvider.getName())
                            .confidenceScore(new BigDecimal("0.80"))
                            .build());
                }
                upserted++;
            }
        }
        return upserted;
    }

    @Transactional(readOnly = true)
    public MarketStatsResponse getStats(Long areaId, String range) {
        MarketArea area = areaRepository.findById(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("MarketArea", areaId));
        return buildStatsForArea(area, range);
    }

    @Transactional(readOnly = true)
    public MarketStatsResponse getStatsByLocation(String state, String city, Long localityId, String range) {
        String st = requireText(state, "state");
        String ct = requireText(city, "city");
        if (localityId != null) {
            MarketArea locality = areaRepository.findById(localityId)
                    .orElseThrow(() -> new ResourceNotFoundException("MarketArea", localityId));
            if (locality.getLevel() != MarketArea.Level.LOCALITY) {
                throw new BadRequestException("localityId must reference a LOCALITY area");
            }
            return buildStatsForArea(locality, range);
        }
        return buildStatsForCity(st, ct, range);
    }

    private MarketStatsResponse buildStatsForCity(String state, String city, String range) {
        String normalizedRange = normalizeRange(range);
        LocalDate from = rangeStart(normalizedRange);
        List<MarketStatSnapshot> snaps = collectSnapshotsForCity(state, city, from);
        if (snaps.isEmpty()) {
            return buildBenchmarkStats(state, city, range);
        }
        MarketArea virtual = MarketArea.builder()
                .level(MarketArea.Level.CITY)
                .name(city)
                .stateName(state)
                .cityName(city)
                .active(true)
                .build();
        return buildStatsFromSnapshots(virtual, snaps, normalizedRange, benchmarkRates.forCity(state, city));
    }

    private MarketStatsResponse buildStatsForArea(MarketArea area, String range) {
        String normalizedRange = normalizeRange(range);
        LocalDate from = rangeStart(normalizedRange);
        List<MarketStatSnapshot> snaps = collectSnapshotsForArea(area, from);
        if (snaps.isEmpty()) {
            BigDecimal benchmark = benchmarkForArea(area);
            return MarketStatsResponse.builder()
                    .area(MarketAreaDto.from(area))
                    .range(normalizedRange)
                    .dataAvailable(false)
                    .message("No snapshots for this area yet. Using market average of "
                            + benchmark + "% p.a. for " + areaLabel(area) + ".")
                    .derivedCagrPct(benchmark)
                    .history(List.of())
                    .build();
        }
        return buildStatsFromSnapshots(area, snaps, normalizedRange, benchmarkForArea(area));
    }

    private MarketStatsResponse buildStatsFromSnapshots(MarketArea area, List<MarketStatSnapshot> snaps,
                                                        String normalizedRange, BigDecimal benchmarkFallback) {
        snaps = sortSnapshots(snaps);
        MarketStatSnapshot first = snaps.get(0);
        MarketStatSnapshot last = snaps.get(snaps.size() - 1);
        BigDecimal regionalRate = deriveRegionalRateFromSnapshots(snaps, benchmarkFallback);
        BigDecimal firstIdx = effectiveIndex(first, snaps);
        BigDecimal lastIdx = effectiveIndex(last, snaps);
        BigDecimal rangeReturn = percentChange(firstIdx, lastIdx);

        List<MarketStatsResponse.HistoryPoint> history = snaps.stream()
                .map(s -> MarketStatsResponse.HistoryPoint.builder()
                        .date(s.getSnapshotDate())
                        .index(effectiveIndex(s, snaps))
                        .avgPricePerSqft(s.getAvgPricePerSqft())
                        .yoyGrowthPct(s.getYoyGrowthPct())
                        .build())
                .collect(Collectors.toList());

        return MarketStatsResponse.builder()
                .area(MarketAreaDto.from(area))
                .range(normalizedRange)
                .dataAvailable(true)
                .message("Based on " + snaps.size() + " admin snapshot(s) from "
                        + first.getSnapshotDate() + " to " + last.getSnapshotDate() + ".")
                .latestIndex(lastIdx)
                .latestAvgPricePerSqft(last.getAvgPricePerSqft())
                .latestRentalYieldPct(last.getRentalYieldPct())
                .rangeReturnPct(rangeReturn)
                .derivedCagrPct(regionalRate)
                .coverageFrom(first.getSnapshotDate())
                .coverageTo(last.getSnapshotDate())
                .history(history)
                .build();
    }

    private MarketStatsResponse buildBenchmarkStats(String state, String city, String range) {
        String normalizedRange = normalizeRange(range);
        BigDecimal rate = benchmarkRates.forCity(state, city);
        MarketArea virtual = MarketArea.builder()
                .level(MarketArea.Level.CITY)
                .name(city)
                .stateName(state)
                .cityName(city)
                .active(true)
                .build();
        return MarketStatsResponse.builder()
                .area(MarketAreaDto.from(virtual))
                .range(normalizedRange)
                .dataAvailable(false)
                .message("Using market average growth rate of " + rate + "% p.a. for "
                        + city + ", " + state + ". Add localities and snapshots in admin for precise data.")
                .derivedCagrPct(rate)
                .history(List.of())
                .build();
    }

    @Transactional(readOnly = true)
    public MarketProjectionResponse project(MarketProjectionRequest req) {
        String state = requireText(req.getState(), "state");
        String city = requireText(req.getCity(), "city");
        Long localityId = req.getLocalityId() != null ? req.getLocalityId() : req.getAreaId();
        String range = normalizeRange(req.getRange());

        MarketStatsResponse market = localityId != null
                ? getStats(localityId, range)
                : getStatsByLocation(state, city, null, range);

        LocalDate from = rangeStart(range);
        List<MarketStatSnapshot> snaps = resolveSnapshots(state, city, localityId, from);
        if (snaps.isEmpty() && market.isDataAvailable()) {
            snaps = resolveSnapshots(state, city, localityId, null);
        }

        BigDecimal benchmark = benchmarkRates.forCity(state, city);
        BigDecimal regionalRate = !snaps.isEmpty()
                ? deriveRegionalRateFromSnapshots(snaps, benchmark)
                : (market.getDerivedCagrPct() != null ? market.getDerivedCagrPct() : benchmark);
        BigDecimal userRate = req.getExpectedRatePct() != null
                ? req.getExpectedRatePct() : regionalRate;

        int years = Math.max(1, Math.min(req.getYears(), 30));
        BigDecimal initial = req.getInitialAmount();
        BigDecimal monthly = req.getMonthlyContribution() != null
                ? req.getMonthlyContribution() : BigDecimal.ZERO;

        List<MarketProjectionResponse.ProjectionPoint> points;
        if (!snaps.isEmpty()) {
            points = buildProjectionFromSnapshots(snaps, initial, monthly, userRate, regionalRate, years);
        } else {
            points = new ArrayList<>();
            for (int y = 0; y <= years; y++) {
                points.add(MarketProjectionResponse.ProjectionPoint.builder()
                        .year(y)
                        .regional(lumpSumFuture(initial, regionalRate, y))
                        .user(portfolioFuture(initial, monthly, userRate, y))
                        .forecast(y > 0)
                        .build());
            }
        }

        MarketProjectionResponse.ProjectionPoint last = points.get(points.size() - 1);
        return MarketProjectionResponse.builder()
                .market(market)
                .regionalRatePct(regionalRate)
                .userRatePct(userRate)
                .regionalFinal(last.getRegional())
                .userFinal(last.getUser())
                .points(points)
                .build();
    }

    private MarketArea upsertArea(MarketArea parent, MarketArea.Level level, String name,
                                  String stateName, String cityName) {
        var existing = parent == null
                ? areaRepository.findByLevelAndNameIgnoreCaseAndParentIsNull(level, name)
                : areaRepository.findByLevelAndNameIgnoreCaseAndParentId(level, name, parent.getId());
        if (existing.isPresent()) {
            MarketArea area = existing.get();
            area.setStateName(stateName);
            if (level != MarketArea.Level.STATE) {
                area.setCityName(cityName);
            }
            area.setStateSlug(slugify(stateName));
            area.setCitySlug(level == MarketArea.Level.STATE ? null : slugify(cityName));
            area.setLocationSlug(level == MarketArea.Level.LOCALITY ? slugify(name) : null);
            area.setActive(true);
            return areaRepository.save(area);
        }
        MarketArea area = MarketArea.builder()
                .parent(parent)
                .level(level)
                .name(name)
                .stateName(stateName)
                .cityName(level == MarketArea.Level.STATE ? null : cityName)
                .stateSlug(slugify(stateName))
                .citySlug(level == MarketArea.Level.STATE ? null : slugify(cityName))
                .locationSlug(level == MarketArea.Level.LOCALITY ? slugify(name) : null)
                .active(true)
                .sortOrder(0)
                .build();
        return areaRepository.save(area);
    }

    static BigDecimal lumpSumFuture(BigDecimal principal, BigDecimal annualPct, int years) {
        if (years <= 0) return principal.setScale(2, RoundingMode.HALF_UP);
        double r = annualPct.doubleValue() / 100.0;
        double fv = principal.doubleValue() * Math.pow(1 + r, years);
        return BigDecimal.valueOf(fv).setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal portfolioFuture(BigDecimal principal, BigDecimal monthly,
                                      BigDecimal annualPct, int years) {
        if (years <= 0) return principal.setScale(2, RoundingMode.HALF_UP);
        double rMonthly = annualPct.doubleValue() / 100.0 / 12.0;
        int n = years * 12;
        double p = principal.doubleValue();
        double m = monthly.doubleValue();
        double fv = p * Math.pow(1 + rMonthly, n);
        if (rMonthly != 0) {
            fv += m * ((Math.pow(1 + rMonthly, n) - 1) / rMonthly);
        } else {
            fv += m * n;
        }
        return BigDecimal.valueOf(fv).setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal computeCagr(BigDecimal start, BigDecimal end, LocalDate from, LocalDate to) {
        if (start == null || end == null || start.compareTo(BigDecimal.ZERO) <= 0
                || end.compareTo(BigDecimal.ZERO) <= 0 || from == null || to == null) {
            return null;
        }
        double years = ChronoUnit.DAYS.between(from, to) / 365.25;
        if (years < 0.25) {
            return null;
        }
        double cagr = (Math.pow(end.doubleValue() / start.doubleValue(), 1.0 / years) - 1.0) * 100.0;
        return BigDecimal.valueOf(cagr).setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal percentChange(BigDecimal start, BigDecimal end) {
        if (start == null || end == null || start.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return end.subtract(start).multiply(BigDecimal.valueOf(100), MC)
                .divide(start, MC).setScale(2, RoundingMode.HALF_UP);
    }

    private static LocalDate rangeStart(String range) {
        LocalDate today = LocalDate.now();
        return switch (range) {
            case "YTD" -> LocalDate.of(today.getYear(), 1, 1);
            case "1Y" -> today.minusYears(1);
            case "3Y" -> today.minusYears(3);
            case "5Y" -> today.minusYears(5);
            case "10Y" -> today.minusYears(10);
            default -> null; // MAX
        };
    }

    private static String normalizeRange(String range) {
        if (!StringUtils.hasText(range)) return "5Y";
        String r = range.trim().toUpperCase(Locale.ROOT);
        return switch (r) {
            case "YTD", "1Y", "3Y", "5Y", "10Y", "MAX" -> r;
            default -> "5Y";
        };
    }

    private static void validateHierarchy(MarketArea.Level level, MarketArea parent) {
        if (level == MarketArea.Level.STATE && parent != null) {
            throw new BadRequestException("STATE areas must not have a parent");
        }
        if (level == MarketArea.Level.CITY && (parent == null || parent.getLevel() != MarketArea.Level.STATE)) {
            throw new BadRequestException("CITY areas require a STATE parent");
        }
        if (level == MarketArea.Level.LOCALITY && (parent == null || parent.getLevel() != MarketArea.Level.CITY)) {
            throw new BadRequestException("LOCALITY areas require a CITY parent");
        }
    }

    private static String resolveStateName(MarketArea.Level level, MarketArea parent,
                                           String requested, String name) {
        if (level == MarketArea.Level.STATE) return name;
        if (StringUtils.hasText(requested)) return requested.trim();
        if (parent != null && StringUtils.hasText(parent.getStateName())) return parent.getStateName();
        return name;
    }

    private static String resolveCityName(MarketArea.Level level, MarketArea parent,
                                          String requested, String name) {
        if (level == MarketArea.Level.STATE) return null;
        if (level == MarketArea.Level.CITY) return name;
        if (StringUtils.hasText(requested)) return requested.trim();
        if (parent != null && StringUtils.hasText(parent.getCityName())) return parent.getCityName();
        if (parent != null) return parent.getName();
        return name;
    }

    private static MarketArea.Level parseLevel(String level) {
        try {
            return MarketArea.Level.valueOf(level.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new BadRequestException("Invalid level. Use STATE, CITY, or LOCALITY");
        }
    }

    private static MarketArea.Level parseLevelOrNull(String level) {
        if (!StringUtils.hasText(level)) return null;
        return parseLevel(level);
    }

    private static MarketStatSnapshot.Granularity parseGranularity(String g) {
        if (!StringUtils.hasText(g)) return MarketStatSnapshot.Granularity.QUARTERLY;
        try {
            return MarketStatSnapshot.Granularity.valueOf(g.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new BadRequestException("Invalid granularity");
        }
    }

    private static MarketStatSnapshot.SourceType parseSourceType(String s) {
        if (!StringUtils.hasText(s)) return MarketStatSnapshot.SourceType.ADMIN;
        try {
            return MarketStatSnapshot.SourceType.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new BadRequestException("Invalid sourceType");
        }
    }

    private static String slugify(String value) {
        if (!StringUtils.hasText(value)) return null;
        return value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private static String blankToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    private List<MarketStatSnapshot> findSnapshotsWithParentFallback(Long areaId, LocalDate from) {
        List<MarketStatSnapshot> snaps = snapshotRepository.findByAreaAndFromDate(areaId, from);
        Long fallbackId = areaRepository.findById(areaId)
                .map(MarketArea::getParent)
                .map(MarketArea::getId)
                .orElse(null);
        while (snaps.isEmpty() && fallbackId != null) {
            snaps = snapshotRepository.findByAreaAndFromDate(fallbackId, from);
            if (snaps.isEmpty()) {
                fallbackId = areaRepository.findById(fallbackId)
                        .map(MarketArea::getParent)
                        .map(MarketArea::getId)
                        .orElse(null);
            }
        }
        return snaps;
    }

    private MarketArea resolveLocalityParent(MarketAreaCreateUpdateRequest req) {
        if (req.getParentId() != null) {
            MarketArea parent = areaRepository.findById(req.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("MarketArea", req.getParentId()));
            if (parent.getLevel() != MarketArea.Level.CITY) {
                throw new BadRequestException("LOCALITY areas require a CITY parent");
            }
            return parent;
        }
        String stateName = requireText(req.getStateName(), "stateName");
        String cityName = requireText(req.getCityName(), "cityName");
        return ensureCityArea(stateName, cityName);
    }

    private MarketArea ensureCityArea(String stateName, String cityName) {
        return areaRepository
                .findFirstByActiveTrueAndLevelAndStateNameIgnoreCaseAndNameIgnoreCase(
                        MarketArea.Level.CITY, stateName, cityName)
                .orElseGet(() -> {
                    MarketArea state = ensureStateArea(stateName);
                    return areaRepository.save(MarketArea.builder()
                            .parent(state)
                            .level(MarketArea.Level.CITY)
                            .name(cityName)
                            .stateName(stateName)
                            .cityName(cityName)
                            .stateSlug(slugify(stateName))
                            .citySlug(slugify(cityName))
                            .active(true)
                            .sortOrder(0)
                            .build());
                });
    }

    private MarketArea ensureStateArea(String stateName) {
        return areaRepository
                .findFirstByActiveTrueAndLevelAndStateNameIgnoreCaseAndNameIgnoreCase(
                        MarketArea.Level.STATE, stateName, stateName)
                .orElseGet(() -> areaRepository.save(MarketArea.builder()
                        .level(MarketArea.Level.STATE)
                        .name(stateName)
                        .stateName(stateName)
                        .stateSlug(slugify(stateName))
                        .active(true)
                        .sortOrder(0)
                        .build()));
    }

    private BigDecimal benchmarkForArea(MarketArea area) {
        if (area.getLevel() == MarketArea.Level.LOCALITY) {
            return benchmarkRates.forCity(area.getStateName(), area.getCityName());
        }
        if (area.getLevel() == MarketArea.Level.CITY) {
            return benchmarkRates.forCity(area.getStateName(), area.getName());
        }
        return benchmarkRates.forState(area.getStateName());
    }

    private static String areaLabel(MarketArea area) {
        if (area.getLevel() == MarketArea.Level.LOCALITY) {
            return area.getName() + ", " + area.getCityName();
        }
        return area.getName();
    }

    private static String requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(field + " is required");
        }
        return value.trim();
    }

    private static void validateSnapshotDate(LocalDate date) {
        if (date == null || !date.isBefore(LocalDate.now())) {
            throw new BadRequestException("Snapshot date must be a past date (before today)");
        }
    }

    private List<MarketStatSnapshot> collectSnapshotsForArea(MarketArea area, LocalDate from) {
        List<MarketStatSnapshot> snaps = findSnapshotsWithParentFallback(area.getId(), from);
        if (snaps.isEmpty() && from != null) {
            snaps = findSnapshotsWithParentFallback(area.getId(), null);
        }
        return sortSnapshots(snaps);
    }

    private List<MarketStatSnapshot> collectSnapshotsForCity(String state, String city, LocalDate from) {
        List<MarketStatSnapshot> merged = new ArrayList<>();
        for (MarketArea loc : findLocalitiesForCity(state, city)) {
            merged.addAll(collectSnapshotsForArea(loc, from));
        }
        areaRepository.findFirstByActiveTrueAndLevelAndStateNameIgnoreCaseAndNameIgnoreCase(
                        MarketArea.Level.CITY, state, city)
                .ifPresent(cityArea -> merged.addAll(collectSnapshotsForArea(cityArea, from)));
        if (merged.isEmpty() && from != null) {
            return collectSnapshotsForCity(state, city, null);
        }
        return mergeSnapshotsByDate(merged);
    }

    private List<MarketStatSnapshot> resolveSnapshots(String state, String city, Long localityId, LocalDate from) {
        if (localityId != null) {
            MarketArea area = areaRepository.findById(localityId)
                    .orElseThrow(() -> new ResourceNotFoundException("MarketArea", localityId));
            return collectSnapshotsForArea(area, from);
        }
        return collectSnapshotsForCity(state, city, from);
    }

    private static List<MarketStatSnapshot> sortSnapshots(List<MarketStatSnapshot> snaps) {
        return snaps.stream()
                .sorted(Comparator.comparing(MarketStatSnapshot::getSnapshotDate))
                .collect(Collectors.toList());
    }

    private static List<MarketStatSnapshot> mergeSnapshotsByDate(List<MarketStatSnapshot> snaps) {
        if (snaps.isEmpty()) {
            return List.of();
        }
        Map<LocalDate, List<MarketStatSnapshot>> grouped = snaps.stream()
                .collect(Collectors.groupingBy(MarketStatSnapshot::getSnapshotDate, LinkedHashMap::new, Collectors.toList()));
        List<MarketStatSnapshot> merged = new ArrayList<>();
        for (Map.Entry<LocalDate, List<MarketStatSnapshot>> entry : grouped.entrySet()) {
            List<MarketStatSnapshot> group = entry.getValue();
            if (group.size() == 1) {
                merged.add(group.get(0));
                continue;
            }
            BigDecimal avgIndex = averageDecimal(group.stream().map(MarketStatSnapshot::getPriceIndex).toList());
            BigDecimal avgYoy = averageDecimal(group.stream().map(MarketStatSnapshot::getYoyGrowthPct).toList());
            BigDecimal avgPsf = averageDecimal(group.stream().map(MarketStatSnapshot::getAvgPricePerSqft).toList());
            BigDecimal avgYield = averageDecimal(group.stream().map(MarketStatSnapshot::getRentalYieldPct).toList());
            MarketStatSnapshot sample = group.get(0);
            merged.add(MarketStatSnapshot.builder()
                    .marketArea(sample.getMarketArea())
                    .snapshotDate(entry.getKey())
                    .granularity(sample.getGranularity())
                    .priceIndex(avgIndex)
                    .avgPricePerSqft(avgPsf)
                    .yoyGrowthPct(avgYoy)
                    .rentalYieldPct(avgYield)
                    .sourceType(sample.getSourceType())
                    .sourceLabel(sample.getSourceLabel())
                    .build());
        }
        return sortSnapshots(merged);
    }

    private static BigDecimal averageDecimal(List<BigDecimal> values) {
        List<BigDecimal> present = values.stream()
                .filter(v -> v != null)
                .toList();
        if (present.isEmpty()) {
            return null;
        }
        BigDecimal sum = present.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(present.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal deriveRegionalRateFromSnapshots(List<MarketStatSnapshot> snaps, BigDecimal benchmarkFallback) {
        if (snaps == null || snaps.isEmpty()) {
            return benchmarkFallback;
        }
        snaps = sortSnapshots(snaps);
        if (snaps.size() >= 2) {
            MarketStatSnapshot first = snaps.get(0);
            MarketStatSnapshot last = snaps.get(snaps.size() - 1);
            BigDecimal cagr = computeCagr(
                    effectiveIndex(first, snaps),
                    effectiveIndex(last, snaps),
                    first.getSnapshotDate(),
                    last.getSnapshotDate());
            if (cagr != null && cagr.compareTo(BigDecimal.ZERO) != 0) {
                return cagr;
            }
        }
        BigDecimal avgYoy = averageDecimal(snaps.stream().map(MarketStatSnapshot::getYoyGrowthPct).toList());
        if (avgYoy != null) {
            return avgYoy.setScale(2, RoundingMode.HALF_UP);
        }
        return benchmarkFallback;
    }

    private List<MarketProjectionResponse.ProjectionPoint> buildProjectionFromSnapshots(
            List<MarketStatSnapshot> snaps,
            BigDecimal initial,
            BigDecimal monthly,
            BigDecimal userRate,
            BigDecimal regionalRate,
            int horizonYears) {
        snaps = sortSnapshots(snaps);
        BigDecimal baseIndex = effectiveIndex(snaps.get(0), snaps);
        LocalDate firstDate = snaps.get(0).getSnapshotDate();
        LocalDate lastDate = snaps.get(snaps.size() - 1).getSnapshotDate();
        double spanYears = Math.max(0.25, ChronoUnit.DAYS.between(firstDate, lastDate) / 365.25);
        double histCap = Math.min(spanYears, horizonYears);
        double scale = spanYears > histCap ? histCap / spanYears : 1.0;

        Map<Integer, MarketProjectionResponse.ProjectionPoint> pointsByYear = new LinkedHashMap<>();
        pointsByYear.put(0, MarketProjectionResponse.ProjectionPoint.builder()
                .year(0)
                .regional(initial.setScale(2, RoundingMode.HALF_UP))
                .user(portfolioFuture(initial, monthly, userRate, 0))
                .forecast(false)
                .build());
        for (MarketStatSnapshot snap : snaps) {
            double offsetYears = ChronoUnit.DAYS.between(firstDate, snap.getSnapshotDate()) / 365.25;
            int year = (int) Math.round(offsetYears * scale);
            year = Math.max(0, Math.min(year, horizonYears));
            BigDecimal regional = initial.multiply(effectiveIndex(snap, snaps), MC).divide(baseIndex, MC)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal user = portfolioFuture(initial, monthly, userRate, year);
            pointsByYear.put(year, MarketProjectionResponse.ProjectionPoint.builder()
                    .year(year)
                    .regional(regional)
                    .user(user)
                    .forecast(false)
                    .build());
        }

        if (pointsByYear.isEmpty()) {
            pointsByYear.put(0, MarketProjectionResponse.ProjectionPoint.builder()
                    .year(0)
                    .regional(initial.setScale(2, RoundingMode.HALF_UP))
                    .user(portfolioFuture(initial, monthly, userRate, 0))
                    .forecast(false)
                    .build());
        }

        int lastHistYear = pointsByYear.keySet().stream().max(Integer::compareTo).orElse(0);
        BigDecimal lastRegional = pointsByYear.get(lastHistYear).getRegional();
        for (int y = lastHistYear + 1; y <= horizonYears; y++) {
            int forwardYears = y - lastHistYear;
            pointsByYear.put(y, MarketProjectionResponse.ProjectionPoint.builder()
                    .year(y)
                    .regional(lumpSumFuture(lastRegional, regionalRate, forwardYears))
                    .user(portfolioFuture(initial, monthly, userRate, y))
                    .forecast(true)
                    .build());
        }
        return new ArrayList<>(pointsByYear.values());
    }

    /**
     * When admin snapshots keep the default price index (100) but supply avg ₹/sqft,
     * derive a normalized index from the first snapshot's price so CAGR and charts reflect real growth.
     */
    private static BigDecimal effectiveIndex(MarketStatSnapshot snap, List<MarketStatSnapshot> snaps) {
        if (snap == null) {
            return BigDecimal.ZERO;
        }
        if (snaps != null && !snaps.isEmpty() && !priceIndicesAreInformative(snaps)) {
            BigDecimal baselinePsf = baselineAvgPricePerSqft(snaps);
            if (baselinePsf != null
                    && snap.getAvgPricePerSqft() != null
                    && snap.getAvgPricePerSqft().compareTo(BigDecimal.ZERO) > 0) {
                return snap.getAvgPricePerSqft()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(baselinePsf, 4, RoundingMode.HALF_UP);
            }
        }
        return snap.getPriceIndex() != null ? snap.getPriceIndex() : BigDecimal.ZERO;
    }

    private static boolean priceIndicesAreInformative(List<MarketStatSnapshot> snaps) {
        BigDecimal first = null;
        for (MarketStatSnapshot s : snaps) {
            if (s.getPriceIndex() == null) {
                continue;
            }
            if (first == null) {
                first = s.getPriceIndex();
            } else if (s.getPriceIndex().compareTo(first) != 0) {
                return true;
            }
        }
        return false;
    }

    private static BigDecimal baselineAvgPricePerSqft(List<MarketStatSnapshot> snaps) {
        for (MarketStatSnapshot s : snaps) {
            if (s.getAvgPricePerSqft() != null && s.getAvgPricePerSqft().compareTo(BigDecimal.ZERO) > 0) {
                return s.getAvgPricePerSqft();
            }
        }
        return null;
    }

    /**
     * When avg ₹/sqft is supplied, store a normalized price index (earliest psf in the series = 100).
     */
    private BigDecimal resolvePriceIndexForSave(Long areaId, LocalDate snapshotDate, BigDecimal requestedIndex,
                                                BigDecimal avgPsf, Long excludeSnapshotId) {
        if (avgPsf == null || avgPsf.compareTo(BigDecimal.ZERO) <= 0) {
            return requestedIndex != null ? requestedIndex : BigDecimal.valueOf(100);
        }
        BigDecimal baseline = snapshotRepository.findByMarketAreaIdOrderBySnapshotDateAsc(areaId).stream()
                .filter(s -> excludeSnapshotId == null || !excludeSnapshotId.equals(s.getId()))
                .map(MarketStatSnapshot::getAvgPricePerSqft)
                .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .orElse(avgPsf);
        if (snapshotDate != null) {
            Optional<MarketStatSnapshot> earliestOther = snapshotRepository
                    .findByMarketAreaIdOrderBySnapshotDateAsc(areaId).stream()
                    .filter(s -> excludeSnapshotId == null || !excludeSnapshotId.equals(s.getId()))
                    .filter(s -> s.getAvgPricePerSqft() != null && s.getAvgPricePerSqft().compareTo(BigDecimal.ZERO) > 0)
                    .findFirst();
            if (earliestOther.isPresent() && snapshotDate.isBefore(earliestOther.get().getSnapshotDate())) {
                baseline = avgPsf;
            }
        }
        return avgPsf.multiply(BigDecimal.valueOf(100)).divide(baseline, 4, RoundingMode.HALF_UP);
    }

    private void ensureUniqueSnapshot(Long areaId, LocalDate date,
                                      MarketStatSnapshot.Granularity granularity, Long excludeId) {
        var existing = snapshotRepository.findByMarketAreaIdAndSnapshotDateAndGranularity(areaId, date, granularity);
        if (existing.isPresent() && (excludeId == null || !existing.get().getId().equals(excludeId))) {
            throw new BadRequestException(
                    "A snapshot already exists for this area on " + date + " (" + granularity + "). Edit the existing row instead.");
        }
    }
}
