package gal.usc.etse.es.restdemo.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonView;
import gal.usc.etse.es.restdemo.model.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@Relation(collectionRelation = "users", itemRelation = "user")
public record User(
        @JsonView(Views.Public.class)
        @JsonAlias("username")
        @NotBlank(groups = Views.Create.class)
        @Pattern(regexp = "[a-zA-Z0-9_.-]{3,30}", message = "debe ter entre 3 e 30 letras, números, '.', '_' ou '-'", groups = Views.Create.class)
        @Schema(example = "user")
        String userID,

        @JsonView(Views.Public.class)
        @Size(max = 60)
        String name,

        @JsonView(Views.Public.class)
        @Size(max = 100)
        String lastName,

        @JsonView(Views.Public.class)
        @Size(max = 30)
        String nickname,

        @JsonView(Views.Public.class)
        @Past
        LocalDate birthday,

        @JsonView(Views.Public.class)
        @Size(max = 100)
        String location,

        @JsonView(Views.Private.class)
        @NotBlank(groups = Views.Create.class)
        @Size(min = 4, max = 100)
        @Schema(accessMode = Schema.AccessMode.WRITE_ONLY, example = "1234")
        String password,

        @JsonView(Views.Private.class)
        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        Set<String> roles
) {
    public static User from(gal.usc.etse.es.restdemo.model.entity.User user) {
        return new User(
                user.getUserID(),
                user.getName(),
                user.getLastName(),
                user.getNickname(),
                user.getBirthday(),
                user.getLocation(),
                user.getPassword(),
                user.getRoles().stream().map(Role::getRolename).collect(Collectors.toSet())
        );
    }

    public interface Views {
        interface Public {}
        interface Private extends Public {}
        /** Grupo de validación usado só no rexistro */
        interface Create {}
    }
}
