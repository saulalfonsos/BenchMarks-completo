package gal.usc.etse.es.restdemo.configuration;

import gal.usc.etse.es.restdemo.repository.PhotoRepository;
import gal.usc.etse.es.restdemo.service.BenchService;
import gal.usc.etse.es.restdemo.service.PhotoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Completa a carga dos datos de proba que fan os scripts SQL en cada arranque:
 * <ul>
 *     <li>Baleira a caché de bancos en Redis, que sobrevive aos reinicios mentres a base de datos se recrea.</li>
 *     <li>Sobe as imaxes de exemplo de static/img como fotos dos bancos (son binarias e non se poden
 *     cargar dende SQL), usando o mesmo servizo que a API, polo que tamén se xeran as miniaturas.</li>
 * </ul>
 */
@Component
public class SampleDataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SampleDataInitializer.class);
    private static final int SAMPLE_IMAGES = 30;
    private static final int SAMPLE_BENCHES = 20;
    private static final List<String> UPLOADERS = List.of("user", "ana", "brais", "admin");

    private final PhotoService photos;
    private final PhotoRepository repository;
    private final CacheManager caches;

    public SampleDataInitializer(PhotoService photos, PhotoRepository repository, CacheManager caches) {
        this.photos = photos;
        this.repository = repository;
        this.caches = caches;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Optional.ofNullable(caches.getCache(BenchService.CACHE)).ifPresent(cache -> cache.clear());

        if (repository.count() > 0) return;
        for (int i = 1; i <= SAMPLE_IMAGES; i++) {
            var image = new ClassPathResource("static/img/img" + i + ".jpg");
            if (!image.exists()) continue;
            long benchID = (i - 1) % SAMPLE_BENCHES + 1;
            photos.upload(benchID, UPLOADERS.get(i % UPLOADERS.size()), image.getContentAsByteArray());
        }
        log.info("Sample photos loaded");
    }
}
