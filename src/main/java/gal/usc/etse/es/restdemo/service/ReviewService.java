package gal.usc.etse.es.restdemo.service;

import gal.usc.etse.es.restdemo.event.ReviewChangedEvent;
import gal.usc.etse.es.restdemo.exception.DuplicateReviewException;
import gal.usc.etse.es.restdemo.exception.ResourceNotFoundException;
import gal.usc.etse.es.restdemo.model.dto.Review;
import gal.usc.etse.es.restdemo.repository.BenchRepository;
import gal.usc.etse.es.restdemo.repository.ReviewRepository;
import gal.usc.etse.es.restdemo.repository.UserRepository;
import gal.usc.etse.es.utils.patch.JsonPatchOperation;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@NullMarked
public class ReviewService {
    private final ReviewRepository reviews;
    private final BenchRepository benches;
    private final UserRepository users;
    private final Patcher patcher;
    private final ApplicationEventPublisher events;
    private final ZoneId zone;

    public ReviewService(ReviewRepository reviews, BenchRepository benches, UserRepository users, Patcher patcher,
                         ApplicationEventPublisher events, @Value("${benchmarks.timezone:Europe/Madrid}") ZoneId zone) {
        this.reviews = reviews;
        this.benches = benches;
        this.users = users;
        this.patcher = patcher;
        this.events = events;
        this.zone = zone;
    }

    @Transactional(readOnly = true)
    public Page<Review> getAll(Pageable pageable) {
        return reviews.findAll(pageable).map(Review::from);
    }

    @Transactional(readOnly = true)
    public Page<Review> getByBench(Long benchID, Pageable pageable) {
        if (!benches.existsById(benchID)) throw new ResourceNotFoundException("Bench", benchID);
        return reviews.findByBench_BenchID(benchID, pageable).map(Review::from);
    }

    @Transactional(readOnly = true)
    public Page<Review> getByAuthor(String userID, Pageable pageable) {
        if (!users.existsById(userID)) throw new ResourceNotFoundException("User", userID);
        return reviews.findByAuthor_UserID(userID, pageable).map(Review::from);
    }

    @Transactional(readOnly = true)
    public Review get(Long id) {
        return reviews.findById(id).map(Review::from).orElseThrow(() -> new ResourceNotFoundException("Review", id));
    }

    /**
     * Crea a valoración do usuario autenticado. A nota media do banco recalcúlase de forma asíncrona
     * cando a transacción confirma (ver RatingUpdater).
     */
    @Transactional
    public Review create(Long benchID, String author, Review review) {
        var bench = benches.findById(benchID).orElseThrow(() -> new ResourceNotFoundException("Bench", benchID));
        reviews.findByAuthor_UserIDAndBench_BenchID(author, benchID).ifPresent(existing -> {
            throw new DuplicateReviewException(author, benchID, existing.getReviewID());
        });

        var entity = new gal.usc.etse.es.restdemo.model.entity.Review(
                users.getReferenceById(author), bench, LocalDate.now(zone), review.mark(), review.note()
        );
        Review created = Review.from(reviews.save(entity));
        events.publishEvent(new ReviewChangedEvent(benchID));
        return created;
    }

    @Transactional
    public Review update(Long id, List<JsonPatchOperation> changes) {
        var entity = reviews.findById(id).orElseThrow(() -> new ResourceNotFoundException("Review", id));
        Review patched = patcher.apply(Review.from(entity), changes, Review.class);

        entity.setMark(patched.mark()).setNote(patched.note()).setDate(LocalDate.now(zone));
        events.publishEvent(new ReviewChangedEvent(entity.getBench().getBenchID()));
        return Review.from(entity);
    }

    @Transactional
    public void delete(Long id) {
        var entity = reviews.findById(id).orElseThrow(() -> new ResourceNotFoundException("Review", id));
        Long benchID = entity.getBench().getBenchID();
        reviews.delete(entity);
        events.publishEvent(new ReviewChangedEvent(benchID));
    }

    /** O autor pode editar/borrar a súa valoración. Se non existe, deixa pasar para responder 404. */
    @Transactional(readOnly = true)
    public boolean canModify(Long id, String user) {
        return reviews.findById(id).map(r -> r.getAuthor().getUserID().equals(user)).orElse(true);
    }
}
