package gal.usc.etse.es.restdemo.controller;

import gal.usc.etse.es.restdemo.controller.hal.BenchAssembler;
import gal.usc.etse.es.restdemo.controller.hal.Pages;
import gal.usc.etse.es.restdemo.model.dto.Bench;
import gal.usc.etse.es.restdemo.model.dto.ShadeReport;
import gal.usc.etse.es.restdemo.service.BenchService;
import gal.usc.etse.es.restdemo.shade.ShadeService;
import gal.usc.etse.es.utils.patch.JsonPatchOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@NullMarked
@RestController
@RequestMapping("benches")
@Tag(name = "Bancos", description = "Alta, busca, edición e borrado de bancos, e predición de sombra")
public class BenchController {
    private final BenchService benches;
    private final ShadeService shade;
    private final BenchAssembler assembler;
    private final Pages pages;
    private final ZoneId zone;

    public BenchController(BenchService benches, ShadeService shade, BenchAssembler assembler, Pages pages,
                           @Value("${benchmarks.timezone:Europe/Madrid}") ZoneId zone) {
        this.benches = benches;
        this.shade = shade;
        this.assembler = assembler;
        this.pages = pages;
        this.zone = zone;
    }

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAuthority('benches:read')")
    @Operation(summary = "Buscar e filtrar bancos",
            description = "Todos os filtros son opcionais e combínanse. A busca xeográfica require lat, lon e radius á vez.")
    public ResponseEntity<PagedModel<EntityModel<Bench>>> search(
            @Parameter(description = "Texto libre sobre nome, descrición e tipo", example = "alameda")
            @RequestParam(required = false) @Nullable String q,
            @Parameter(description = "Tipo exacto de banco", example = "Stone")
            @RequestParam(required = false) @Nullable String type,
            @Parameter(description = "Só bancos cubertos (true) ou ao descuberto (false)")
            @RequestParam(required = false) @Nullable Boolean covered,
            @Parameter(description = "Nota media mínima", example = "7")
            @RequestParam(required = false) @Min(0) @Max(10) @Nullable Double minMark,
            @Parameter(description = "Usuario que deu de alta o banco", example = "user")
            @RequestParam(required = false) @Nullable String owner,
            @Parameter(description = "Latitude do centro da busca", example = "42.8805")
            @RequestParam(required = false) @DecimalMin("-90") @DecimalMax("90") @Nullable Double lat,
            @Parameter(description = "Lonxitude do centro da busca", example = "-8.5456")
            @RequestParam(required = false) @DecimalMin("-180") @DecimalMax("180") @Nullable Double lon,
            @Parameter(description = "Radio en km (máximo 50)", example = "1.5")
            @RequestParam(required = false) @DecimalMin("0.01") @DecimalMax("50") @Nullable Double radius,
            @ParameterObject @PageableDefault(sort = "benchID") @Nullable Pageable pageable
    ) {
        var filter = new BenchService.Filter(q, type, covered, minMark, owner, lat, lon, radius);
        return ResponseEntity.ok(pages.toModel(benches.search(filter, pageable), Bench.class, assembler));
    }

    @GetMapping(path = "{id}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAuthority('benches:read')")
    @Operation(summary = "Obter un banco")
    public ResponseEntity<EntityModel<Bench>> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(assembler.toModel(benches.get(id)));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAuthority('benches:create')")
    @Operation(summary = "Dar de alta un banco", description = "O usuario autenticado queda como propietario do banco.")
    public ResponseEntity<EntityModel<Bench>> create(@RequestBody @Valid Bench bench, @Parameter(hidden = true) Authentication authentication) {
        Bench created = benches.create(bench, authentication.getName());
        return ResponseEntity
                .created(linkTo(methodOn(BenchController.class).get(created.benchID())).toUri())
                .body(assembler.toModel(created));
    }

    @PatchMapping(path = "{id}", consumes = "application/json-patch+json", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAuthority('benches:update') or @benchService.canModify(#id, authentication.name)")
    @Operation(summary = "Modificar un banco (JSON-Patch)", description = "Só o propietario ou un administrador. Campos editables: name, description, latitude, longitude, type, covered.")
    public ResponseEntity<EntityModel<Bench>> update(@PathVariable("id") Long id, @RequestBody List<JsonPatchOperation> changes) {
        return ResponseEntity.ok(assembler.toModel(benches.update(id, changes)));
    }

    @DeleteMapping(path = "{id}")
    @PreAuthorize("hasAuthority('benches:delete') or @benchService.canModify(#id, authentication.name)")
    @Operation(summary = "Borrar un banco", description = "Só o propietario ou un administrador. Borra tamén as súas valoracións, comentarios e fotos.")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        benches.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(path = "{id}/shade", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAuthority('benches:read')")
    @Operation(summary = "Predición de sombra por horas",
            description = """
                    Indica para cada hora do intervalo se o banco estará á sombra e por que: está cuberto (COVERED),
                    é de noite (NIGHT), o sol está moi baixo (LOW_SUN), o ceo está cuberto (OVERCAST) ou dálle o sol (DIRECT_SUN).
                    Combina a posición solar calculada polo servidor coa nubosidade prevista polo servizo externo Open-Meteo;
                    se este non está dispoñible (ou a data está fóra do seu rango) a resposta indica source=ASTRONOMICAL.
                    """)
    public ResponseEntity<EntityModel<ShadeReport>> shade(
            @PathVariable("id") Long id,
            @Parameter(description = "Día (por defecto, hoxe)", example = "2026-11-20")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate date,
            @Parameter(description = "Hora local inicial (0-23, por defecto 8)", example = "10")
            @RequestParam(required = false) @Min(0) @Max(23) @Nullable Integer from,
            @Parameter(description = "Hora local final (0-23, por defecto 20)", example = "18")
            @RequestParam(required = false) @Min(0) @Max(23) @Nullable Integer to
    ) {
        ShadeReport report = shade.forecast(id,
                date == null ? LocalDate.now(zone) : date,
                from == null ? 8 : from,
                to == null ? 20 : to);

        return ResponseEntity.ok(EntityModel.of(report,
                linkTo(methodOn(BenchController.class).shade(id, report.date(), from, to)).withSelfRel(),
                linkTo(methodOn(BenchController.class).get(id)).withRel("bench")));
    }
}
