package gal.usc.etse.es.restdemo.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;

@Relation(collectionRelation = "comments", itemRelation = "comment")
@Schema(description = "Comentario libre sobre un banco")
public record Comment(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY, example = "1")
        Long commentID,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        String author,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        Long benchID,

        @NotBlank @Size(max = 1000)
        @Schema(example = "Alguén sabe se o arranxaron xa?")
        String text,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        Instant createdAt,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        Instant editedAt
) {
    public static Comment from(gal.usc.etse.es.restdemo.model.entity.Comment comment) {
        return new Comment(
                comment.getCommentID(),
                comment.getAuthor().getUserID(),
                comment.getBench().getBenchID(),
                comment.getText(),
                comment.getCreatedAt(),
                comment.getEditedAt()
        );
    }
}
