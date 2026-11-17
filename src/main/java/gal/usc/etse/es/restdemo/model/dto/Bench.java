package gal.usc.etse.es.restdemo.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.springframework.hateoas.server.core.Relation;

import java.io.Serializable;
import java.time.Instant;

@Relation(collectionRelation = "benches", itemRelation = "bench")
@Schema(description = "Banco público para sentar")
public record Bench(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "1")
        Long benchID,

        @NotBlank @Size(max = 100)
        @Schema(example = "Banco da Alameda")
        String name,

        @Size(max = 1000)
        @Schema(example = "Banco de pedra con vistas á Catedral")
        String description,

        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
        @Schema(example = "42.8770")
        Double latitude,

        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
        @Schema(example = "-8.5480")
        Double longitude,

        @NotBlank @Size(max = 50)
        @Schema(example = "Stone")
        String type,

        @Schema(description = "O banco está baixo unha cuberta (árbore, soportal...) e polo tanto sempre á sombra", defaultValue = "false")
        Boolean covered,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Usuario que deu de alta o banco")
        String owner,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        Instant createdAt,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Nota media (0-10) das valoracións")
        Double averageMark,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        Integer reviewCount,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        Long likes
) implements Serializable {
    public static Bench from(gal.usc.etse.es.restdemo.model.entity.Bench bench) {
        return new Bench(
                bench.getBenchID(),
                bench.getName(),
                bench.getDescription(),
                bench.getLatitude(),
                bench.getLongitude(),
                bench.getType(),
                bench.isCovered(),
                bench.getOwner() == null ? null : bench.getOwner().getUserID(),
                bench.getCreatedAt(),
                bench.getAverageMark(),
                bench.getReviewCount(),
                bench.getLikes()
        );
    }
}
