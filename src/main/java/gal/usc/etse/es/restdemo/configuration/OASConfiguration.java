package gal.usc.etse.es.restdemo.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
@SecuritySchemes({
        @SecurityScheme(
                type = SecuritySchemeType.HTTP,
                name = "jwt",
                description = "Standard JWT token authentication",
                in = SecuritySchemeIn.HEADER,
                paramName = HttpHeaders.AUTHORIZATION,
                scheme = "Bearer",
                bearerFormat = "JWT"
        )
})

@OpenAPIDefinition(
        info = @Info(
                title = "BenchMarks API",
                description = """
                        API REST para descubrir, valorar e compartir bancos públicos para sentar (un "Letterboxd" de bancos).

                        * Autenticación con JWT: `POST /auth/login` devolve o token na cabeceira `Authorization`.
                        * As coleccións devolven HAL (`application/hal+json`) con ligazóns de navegación e paxinación (`page`, `size`, `sort=campo,desc`).
                        * As modificacións parciais usan JSON-Patch (`application/json-patch+json`).
                        * Os erros devólvense como Problem Details (RFC 9457).
                        """,
                summary = "API REST de BenchMarks (Enxeñaría de Servizos, USC)",
                contact = @Contact(name = "Equipo BenchMarks"),
                license = @License(
                        name = "Apache 2.0",
                        identifier = "Apache-2.0",
                        url = "https://spdx.org/licenses/Apache-2.0.html"
                ),
                version = "1.0.0"
        ),
        servers = {
                @Server(
                        url = "http://localhost:18080",
                        description = "API para desenvolvemento"
                )
        },
        security = {
                @SecurityRequirement(
                        name = "jwt"
                )
        }
)
public class OASConfiguration {
}
