package gal.usc.etse.es.restdemo.shade;

import gal.usc.etse.es.restdemo.model.dto.Bench;
import gal.usc.etse.es.restdemo.model.dto.ShadeReport;
import gal.usc.etse.es.restdemo.model.dto.ShadeReport.Hour;
import gal.usc.etse.es.restdemo.model.dto.ShadeReport.Reason;
import gal.usc.etse.es.restdemo.service.BenchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Calcula se un banco estará á sombra en cada hora dun intervalo, compoñendo:
 * <ol>
 *     <li>datos propios do banco (se está cuberto),</li>
 *     <li>a posición do sol, calculada localmente (SolarPosition), e</li>
 *     <li>a nubosidade prevista, obtida do servizo externo Open-Meteo (WeatherService).</li>
 * </ol>
 */
@Service
public class ShadeService {
    /** Por debaixo desta elevación as sombras dos edificios e árbores próximos cobren o banco. */
    static final double LOW_SUN_ELEVATION = 10.0;
    /** A partir desta porcentaxe de nubes non hai sol directo. */
    static final int OVERCAST_CLOUD_COVER = 75;

    private final BenchService benches;
    private final WeatherService weather;
    private final ZoneId zone;

    public ShadeService(BenchService benches, WeatherService weather, @Value("${benchmarks.timezone:Europe/Madrid}") ZoneId zone) {
        this.benches = benches;
        this.weather = weather;
        this.zone = zone;
    }

    public ShadeReport forecast(Long benchID, LocalDate date, int fromHour, int toHour) {
        if (fromHour > toHour) {
            throw new IllegalArgumentException("'from' must be lower than or equal to 'to'");
        }
        Bench bench = benches.get(benchID);
        boolean covered = Boolean.TRUE.equals(bench.covered());

        Optional<WeatherService.CloudCover> clouds = covered
                ? Optional.empty()
                : weather.cloudCover(round(bench.latitude()), round(bench.longitude()), date, zone);

        List<Hour> hours = new ArrayList<>();
        for (int h = fromHour; h <= toHour; h++) {
            final int hour = h;
            var sun = SolarPosition.at(date.atTime(h, 0).atZone(zone).toInstant(), bench.latitude(), bench.longitude());
            Integer cloudCover = clouds.map(c -> c.at(hour)).orElse(null);
            Reason reason = classify(covered, sun.elevation(), cloudCover);
            hours.add(new Hour(h, round(sun.elevation()), round(sun.azimuth()), cloudCover, reason != Reason.DIRECT_SUN, reason));
        }

        int shaded = (int) hours.stream().filter(Hour::shaded).count();
        return new ShadeReport(benchID, date, zone.getId(),
                clouds.isPresent() ? ShadeReport.Source.OPEN_METEO : ShadeReport.Source.ASTRONOMICAL,
                shaded, hours.size() - shaded, hours);
    }

    static Reason classify(boolean covered, double sunElevation, Integer cloudCover) {
        if (covered) return Reason.COVERED;
        if (sunElevation <= 0) return Reason.NIGHT;
        if (sunElevation < LOW_SUN_ELEVATION) return Reason.LOW_SUN;
        if (cloudCover != null && cloudCover >= OVERCAST_CLOUD_COVER) return Reason.OVERCAST;
        return Reason.DIRECT_SUN;
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
