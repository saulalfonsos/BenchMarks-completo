package gal.usc.etse.es.restdemo.repository;

import gal.usc.etse.es.restdemo.model.entity.Photo;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * As consultas de metadatos usan proxeccións a DTO para non ler os bytes das imaxes.
 */
@Repository
@NullMarked
public interface PhotoRepository extends JpaRepository<Photo, Long> {
    String METADATA = """
            SELECT new gal.usc.etse.es.restdemo.model.dto.Photo(
                p.photoID, p.bench.benchID, p.uploader.userID, p.contentType, p.size, p.width, p.height, p.uploadedAt,
                CASE WHEN p.thumbnail IS NULL THEN false ELSE true END)
            FROM Photo p
            """;

    @Query(METADATA + " WHERE p.photoID = :photoID")
    Optional<gal.usc.etse.es.restdemo.model.dto.Photo> findMetadata(@Param("photoID") Long photoID);

    @Query(value = METADATA + " WHERE p.bench.benchID = :benchID",
            countQuery = "SELECT count(p) FROM Photo p WHERE p.bench.benchID = :benchID")
    Page<gal.usc.etse.es.restdemo.model.dto.Photo> findMetadataByBench(@Param("benchID") Long benchID, Pageable pageable);

    @Query("SELECT p.uploader.userID FROM Photo p WHERE p.photoID = :photoID")
    Optional<String> findUploader(@Param("photoID") Long photoID);
}
