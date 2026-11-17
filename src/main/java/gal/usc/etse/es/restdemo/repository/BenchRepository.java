package gal.usc.etse.es.restdemo.repository;

import gal.usc.etse.es.restdemo.model.entity.Bench;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@NullMarked
public interface BenchRepository extends JpaRepository<Bench, Long>, JpaSpecificationExecutor<Bench> {
    @Query("SELECT b FROM Bench b WHERE EXISTS (SELECT 1 FROM User u JOIN u.favoriteBenches f WHERE u.userID = :userID AND f = b)")
    Page<Bench> findFavoritesOf(@Param("userID") String userID, Pageable pageable);

    @Query("SELECT b FROM Bench b WHERE EXISTS (SELECT 1 FROM User u JOIN u.likedBenches l WHERE u.userID = :userID AND l = b)")
    Page<Bench> findLikedBy(@Param("userID") String userID, Pageable pageable);
}
