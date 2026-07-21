package com.realestate.config;

import com.realestate.repository.MarketAreaRepository;
import com.realestate.service.MarketStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seeds free/open RBI-style market statistics when the catalog is empty,
 * so the SIP calculator works without a manual admin import.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class MarketStatsSeedRunner implements ApplicationRunner {

    private final MarketAreaRepository marketAreaRepository;
    private final MarketStatsService marketStatsService;

    @Override
    public void run(ApplicationArguments args) {
        if (marketAreaRepository.count() > 0) {
            return;
        }
        try {
            int upserted = marketStatsService.importRbiSeed();
            log.info("Seeded market statistics from open/RBI-style dataset ({} records)", upserted);
        } catch (Exception e) {
            log.warn("Market statistics seed skipped: {}", e.getMessage());
        }
    }
}
