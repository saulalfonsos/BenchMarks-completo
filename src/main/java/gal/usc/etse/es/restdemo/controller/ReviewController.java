package gal.usc.etse.es.restdemo.controller;

import gal.usc.etse.es.restdemo.controller.hal.Pages;
import gal.usc.etse.es.restdemo.controller.hal.ReviewAssembler;
import gal.usc.etse.es.restdemo.model.dto.Review;
import gal.usc.etse.es.restdemo.service.ReviewService;
import gal.usc.etse.es.utils.patch.JsonPatchOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@NullMarked
@RestController
@Tag(name = "Valoracións", description = "Notas (0-10) e reseñas dos bancos. Unha por usuario e banco.")
public class ReviewController {
    private final ReviewService reviews;
    private final ReviewAssembler assembler;
    private final Pages pages;

    public ReviewController(ReviewService reviews, ReviewAssembler assembler, Pages pages) {
        this.reviews = reviews;
        this.assembler = assembler;
        this.pages = pages;
    }

    @GetMapping(path = "reviews", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAuthority('reviews:read')")
    @Operation(summary = "Últimas valoracións de todos os bancos")
    public ResponseEntity<PagedModel<EntityModel<Review>>> getAll(
            @ParameterObject @PageableDefault(sort = "reviewID", direction = Sort.Direction.DESC) @Nullable Pageable pageable) {
        return ResponseEntity.ok(pages.toModel(reviews.getAll(pageable), Review.class, assembler));
    }

    @GetMapping(path = "benches/{benchID}/reviews", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAuthority('reviews:read')")
    @Operation(summary = "Valoracións dun banco")
    public ResponseEntity<PagedModel<EntityModel<Review>>> getByBench(
            @PathVariable("benchID") Long benchID,
            @ParameterObject @PageableDefault(sort = "reviewID", direction = Sort.Direction.DESC) @Nullable Pageable pageable) {
        return ResponseEntity.ok(pages.toModel(reviews.getByBench(benchID, pageable), Review.class, assembler));
    }

    @GetMapping(path = "users/{userID}/reviews", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAuthority('reviews:read')")
    @Operation(summary = "Valoracións feitas por un usuario")
    public ResponseEntity<PagedModel<EntityModel<Review>>> getByAuthor(
            @PathVariable("userID") String userID,
            @ParameterObject @PageableDefault(sort = "reviewID", direction = Sort.Direction.DESC) @Nullable Pageable pageable) {
        return ResponseEntity.ok(pages.toModel(reviews.getByAuthor(userID, pageable), Review.class, assembler));
    }

    @GetMapping(path = "reviews/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAuthority('reviews:read')")
    @Operation(summary = "Obter unha valoración")
    public ResponseEntity<EntityModel<Review>> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(assembler.toModel(reviews.get(id)));
    }

    @PostMapping(path = "benches/{benchID}/reviews", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAuthority('reviews:create')")
    @Operation(summary = "Valorar un banco",
            description = "O autor é sempre o usuario autenticado. Se xa valorou o banco devólvese 409 coa cabeceira Location da valoración existente.")
    public ResponseEntity<EntityModel<Review>> create(@PathVariable("benchID") Long benchID, @RequestBody @Valid Review review,
                                                      @Parameter(hidden = true) Authentication authentication) {
        Review created = reviews.create(benchID, authentication.getName(), review);
        return ResponseEntity
                .created(linkTo(methodOn(ReviewController.class).get(created.reviewID())).toUri())
                .body(assembler.toModel(created));
    }

    @PatchMapping(path = "reviews/{id}", consumes = "application/json-patch+json", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAuthority('reviews:update') or @reviewService.canModify(#id, authentication.name)")
    @Operation(summary = "Modificar unha valoración (JSON-Patch)", description = "Só o autor ou un administrador. Campos editables: mark, note.")
    public ResponseEntity<EntityModel<Review>> update(@PathVariable("id") Long id, @RequestBody List<JsonPatchOperation> changes) {
        return ResponseEntity.ok(assembler.toModel(reviews.update(id, changes)));
    }

    @DeleteMapping(path = "reviews/{id}")
    @PreAuthorize("hasAuthority('reviews:delete') or @reviewService.canModify(#id, authentication.name)")
    @Operation(summary = "Borrar unha valoración", description = "Só o autor ou un administrador.")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        reviews.delete(id);
        return ResponseEntity.noContent().build();
    }
}
