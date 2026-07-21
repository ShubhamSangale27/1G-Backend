package com.realestate.service.market;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Built-in free/open-style seed dataset approximating RBI HPI-style quarterly
 * indices for a handful of major Indian cities. Not a live network API —
 * data is embedded so the calculator works without paid third-party keys.
 */
@Component
public class RbiHpiSeedProvider implements MarketDataProvider {

    @Override
    public String getName() {
        return "RBI_HPI_SEED";
    }

    @Override
    public List<SupportedArea> listSupportedAreas() {
        return List.of(
                new SupportedArea("IN-KA-Bangalore-Whitefield", "Karnataka", "Bangalore", "Whitefield"),
                new SupportedArea("IN-MH-Mumbai-Andheri", "Maharashtra", "Mumbai", "Andheri"),
                new SupportedArea("IN-MH-Pune-Hinjewadi", "Maharashtra", "Pune", "Hinjewadi"),
                new SupportedArea("IN-TG-Hyderabad-Gachibowli", "Telangana", "Hyderabad", "Gachibowli"),
                new SupportedArea("IN-DL-Delhi-Dwarka", "Delhi", "New Delhi", "Dwarka")
        );
    }

    @Override
    public List<SeriesPoint> fetchAreaSeries(String areaKey, LocalDate from, LocalDate to) {
        double quarterlyGrowth = switch (areaKey) {
            case "IN-KA-Bangalore-Whitefield" -> 0.022;
            case "IN-MH-Mumbai-Andheri" -> 0.018;
            case "IN-MH-Pune-Hinjewadi" -> 0.020;
            case "IN-TG-Hyderabad-Gachibowli" -> 0.021;
            case "IN-DL-Delhi-Dwarka" -> 0.015;
            default -> 0.018;
        };
        double priceBase = switch (areaKey) {
            case "IN-KA-Bangalore-Whitefield" -> 6500;
            case "IN-MH-Mumbai-Andheri" -> 22000;
            case "IN-MH-Pune-Hinjewadi" -> 7200;
            case "IN-TG-Hyderabad-Gachibowli" -> 6800;
            case "IN-DL-Delhi-Dwarka" -> 9500;
            default -> 7000;
        };

        LocalDate start = LocalDate.of(2016, 3, 31);
        LocalDate end = LocalDate.of(2026, 3, 31);
        List<SeriesPoint> points = new ArrayList<>();
        double index = 100.0;
        LocalDate cursor = start;
        int q = 0;
        while (!cursor.isAfter(end) && q < 80) {
            if ((from == null || !cursor.isBefore(from)) && (to == null || !cursor.isAfter(to))) {
                double yoy = q >= 4 ? (Math.pow(1 + quarterlyGrowth, 4) - 1) * 100.0 : 0.0;
                points.add(new SeriesPoint(
                        cursor,
                        BigDecimal.valueOf(round4(index)),
                        BigDecimal.valueOf(round2(priceBase * (index / 100.0))),
                        BigDecimal.valueOf(round4(yoy))
                ));
            }
            index *= (1 + quarterlyGrowth);
            cursor = nextQuarterEnd(cursor);
            q++;
        }
        return points;
    }

    private static LocalDate nextQuarterEnd(LocalDate current) {
        int month = current.getMonthValue();
        int year = current.getYear();
        return switch (month) {
            case 3 -> LocalDate.of(year, 6, 30);
            case 6 -> LocalDate.of(year, 9, 30);
            case 9 -> LocalDate.of(year, 12, 31);
            default -> LocalDate.of(year + 1, 3, 31);
        };
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
