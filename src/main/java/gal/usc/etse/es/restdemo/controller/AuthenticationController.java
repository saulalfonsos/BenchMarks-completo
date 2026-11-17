package gal.usc.etse.es.restdemo.controller;

import com.fasterxml.jackson.annotation.JsonView;
import gal.usc.etse.es.restdemo.exception.DuplicateUserException;
import gal.usc.etse.es.restdemo.exception.InvalidRefreshTokenException;
import gal.usc.etse.es.restdemo.model.dto.User;
import gal.usc.etse.es.restdemo.service.AuthenticationService;
import gal.usc.etse.es.restdemo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.web.server.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@NullMarked
@RestController
@RequestMapping("auth")
@Tag(name = "Autenticación", description = "Rexistro, inicio de sesión con JWT, renovación e peche de sesión")
public class AuthenticationController {
    private static final String REFRESH_TOKEN_COOKIE_NAME = "__Secure-RefreshToken";
    private static final String REFRESH_PATH = "/auth";

    private final AuthenticationService authentication;
    private final UserService users;

    public AuthenticationController(AuthenticationService authentication, UserService users) {
        this.authentication = authentication;
        this.users = users;
    }

    @PostMapping(path = "login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAnonymous()")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión",
            description = "Devolve o JWT (válido 15 minutos) na cabeceira Authorization e un refresh token (7 días) nunha cookie HttpOnly.")
    @ApiResponse(responseCode = "204", headers = @Header(name = HttpHeaders.AUTHORIZATION, description = "Bearer <JWT>"))
    public ResponseEntity<Void> login(@RequestBody User user) {
        User loggedUser = authentication.login(user);
        return tokenResponse(loggedUser);
    }

    @PostMapping(path = "register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAnonymous()")
    @SecurityRequirements
    @JsonView(User.Views.Public.class)
    @Operation(summary = "Crear unha conta", description = "Os usuarios novos teñen sempre o rol USER.")
    public ResponseEntity<User> register(@RequestBody @Validated({Default.class, User.Views.Create.class}) User user) throws DuplicateUserException {
        User createdUser = users.create(user);

        return ResponseEntity.created(linkTo(methodOn(UserController.class).get(createdUser.userID())).toUri())
                .body(createdUser);
    }

    @PostMapping("refresh")
    @SecurityRequirements
    @Operation(summary = "Renovar o JWT",
            description = "Usa o refresh token da cookie (non precisa un JWT válido). O refresh token rótase en cada uso.")
    public ResponseEntity<Void> refresh(@CookieValue(name = REFRESH_TOKEN_COOKIE_NAME) String refreshToken) throws InvalidRefreshTokenException {
        User user = authentication.login(refreshToken);
        return tokenResponse(user);
    }

    @PostMapping("logout")
    @SecurityRequirements
    @Operation(summary = "Pechar sesión",
            description = "Invalida todos os refresh tokens do usuario e borra a cookie. O usuario identifícase polo refresh token da cookie "
                    + "ou, na súa falta, polo JWT, de modo que funciona aínda que o JWT xa caducase.")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) @Nullable String refreshToken,
                                       @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) @Nullable String token) {
        authentication.logout(refreshToken, token == null ? null : token.replaceFirst("^Bearer ", ""));
        ResponseCookie cookie = refreshCookie("", Duration.ZERO);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    private ResponseEntity<Void> tokenResponse(User loggedUser) {
        String refreshToken = authentication.regenerateRefreshToken(loggedUser);

        return ResponseEntity.noContent()
                .headers(headers -> headers.setBearerAuth(loggedUser.password()))
                .header(HttpHeaders.SET_COOKIE, refreshCookie(refreshToken, Duration.ofDays(7)).toString())
                .build();
    }

    private static ResponseCookie refreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, value)
                .secure(true)
                .httpOnly(true)
                .sameSite(Cookie.SameSite.STRICT.toString())
                .path(REFRESH_PATH)
                .maxAge(maxAge)
                .build();
    }
}
