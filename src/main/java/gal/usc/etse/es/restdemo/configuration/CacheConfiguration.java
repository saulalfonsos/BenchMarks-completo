package gal.usc.etse.es.restdemo.configuration;

import gal.usc.etse.es.restdemo.service.BenchService;
import gal.usc.etse.es.restdemo.shade.WeatherService;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import java.time.Duration;

/**
 * Cachés:
 * <ul>
 *     <li>Servidor (Redis, compartida entre instancias): bancos individuais (10 min) e predicións meteorolóxicas (1 h).</li>
 *     <li>HTTP: ETag calculado sobre o corpo de cada resposta GET, de modo que o cliente pode usar If-None-Match e recibir 304.</li>
 * </ul>
 * O interceptor de caché execútase por fóra do transaccional (orde máxima) para que as invalidacións
 * ocorran despois do commit.
 */
@Configuration
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE)
public class CacheConfiguration {
    @Bean
    public RedisCacheManagerBuilderCustomizer cacheTtls() {
        return builder -> builder
                .withCacheConfiguration(BenchService.CACHE, RedisCacheConfiguration.defaultCacheConfig(getClass().getClassLoader())
                        .entryTtl(Duration.ofMinutes(10)).prefixCacheNameWith("benchmarks::"))
                .withCacheConfiguration(WeatherService.CACHE, RedisCacheConfiguration.defaultCacheConfig(getClass().getClassLoader())
                        .entryTtl(Duration.ofHours(1)).prefixCacheNameWith("benchmarks::"));
    }

    /** Só nos recursos da API: aplicado á páxina de benvida (forward de / a index.html) perdería o corpo. */
    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        var registration = new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns("/benches/*", "/reviews/*", "/comments/*", "/photos/*", "/users/*");
        return registration;
    }
}
