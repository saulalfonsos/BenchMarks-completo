package gal.usc.etse.es.restdemo.controller;

import gal.usc.etse.es.restdemo.controller.hal.CommentAssembler;
import gal.usc.etse.es.restdemo.controller.hal.Pages;
import gal.usc.etse.es.restdemo.model.dto.Comment;
import gal.usc.etse.es.restdemo.service.CommentService;
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
@Tag(name = "Comentarios", description = "Conversa libre sobre cada banco")
public class CommentController {
    private final CommentService comments;
    private final CommentAssembler assembler;
    private final Pages pages;

    public CommentController(CommentService comments, CommentAssembler assembler, Pages pages) {
        this.comments = comments;
        this.assembler = assembler;
        this.pages = pages;
    }

    @GetMapping(path = "benches/{benchID}/comments", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAuthority('comments:read')")
    @Operation(summary = "Comentarios dun banco")
    public ResponseEntity<PagedModel<EntityModel<Comment>>> getByBench(
            @PathVariable("benchID") Long benchID,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) @Nullable Pageable pageable) {
        return ResponseEntity.ok(pages.toModel(comments.getByBench(benchID, pageable), Comment.class, assembler));
    }

    @GetMapping(path = "comments/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAuthority('comments:read')")
    @Operation(summary = "Obter un comentario")
    public ResponseEntity<EntityModel<Comment>> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(assembler.toModel(comments.get(id)));
    }

    @PostMapping(path = "benches/{benchID}/comments", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAuthority('comments:create')")
    @Operation(summary = "Comentar un banco")
    public ResponseEntity<EntityModel<Comment>> create(@PathVariable("benchID") Long benchID, @RequestBody @Valid Comment comment,
                                                       @Parameter(hidden = true) Authentication authentication) {
        Comment created = comments.create(benchID, authentication.getName(), comment);
        return ResponseEntity
                .created(linkTo(methodOn(CommentController.class).get(created.commentID())).toUri())
                .body(assembler.toModel(created));
    }

    @PatchMapping(path = "comments/{id}", consumes = "application/json-patch+json", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAuthority('comments:update') or @commentService.canModify(#id, authentication.name)")
    @Operation(summary = "Editar un comentario (JSON-Patch)", description = "Só o autor ou un administrador. Campo editable: text.")
    public ResponseEntity<EntityModel<Comment>> update(@PathVariable("id") Long id, @RequestBody List<JsonPatchOperation> changes) {
        return ResponseEntity.ok(assembler.toModel(comments.update(id, changes)));
    }

    @DeleteMapping(path = "comments/{id}")
    @PreAuthorize("hasAuthority('comments:delete') or @commentService.canModify(#id, authentication.name)")
    @Operation(summary = "Borrar un comentario", description = "Só o autor ou un administrador.")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        comments.delete(id);
        return ResponseEntity.noContent().build();
    }
}
