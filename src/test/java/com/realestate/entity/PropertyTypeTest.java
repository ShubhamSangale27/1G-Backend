package com.realestate.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class PropertyTypeTest {

    @ParameterizedTest
    @CsvSource({
            "HOUSE, HOUSE",
            "FLAT, FLAT",
            "LAND, LAND",
            "COMMERCIAL, COMMERCIAL",
            "PG, PG",
            "apartment, FLAT",
            "APARTMENT, FLAT",
            "flat, FLAT",
            "pg, PG"
    })
    void fromApi_mapsKnownAndLegacyValues(String input, Property.PropertyType expected) {
        assertEquals(expected, Property.PropertyType.fromApi(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "VILLA", "APT"})
    void fromApi_rejectsUnknown(String input) {
        assertThrows(IllegalArgumentException.class, () -> Property.PropertyType.fromApi(input));
    }

    @Test
    void fromApi_rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> Property.PropertyType.fromApi(null));
    }

    @Test
    void toApi_returnsEnumName() {
        assertEquals("FLAT", Property.PropertyType.FLAT.toApi());
        assertEquals("PG", Property.PropertyType.PG.toApi());
    }
}
