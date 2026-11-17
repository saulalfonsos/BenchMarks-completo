package gal.usc.etse.es.restdemo.configuration;

import gal.usc.etse.es.restdemo.filter.JWTFilter;
import gal.usc.etse.es.restdemo.service.AuthenticationService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    private final JWTFilter jwtFilter;
    private final AuthenticationService authenticationService;
    private final JsonMapper mapper;

    public SecurityConfiguration(JWTFilter jwtFilter, AuthenticationService authenticationService, JsonMapper mapper) {
        this.jwtFilter = jwtFilter;
        this.authenticationService = authenticationService;
        this.mapper = mapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http.authorizeHttpRequests(authorize ->
                        authorize.requestMatchers("/auth/**").permitAll()
                                .requestMatchers("/error").permitAll()
                                .requestMatchers("/docs/**", "/swagger-ui/**").permitAll()
                                .requestMatchers("/actuator/health").permitAll()
                                // As imaxes cárganse dende etiquetas <img>, que non poden enviar a cabeceira Authorization
                                .requestMatchers(HttpMethod.GET, "/photos/*/content").permitAll()
                                .requestMatchers("/", "/index.html", "/favicon.*", "/img/**").permitAll()
                                .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterAfter(jwtFilter, BasicAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, ex) ->
                                writeProblem(response, HttpStatus.UNAUTHORIZED, "unauthorized", "Authentication required",
                                        "A valid JWT must be sent in the Authorization header (Bearer <token>)"))
                        .accessDeniedHandler((request, response, ex) ->
                                writeProblem(response, HttpStatus.FORBIDDEN, "forbidden", "Access denied",
                                        "You do not have permission to perform this operation"))
                )
                .build();
    }

    private void writeProblem(HttpServletResponse response, HttpStatus status, String type, String title, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("/error/" + type));
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        if (status == HttpStatus.UNAUTHORIZED) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        mapper.writeValue(response.getOutputStream(), problem);
    }

    /**
     * Xerarquía de roles e permisos gardada na base de datos (táboas roles, role_hierarchy e role_permissions).
     * Cárgase na primeira comprobación de permisos, cando os datos xa están inicializados.
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        return new RoleHierarchy() {
            private volatile RoleHierarchy delegate;

            @Override
            public Collection<? extends GrantedAuthority> getReachableGrantedAuthorities(Collection<? extends GrantedAuthority> authorities) {
                RoleHierarchy hierarchy = delegate;
                if (hierarchy == null) {
                    hierarchy = delegate = authenticationService.loadRoleHierarchy();
                }
                return hierarchy.getReachableGrantedAuthorities(authorities);
            }
        };
    }
}
