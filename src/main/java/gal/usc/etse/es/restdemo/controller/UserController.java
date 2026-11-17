package gal.usc.etse.es.restdemo.controller;

import com.fasterxml.jackson.annotation.JsonView;
import gal.usc.etse.es.restdemo.controller.hal.BenchAssembler;
import gal.usc.etse.es.restdemo.controller.hal.Pages;
import gal.usc.etse.es.restdemo.controller.hal.UserAssembler;
import gal.usc.etse.es.restdemo.model.dto.Bench;
import gal.usc.etse.es.restdemo.model.dto.User;
import gal.usc.etse.es.restdemo.service.UserService;
import gal.usc.etse.es.utils.patch.JsonPatchOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@NullMarked
@RestController
@RequestMapping("users")
@Tag(name = "Usuarios", description = "Perfís, lista de bancos gardados (favoritos) e likes")
public class UserController {

    private final UserService users;
    private final UserAssembler assembler;
    private final BenchAssembler benchAssembler;
    private final Pages pages;

    public UserController(UserService users, UserAssembler assembler, BenchAssembler benchAssembler, Pages pages) {
        this.users = users;
        this.assembler = assembler;
        this.benchAssembler = benchAssembler;
        this.pages = pages;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, version = "0")
    @PreAuthorize("hasAuthority('users:read')")
    @JsonView(User.Views.Public.class)
    @Operation(summary = "Listar usuarios (v0, sen paxinar)", description = "Versión antiga mantida por compatibilidade. Seleccionar coa cabeceira API-Version: 0.")
    public ResponseEntity<List<User>> get() {
        return ResponseEntity.ok(users.get());
    }

    @GetMapping(produces = MediaTypes.HAL_JSON_VALUE, version = "1")
    @PreAuthorize("hasAuthority('users:read')")
    @JsonView(User.Views.Public.class)
    @Operation(summary = "Listar usuarios (v1, paxinado)", description = "Versión por defecto.")
    public ResponseEntity<PagedModel<EntityModel<User>>> get(@ParameterObject @PageableDefault(sort = "userID") @Nullable Pageable pageable) {
        return ResponseEntity.ok(pages.toModel(users.get(pageable), User.class, assembler));
    }

    @GetMapping(path = "{userID}", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAuthority('users:read')")
    @JsonView(User.Views.Public.class)
    @Operation(summary = "Perfil público dun usuario")
    public ResponseEntity<EntityModel<User>> get(@PathVariable("userID") String userID) {
        return ResponseEntity.ok(assembler.toModel(users.get(userID)));
    }

    @PutMapping(path = "{userID}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("#userID == authentication.name or hasAuthority('users:update')")
    @JsonView(User.Views.Public.class)
    @Operation(summary = "Substituír o perfil", description = "Só o propio usuario ou un administrador. O contrasinal só cambia se se envía.")
    public ResponseEntity<EntityModel<User>> replace(@PathVariable("userID") String userID, @RequestBody @Valid User user) {
        return ResponseEntity.ok(assembler.toModel(users.replace(userID, user)));
    }

    @PatchMapping(path = "{userID}", consumes = "application/json-patch+json", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("#userID == authentication.name or hasAuthority('users:update')")
    @JsonView(User.Views.Public.class)
    @Operation(summary = "Modificar o perfil (JSON-Patch)", description = "Só o propio usuario ou un administrador. Ex.: [{\"op\":\"replace\",\"path\":\"/location\",\"value\":\"Lugo\"}]")
    public ResponseEntity<EntityModel<User>> update(@PathVariable("userID") String userID, @RequestBody List<JsonPatchOperation> changes) {
        return ResponseEntity.ok(assembler.toModel(users.update(userID, changes)));
    }

    @DeleteMapping(path = "{userID}")
    @PreAuthorize("#userID == authentication.name or hasAuthority('users:delete')")
    @Operation(summary = "Borrar a conta", description = "Só o propio usuario ou un administrador. Borra tamén as súas valoracións, comentarios e fotos.")
    public ResponseEntity<Void> delete(@PathVariable("userID") String userID) {
        users.delete(userID);
        return ResponseEntity.noContent().build();
    }

    // --- Favoritos: lista de bancos gardados ---

    @GetMapping(path = "{userID}/favorites", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAuthority('benches:read')")
    @Operation(summary = "Lista de bancos gardados dun usuario")
    public ResponseEntity<PagedModel<EntityModel<Bench>>> getFavorites(@PathVariable("userID") String userID,
                                                                       @ParameterObject @PageableDefault(sort = "benchID") @Nullable Pageable pageable) {
        return ResponseEntity.ok(pages.toModel(users.getFavorites(userID, pageable), Bench.class, benchAssembler));
    }

    @PutMapping(path = "{userID}/favorites/{benchID}")
    @PreAuthorize("#userID == authentication.name or hasAuthority('users:update')")
    @Operation(summary = "Gardar un banco na lista", description = "Idempotente: repetir a petición non ten efecto adicional.")
    public ResponseEntity<Void> addFavorite(@PathVariable("userID") String userID, @PathVariable("benchID") Long benchID) {
        users.addFavorite(userID, benchID);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(path = "{userID}/favorites/{benchID}")
    @PreAuthorize("#userID == authentication.name or hasAuthority('users:update')")
    @Operation(summary = "Quitar un banco da lista")
    public ResponseEntity<Void> removeFavorite(@PathVariable("userID") String userID, @PathVariable("benchID") Long benchID) {
        users.removeFavorite(userID, benchID);
        return ResponseEntity.noContent().build();
    }

    // --- Likes ---

    @GetMapping(path = "{userID}/likes", produces = MediaTypes.HAL_JSON_VALUE)
    @PreAuthorize("hasAuthority('benches:read')")
    @Operation(summary = "Bancos que lle gustan a un usuario")
    public ResponseEntity<PagedModel<EntityModel<Bench>>> getLikes(@PathVariable("userID") String userID,
                                                                   @ParameterObject @PageableDefault(sort = "benchID") @Nullable Pageable pageable) {
        return ResponseEntity.ok(pages.toModel(users.getLikes(userID, pageable), Bench.class, benchAssembler));
    }

    @PutMapping(path = "{userID}/likes/{benchID}")
    @PreAuthorize("#userID == authentication.name or hasAuthority('users:update')")
    @Operation(summary = "Darlle like a un banco", description = "Idempotente: un like por usuario e banco.")
    public ResponseEntity<Void> like(@PathVariable("userID") String userID, @PathVariable("benchID") Long benchID) {
        users.like(userID, benchID);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(path = "{userID}/likes/{benchID}")
    @PreAuthorize("#userID == authentication.name or hasAuthority('users:update')")
    @Operation(summary = "Quitar o like")
    public ResponseEntity<Void> unlike(@PathVariable("userID") String userID, @PathVariable("benchID") Long benchID) {
        users.unlike(userID, benchID);
        return ResponseEntity.noContent().build();
    }
}
