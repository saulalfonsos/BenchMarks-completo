package gal.usc.etse.es.restdemo.event;

import gal.usc.etse.es.restdemo.repository.ReviewRepository;
import gal.usc.etse.es.restdemo.service.BenchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Mantén actualizadas a nota media e o número de valoracións de cada banco.
 * <ul>
 *     <li>De forma reactiva: escoita {@link ReviewChangedEvent} só despois de que a transacción que
 *     modificou a valoración confirme, e recalcula en segundo plano (@Async) nunha transacción nova.</li>
 *     <li>De forma programada: cada noite reconcilia todos os bancos por se algún evento se perdeu
 *     (por exemplo, se a aplicación se detivo antes de procesalo).</li>
 * </ul>
 */
@Component
public class RatingUpdater {
    private static final Logger log = LoggerFactory.getLogger(RatingUpdater.class);
    private final ReviewRepository reviews;

    public RatingUpdater(ReviewRepository reviews) {
        this.reviews = reviews;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @CacheEvict(cacheNames = BenchService.CACHE, key = "#event.benchID()")
    public void onReviewChanged(ReviewChangedEvent event) {
        reviews.refreshRating(event.benchID());
        log.debug("Rating of bench {} recalculated", event.benchID());
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "${benchmarks.timezone:Europe/Madrid}")
    @Transactional
    @CacheEvict(cacheNames = BenchService.CACHE, allEntries = true)
    public void reconcileAllRatings() {
        int updated = reviews.refreshAllRatings();
        log.info("Nightly rating reconciliation finished: {} benches updated", updated);
    }
}
