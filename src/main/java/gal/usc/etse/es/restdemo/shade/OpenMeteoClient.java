package gal.usc.etse.es.restdemo.shade;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.resilience.annotation.ConcurrencyLimit;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente do servizo externo Open-Meteo (https://open-meteo.com), que non require clave.
 * Patróns de resiliencia: timeouts curtos, reintentos con backoff exponencial e límite de concorrencia
 * para non saturar o servizo externo.
 */
@Component
public class OpenMeteoClient {
    private final RestClient client;

    public OpenMeteoClient(@Value("${benchmarks.weather.base-url}") String baseUrl,
                           @Value("${benchmarks.weather.timeout:PT3S}") Duration timeout) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    /** Porcentaxe de nubosidade para cada hora local (0-23) dun día. */
    @Retryable(includes = RestClientException.class, maxRetries = 2, delay = 250, multiplier = 2)
    @ConcurrencyLimit(5)
    public Map<Integer, Integer> hourlyCloudCover(double latitude, double longitude, LocalDate date, ZoneId zone) {
        Forecast forecast = client.get()
                .uri(uri -> uri
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("hourly", "cloud_cover")
                        .queryParam("timezone", zone.getId())
                        .queryParam("start_date", date)
                        .queryParam("end_date", date)
                        .build())
                .retrieve()
                .body(Forecast.class);

        if (forecast == null || forecast.hourly() == null || forecast.hourly().time() == null || forecast.hourly().cloudCover() == null) {
            throw new RestClientException("Empty response from Open-Meteo");
        }

        Map<Integer, Integer> byHour = new HashMap<>();
        List<String> times = forecast.hourly().time();
        for (int i = 0; i < times.size() && i < forecast.hourly().cloudCover().size(); i++) {
            Integer value = forecast.hourly().cloudCover().get(i);
            if (value != null) byHour.put(LocalDateTime.parse(times.get(i)).getHour(), value);
        }
        return byHour;
    }

    record Forecast(Hourly hourly) {}

    record Hourly(List<String> time, @JsonProperty("cloud_cover") List<Integer> cloudCover) {}
}
