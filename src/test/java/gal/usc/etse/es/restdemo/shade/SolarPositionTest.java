package gal.usc.etse.es.restdemo.shade;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SolarPositionTest {
    private static final double LAT = 42.8805;   // Santiago de Compostela
    private static final double LON = -8.5456;
    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

    private static SolarPosition at(int year, int month, int day, int hour, int minute) {
        return SolarPosition.at(ZonedDateTime.of(year, month, day, hour, minute, 0, 0, MADRID).toInstant(), LAT, LON);
    }

    @Test
    void summerSolsticeNoonElevationMatchesLatitudePlusTilt() {
        // Ao mediodía solar do solsticio, elevación ≈ 90 - lat + 23.44 ≈ 70.6°
        SolarPosition noon = at(2026, 6, 21, 14, 36);
        assertEquals(70.5, noon.elevation(), 0.7);
        assertEquals(180, noon.azimuth(), 3, "ao mediodía solar o sol está ao sur");
    }

    @Test
    void winterSolsticeNoonElevationMatchesLatitudeMinusTilt() {
        SolarPosition noon = at(2026, 12, 21, 13, 36);
        assertEquals(23.7, noon.elevation(), 0.7);
    }

    @Test
    void sunIsBelowHorizonAtMidnight() {
        assertTrue(at(2026, 6, 21, 0, 30).elevation() < 0);
        assertTrue(at(2026, 11, 17, 7, 0).elevation() < 0);
    }

    @Test
    void sunRisesInTheEastAndSetsInTheWest() {
        assertTrue(at(2026, 3, 20, 9, 0).azimuth() < 180, "pola mañá o sol está ao leste");
        assertTrue(at(2026, 3, 20, 18, 0).azimuth() > 180, "pola tarde o sol está ao oeste");
    }
}
