package gal.usc.etse.es.restdemo.repository;

import gal.usc.etse.es.restdemo.model.entity.Review;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@NullMarked
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByBench_BenchID(Long benchID, Pageable pageable);

    Page<Review> findByAuthor_UserID(String userID, Pageable pageable);

    Optional<Review> findByAuthor_UserIDAndBench_BenchID(String userID, Long benchID);

    /** Recalcula nunha soa sentenza a nota media e o número de valoracións dun banco. */
    @Modifying
    @Query("""
            UPDATE Bench b SET
                b.averageMark = (SELECT avg(r.mark) FROM Review r WHERE r.bench = b),
                b.reviewCount = (SELECT count(r) FROM Review r WHERE r.bench = b)
            WHERE b.benchID = :benchID
            """)
    int refreshRating(@Param("benchID") Long benchID);

    @Modifying
    @Query("""
            UPDATE Bench b SET
                b.averageMark = (SELECT avg(r.mark) FROM Review r WHERE r.bench = b),
                b.reviewCount = (SELECT count(r) FROM Review r WHERE r.bench = b)
            """)
    int refreshAllRatings();
}
