package gal.usc.etse.es.restdemo.shade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Fachada sobre o servizo meteorolóxico externo: cachea as predicións (unha hora en Redis) e degrada
 * de forma controlada (fallback) se o servizo non está dispoñible ou a data está fóra de rango.
 */
@Service
public class WeatherService {
    public static final String CACHE = "weather";
    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    /** Open-Meteo ofrece predición ata 16 días vista e histórico recente. */
    static final int MAX_DAYS_AHEAD = 15;
    static final int MAX_DAYS_BEHIND = 60;

    private final OpenMeteoClient client;

    public WeatherService(OpenMeteoClient client) {
        this.client = client;
    }

    public record CloudCover(HashMap<Integer, Integer> byHour) implements Serializable {
        public Integer at(int hour) { return byHour.get(hour); }
    }

    /**
     * As coordenadas deben chegar redondeadas (2 decimais, ~1 km) para que bancos próximos compartan entrada na caché.
     */
    @Cacheable(cacheNames = CACHE, unless = "#result == null")
    public Optional<CloudCover> cloudCover(double latitude, double longitude, LocalDate date, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        if (date.isAfter(today.plusDays(MAX_DAYS_AHEAD)) || date.isBefore(today.minusDays(MAX_DAYS_BEHIND))) {
            return Optional.empty();
        }
        try {
            Map<Integer, Integer> values = client.hourlyCloudCover(latitude, longitude, date, zone);
            return Optional.of(new CloudCover(new HashMap<>(values)));
        } catch (RestClientException e) {
            log.warn("Weather service unavailable, falling back to astronomical shade: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
