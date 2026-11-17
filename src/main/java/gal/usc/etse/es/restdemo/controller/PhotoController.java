package gal.usc.etse.es.restdemo.controller;

import gal.usc.etse.es.restdemo.controller.hal.Pages;
import gal.usc.etse.es.restdemo.controller.hal.PhotoAssembler;
import gal.usc.etse.es.restdemo.exception.InvalidPhotoException;
import gal.usc.etse.es.restdemo.model.dto.Photo;
import gal.usc.etse.es.restdemo.service.PhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@NullMarked
@RestController
@Tag(name = "Fotos", description = "Subida e descarga de fotos dos bancos (JPEG ou PNG, máximo 5 MB)")
public class PhotoController {
    private final PhotoService photos;
    private final PhotoAssembler assembler;
    private final Pages pages;

    public PhotoController(PhotoService photos, PhotoAssembler assembler, Pages pages) {
        this.photos = photos;
        this.assembler = assembler;
        this.pages = pages;
    }

    @GetMapping(path = "benches/{benchID}/photos", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAuthority('photos:read')")
    @Operation(summary = "Fotos dun banco (metadatos)")
    public ResponseEntity<PagedModel<EntityModel<Photo>>> getByBench(
            @PathVariable("benchID") Long benchID,
            @ParameterObject @PageableDefault(size = 20, sort = "photoID", direction = Sort.Direction.ASC) @Nullable Pageable pageable) {
        return ResponseEntity.ok(pages.toModel(photos.getByBench(benchID, pageable), Photo.class, assembler));
    }

    @PostMapping(path = "benches/{benchID}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAuthority('photos:create')")
    @Operation(summary = "Subir unha foto dun banco",
            description = "Envíase como multipart/form-data na parte 'file'. A miniatura xérase de forma asíncrona.")
    public ResponseEntity<EntityModel<Photo>> upload(@PathVariable("benchID") Long benchID,
                                                     @RequestPart("file") MultipartFile file,
                                                     @Parameter(hidden = true) Authentication authentication) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new InvalidPhotoException("The uploaded file could not be read");
        }
        Photo created = photos.upload(benchID, authentication.getName(), bytes);
        return ResponseEntity
                .created(linkTo(methodOn(PhotoController.class).get(created.photoID())).toUri())
                .body(assembler.toModel(created));
    }

    @GetMapping(path = "photos/{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAuthority('photos:read')")
    @Operation(summary = "Metadatos dunha foto")
    public ResponseEntity<EntityModel<Photo>> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(assembler.toModel(photos.get(id)));
    }

    @GetMapping(path = "photos/{id}/content")
    @SecurityRequirements
    @Operation(summary = "Descargar a imaxe",
            description = "Público para poder usarse directamente en etiquetas <img>. Responde con Cache-Control e ETag (If-None-Match → 304).")
    @ApiResponse(responseCode = "200", content = {
            @Content(mediaType = "image/jpeg", schema = @Schema(type = "string", format = "binary")),
            @Content(mediaType = "image/png", schema = @Schema(type = "string", format = "binary"))
    })
    public ResponseEntity<byte[]> content(@PathVariable("id") Long id,
                                          @Parameter(description = "Devolver a miniatura (se xa está xerada)")
                                          @RequestParam(defaultValue = "false") boolean thumbnail) {
        var content = photos.getContent(id, thumbnail);
        return ResponseEntity.ok()
                .contentType(content.mediaType())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(content.bytes());
    }

    @DeleteMapping(path = "photos/{id}")
    @PreAuthorize("hasAuthority('photos:delete') or @photoService.canModify(#id, authentication.name)")
    @Operation(summary = "Borrar unha foto", description = "Só quen a subiu ou un administrador.")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        photos.delete(id);
        return ResponseEntity.noContent().build();
    }
}
