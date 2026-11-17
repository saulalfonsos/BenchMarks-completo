# BenchMarks

API REST para descubrir, valorar y compartir bancos públicos para sentarse: un "Letterboxd de bancos".
Proyecto de la asignatura Enxeñaría de Servizos (USC, curso 2026/27). Incluye un cliente web en `/`.

## Requisitos

- **JDK 17 o superior** para lanzar Gradle. El proyecto compila con Java 25; si no lo tienes, Gradle lo descarga solo.
- **Docker** (Docker Desktop abierto). Postgres y Redis se levantan automáticamente desde `compose.yaml`.
- Puertos libres: `18080` (API), `15432` (Postgres) y `6379` (Redis).
- Conexión a internet para la predicción meteorológica (sin ella la API sigue funcionando, ver [Sombra](#predicción-de-sombra)).

## Ejecutar

```bash
./gradlew bootRun        # en Windows: gradlew.bat bootRun
```

Al arrancar se crean los contenedores, se recrea el esquema y se cargan los datos de prueba
(`src/main/resources/sql`) y 30 fotos de ejemplo. **La base de datos se reinicia en cada arranque.**

| Qué | Dónde |
|---|---|
| Cliente web | http://localhost:18080 |
| Swagger UI | http://localhost:18080/docs/swagger |
| Scalar | http://localhost:18080/docs/scalar |
| OpenAPI (JSON) | http://localhost:18080/docs/oas |

Usuarios de prueba (contraseña `1234`): `user`, `ana`, `brais` (rol USER) y `admin` (rol ADMIN).

## Pruebas

```bash
./gradlew test                 # tests unitarios (no necesitan Docker)
./scripts/smoke-test.sh        # 76 comprobaciones end-to-end contra la API arrancada (requiere curl y jq)
```

`src/test/queries.http` tiene una petición de ejemplo para cada funcionalidad, lista para lanzar desde IntelliJ o VS Code.

## Funcionalidades

| Funcionalidad (lista del foro) | Endpoints |
|---|---|
| Crear usuario / iniciar sesión | `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout` |
| Añadir, editar y eliminar banco | `POST /benches`, `PATCH /benches/{id}`, `DELETE /benches/{id}` |
| Búsqueda y filtrado | `GET /benches?q=&type=&covered=&minMark=&owner=&lat=&lon=&radius=&sort=&page=&size=` |
| Valorar banco | `GET/POST /benches/{id}/reviews`, `GET/PATCH/DELETE /reviews/{id}`, `GET /reviews`, `GET /users/{id}/reviews` |
| Añadir comentarios | `GET/POST /benches/{id}/comments`, `GET/PATCH/DELETE /comments/{id}` |
| Subida de fotos | `GET/POST /benches/{id}/photos` (multipart), `GET/DELETE /photos/{id}`, `GET /photos/{id}/content[?thumbnail=true]` |
| Añadir a la lista (guardados) | `GET /users/{id}/favorites`, `PUT/DELETE /users/{id}/favorites/{benchId}` |
| Likes | `GET /users/{id}/likes`, `PUT/DELETE /users/{id}/likes/{benchId}` |
| Saber si hay sombra en un intervalo de horas | `GET /benches/{id}/shade?date=&from=&to=` |
| Perfil | `GET /users`, `GET/PUT/PATCH/DELETE /users/{id}` |

### Predicción de sombra

`GET /benches/{id}/shade` devuelve, para cada hora local del intervalo, si el banco estará a la sombra y por qué:

| Motivo | Regla |
|---|---|
| `COVERED` | El banco está bajo techo o árboles (`covered=true`) |
| `NIGHT` | El sol está bajo el horizonte |
| `LOW_SUN` | Elevación solar < 10°: las sombras de edificios y árboles cubren el banco |
| `OVERCAST` | Nubosidad prevista ≥ 75 % |
| `DIRECT_SUN` | Le da el sol |

Combina tres fuentes:

- los datos del banco;
- la posición del sol, calculada en el servidor con el algoritmo de la NOAA (`shade/SolarPosition`);
- la nubosidad horaria del servicio externo [Open-Meteo](https://open-meteo.com), gratuito y sin clave.

Si Open-Meteo no responde, o la fecha cae fuera de su rango (unos 60 días atrás y 15 adelante), la respuesta usa solo la posición solar e indica `source=ASTRONOMICAL`.

## Decisiones de diseño (contenidos de la asignatura)

| Tema | Cómo se aplica | Dónde |
|---|---|---|
| Diseño de URLs y verbos | Recursos en plural y subrecursos (`/benches/{id}/reviews`). PUT/DELETE idempotentes para favoritos y likes (un like por usuario, frente al antiguo `POST /like`). | `controller/*` |
| Códigos de estado y cabeceras | 201 + `Location`, 204, 304, 400, 401 + `WWW-Authenticate`, 403, 404, 409 + `Location` del recurso existente, 413, 415 + `Accept`, 422 | controladores, `ErrorController` |
| Gestión centralizada de errores | `@RestControllerAdvice`; todos los errores (incluidos los 401/403 de Spring Security) como Problem Details (RFC 9457), con `errors` por campo en las validaciones | `ErrorController`, `SecurityConfiguration` |
| Validación | Bean Validation en los DTOs, en los parámetros de consulta y **también sobre el resultado de cada JSON-Patch**. Grupo de validación propio para el registro. | `model/dto/*`, `service/Patcher` |
| PATCH / JSON-Patch | `application/json-patch+json` en bancos, valoraciones, comentarios y usuarios. Solo se copian los campos editables, así que los calculados (media, propietario...) no se pueden falsear. | `service/Patcher`, `utils/patch` |
| HATEOAS | Respuestas HAL: cada recurso enlaza a sus relaciones (`reviews`, `photos`, `shade{?date,from,to}`, `owner`...) y las colecciones incluyen enlaces de paginación. El cliente web navega siguiendo esos enlaces. | `controller/hal/*` |
| Paginación y consultas avanzadas | `Pageable` estándar (`page`, `size`, `sort=campo,desc`), con 400 si el campo de ordenación no existe. Búsqueda dinámica con JPA Specifications. Proyecciones JPQL a DTO para no cargar los binarios de las fotos. `@Formula` para el contador de likes. | `repository/*` |
| Serialización con Jackson | `@JsonView`: la contraseña nunca sale en respuestas (vista `Public`). Campos de solo lectura documentados en el esquema. | `model/dto/User` |
| Versionado | Cabecera `API-Version` (o `?version=`): `GET /users` v0 devuelve una lista, v1 (por defecto) una página HAL. Una versión desconocida da 400. | `APIConfiguration`, `UserController` |
| Seguridad | JWT firmado con RSA, refresh token rotatorio en cookie `HttpOnly`/`Secure`/`SameSite=Strict` guardado en Redis. Permisos por recurso en la BD (`benches:update`...) con jerarquía de roles, más reglas de propiedad en `@PreAuthorize` (el autor o el propietario puede modificar; el admin, todo). El autor de una valoración o comentario sale siempre del token, nunca del cuerpo. | `SecurityConfiguration`, `auth.sql`, `*Service.canModify` |
| Cachés | Servidor: Redis, con TTL por caché (bancos 10 min, meteorología 1 h); `@Cacheable`/`@CacheEvict` invalidan al modificar, dar like o recalcular medias. HTTP: `ETag` + `If-None-Match` → 304 y `Cache-Control` en las imágenes. | `CacheConfiguration`, `BenchService`, `WeatherService` |
| Transacciones | `@Transactional(readOnly = true)` en lecturas; las escrituras son atómicas. El recálculo de medias corre en una transacción nueva (`REQUIRES_NEW`) solo después del commit. | servicios, `event/RatingUpdater` |
| Eventos y tareas asíncronas | `ReviewChangedEvent` → `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` recalcula la media. `PhotoUploadedEvent` genera la miniatura en segundo plano, así que el 201 llega sin esperar. | `event/*` |
| Tareas programadas | `@Scheduled` cada noche (04:00, Europe/Madrid) reconcilia las medias de todos los bancos | `event/RatingUpdater` |
| Resiliencia | Cliente de Open-Meteo con timeouts, `@Retryable` con backoff exponencial, `@ConcurrencyLimit` y fallback a cálculo astronómico | `shade/OpenMeteoClient`, `shade/WeatherService` |
| Documentación OpenAPI | Tags, resúmenes y descripciones por operación, parámetros con ejemplos, errores globales documentados, esquema de seguridad JWT | `OASConfiguration`, anotaciones en controladores |

### Modelo de datos

```
User ─┬─< Review >── Bench ──< Photo (bytea + miniatura)
      ├─< Comment >──┘  │
      ├─< favorites >───┤ (user_favorite_benches)
      ├─< likes >───────┘ (user_liked_benches)
      └─< owns Bench
Role ─< permissions, role_hierarchy (ADMIN ⊃ USER)
```

Al borrar un banco o un usuario, la base de datos borra en cascada (`ON DELETE CASCADE`) sus valoraciones, comentarios, fotos, favoritos y likes.
Si se borra un usuario, sus bancos quedan sin propietario (`SET NULL`).
