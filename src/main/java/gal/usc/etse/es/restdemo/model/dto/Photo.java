package gal.usc.etse.es.restdemo.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;

/** Metadatos dunha foto. O binario descárgase en /photos/{id}/content. */
@Relation(collectionRelation = "photos", itemRelation = "photo")
@Schema(description = "Metadatos dunha foto dun banco")
public record Photo(
        Long photoID,
        Long benchID,
        String uploader,
        @Schema(example = "image/jpeg") String contentType,
        @Schema(description = "Tamaño en bytes") long size,
        int width,
        int height,
        Instant uploadedAt,
        @Schema(description = "A miniatura xérase de forma asíncrona tras a subida") boolean thumbnailAvailable
) {}
