package gal.usc.etse.es.restdemo.shade;

import gal.usc.etse.es.restdemo.model.dto.Bench;
import gal.usc.etse.es.restdemo.model.dto.ShadeReport;
import gal.usc.etse.es.restdemo.model.dto.ShadeReport.Reason;
import gal.usc.etse.es.restdemo.service.BenchService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ShadeServiceTest {
    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final LocalDate SUMMER = LocalDate.of(2026, 6, 21);

    private final BenchService benches = mock(BenchService.class);
    private final WeatherService weather = mock(WeatherService.class);
    private final ShadeService service = new ShadeService(benches, weather, MADRID);

    private static Bench bench(boolean covered) {
        return new Bench(1L, "Banco", null, 42.8805, -8.5456, "Stone", covered, "user", null, null, 0, 0L);
    }

    @Test
    void classifyAppliesRulesInOrder() {
        assertEquals(Reason.COVERED, ShadeService.classify(true, 60, 0));
        assertEquals(Reason.NIGHT, ShadeService.classify(false, -3, 0));
        assertEquals(Reason.LOW_SUN, ShadeService.classify(false, 5, 0));
        assertEquals(Reason.OVERCAST, ShadeService.classify(false, 50, 90));
        assertEquals(Reason.DIRECT_SUN, ShadeService.classify(false, 50, 20));
        assertEquals(Reason.DIRECT_SUN, ShadeService.classify(false, 50, null));
    }

    @Test
    void usesCloudCoverFromWeatherService() {
        when(benches.get(1L)).thenReturn(bench(false));
        var clouds = new HashMap<Integer, Integer>();
        clouds.put(13, 10);
        clouds.put(14, 95);
        when(weather.cloudCover(anyDouble(), anyDouble(), eq(SUMMER), eq(MADRID)))
                .thenReturn(Optional.of(new WeatherService.CloudCover(clouds)));

        ShadeReport report = service.forecast(1L, SUMMER, 13, 14);

        assertEquals(ShadeReport.Source.OPEN_METEO, report.source());
        assertEquals(Reason.DIRECT_SUN, report.hours().get(0).reason());
        assertEquals(Reason.OVERCAST, report.hours().get(1).reason());
        assertEquals(1, report.sunnyHours());
        assertEquals(1, report.shadedHours());
    }

    @Test
    void fallsBackToAstronomicalWhenWeatherIsUnavailable() {
        when(benches.get(1L)).thenReturn(bench(false));
        when(weather.cloudCover(anyDouble(), anyDouble(), any(), any())).thenReturn(Optional.empty());

        ShadeReport report = service.forecast(1L, SUMMER, 0, 23);

        assertEquals(ShadeReport.Source.ASTRONOMICAL, report.source());
        assertEquals(24, report.hours().size());
        assertTrue(report.hours().stream().allMatch(h -> h.cloudCover() == null));
        assertEquals(Reason.NIGHT, report.hours().get(0).reason());
        assertEquals(Reason.DIRECT_SUN, report.hours().get(14).reason());
    }

    @Test
    void coveredBenchIsAlwaysShadedAndDoesNotCallWeatherService() {
        when(benches.get(1L)).thenReturn(bench(true));

        ShadeReport report = service.forecast(1L, SUMMER, 8, 20);

        assertEquals(0, report.sunnyHours());
        assertTrue(report.hours().stream().allMatch(h -> h.reason() == Reason.COVERED));
        verifyNoInteractions(weather);
    }

    @Test
    void rejectsInvertedInterval() {
        assertThrows(IllegalArgumentException.class, () -> service.forecast(1L, SUMMER, 18, 10));
    }
}
