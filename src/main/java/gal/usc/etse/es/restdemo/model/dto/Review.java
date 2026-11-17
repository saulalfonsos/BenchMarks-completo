package gal.usc.etse.es.restdemo.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDate;

@Relation(collectionRelation = "reviews", itemRelation = "review")
@Schema(description = "Valoración dun banco. Cada usuario só pode valorar unha vez cada banco")
public record Review(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "1")
        Long reviewID,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Autor da valoración (o usuario autenticado)")
        String author,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        Long benchID,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        LocalDate date,

        @NotNull @Min(0) @Max(10)
        @Schema(example = "8")
        Integer mark,

        @Size(max = 2000)
        @Schema(example = "Moi cómodo e con boa sombra pola tarde")
        String note
) {
    public static Review from(gal.usc.etse.es.restdemo.model.entity.Review review) {
        return new Review(
                review.getReviewID(),
                review.getAuthor().getUserID(),
                review.getBench().getBenchID(),
                review.getDate(),
                review.getMark(),
                review.getNote()
        );
    }
}
