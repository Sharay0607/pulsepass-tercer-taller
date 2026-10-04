# PulsePass

Plataforma académica de eventos, artistas y entradas. Este repositorio contiene la **capa de persistencia** (Spring Boot, Spring Data JPA, Flyway, PostgreSQL, validada con Testcontainers) y la **capa de servicios** (reglas de negocio, transacciones, DTOs y MapStruct, validada con unit tests JUnit 5 + Mockito + AssertJ).

## Tecnologías

| Tecnología | Versión / Uso |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Spring Data JPA | Entidades y repositories (`JpaRepository`) |
| Flyway | Migraciones de esquema versionadas |
| PostgreSQL | Base de datos relacional |
| Testcontainers | Pruebas de integración contra un PostgreSQL real en contenedor |
| Lombok | Reducción de código repetitivo (`@Getter`, `@Setter`, `@NoArgsConstructor`) |
| MapStruct | 1.6.x — mapeo Entity → DTO en la capa de servicios |
| JUnit 5 / Mockito / AssertJ | Unit tests de la capa de servicios (sin Spring ni base de datos) |
| Maven (wrapper) | Construcción y ejecución de pruebas |

## Estructura del proyecto

```
src
├── main
│   ├── java/com/pulsepass
│   │   ├── domain          # Enums y entidades JPA
│   │   ├── repository      # Repositories con Query Methods y JPQL
│   │   ├── dto
│   │   │   ├── request     # Records de entrada (CreateEventRequest, RegisterUserRequest, PurchaseTicketRequest)
│   │   │   └── response    # Records de salida (VenueResponse, EventResponse, TicketResponse, ...)
│   │   ├── mapper          # Interfaces MapStruct (Entity → DTO)
│   │   ├── exception       # ResourceNotFound / DuplicateResource / BusinessRule
│   │   └── service         # Interfaces + TicketPriceCalculator
│   │       └── impl        # Implementaciones @Service
│   └── resources/db/migration
│       ├── V1__create_schema.sql
│       ├── V2__insert_initial_artists.sql
│       └── V3__add_streaming_url_to_event.sql
└── test/java/com/pulsepass
    ├── service             # Unit tests de la capa de servicios (Mockito)
    ├── AbstractIntegrationTest.java
    ├── EventArtistTest.java
    ├── TicketRepositoryTest.java
    ├── UserProfileTest.java
    └── VenueEventRelationTest.java
```

## Modelo de dominio

### Enums

| Enum | Valores |
|---|---|
| `EventCategory` | `MUSIC`, `SPORTS`, `TECHNOLOGY`, `EDUCATION`, `CULTURE`, `ENTERTAINMENT` |
| `EventStatus` | `DRAFT`, `PUBLISHED`, `SOLD_OUT`, `CANCELLED`, `FINISHED` |
| `TicketType` | `GENERAL`, `VIP`, `BACKSTAGE`, `STUDENT` |
| `TicketStatus` | `RESERVED`, `PAID`, `CANCELLED`, `USED` |

Todos los enums se persisten como texto (`@Enumerated(EnumType.STRING)`), de modo que reordenar o agregar valores no corrompe los datos existentes.

### Entidades

| Entidad | Tabla | Atributos principales |
|---|---|---|
| `Venue` | `venues` | `code` (único), `name`, `city`, `address`, `capacity`, `active` |
| `Event` | `events` | `eventCode` (único), `name`, `description`, `category`, `status`, `eventDate`, `minimumAge`, `streamingUrl` |
| `Artist` | `artists` | `stageName` (único), `country`, `genre`, `active` |
| `User` | `users` | `username` (único), `email` (único), `active` |
| `UserProfile` | `user_profiles` | `firstName`, `lastName`, `phone`, `city`, `birthDate` |
| `Ticket` | `tickets` | `ticketCode` (único), `type`, `price`, `status`, `purchaseDate` |

Además existe la tabla intermedia `event_artists`, que resuelve la relación muchos a muchos entre eventos y artistas. En total el esquema tiene **7 tablas**.

### Relaciones

```mermaid
erDiagram
    VENUES ||--o{ EVENTS : "alberga"
    EVENTS }o--o{ ARTISTS : "event_artists"
    USERS ||--|| USER_PROFILES : "tiene"
    USERS ||--o{ TICKETS : "compra"
    EVENTS ||--o{ TICKETS : "emite"
```

- **Venue 1 — N Event**: un venue alberga muchos eventos; cada evento pertenece a exactamente un venue (`@ManyToOne` en `Event`, `@OneToMany` en `Venue`).
- **Event N — M Artist**: relación pura sin datos propios, modelada con `@ManyToMany` y la tabla intermedia `event_artists` (PK compuesta `event_id, artist_id`).
- **User 1 — 1 UserProfile**: relación `@OneToOne`, reforzada con `UNIQUE` sobre la FK `user_id` en `user_profiles`.
- **User 1 — N Ticket** y **Event 1 — N Ticket**: `Ticket` se modela como entidad propia (no como `@ManyToMany`) porque contiene atributos propios de la asociación: `ticketCode`, `type`, `price`, `status` y `purchaseDate` (regla BR-006).

### Decisiones de diseño

- Las claves primarias usan `GenerationType.IDENTITY`.
- Las asociaciones `@ManyToOne` y `@OneToOne` se cargan de forma perezosa (`FetchType.LAZY`).
- Los precios se guardan como `BigDecimal` con `precision = 10` y `scale = 2`.
- Los campos de negocio identificadores (`code`, `eventCode`, `ticketCode`, `stageName`, `username`, `email`) tienen restricción `UNIQUE`.

## Migraciones Flyway

| Migración | Propósito |
|---|---|
| `V1__create_schema.sql` | Crea las 7 tablas del modelo con PK, FK, UNIQUE, CHECK e índices. |
| `V2__insert_initial_artists.sql` | Inserta el catálogo semilla de 5 artistas de prueba. |
| `V3__add_streaming_url_to_event.sql` | Agrega `streaming_url` (nullable) a `events`, sin modificar V1 ya aplicada. |

Hibernate corre en modo `ddl-auto: validate`: nunca crea ni modifica el esquema, solo verifica que las entidades coincidan con lo que Flyway construyó. **Flyway es la única fuente de verdad del esquema** (NFR-002). Los cambios posteriores al esquema se hacen con una nueva migración (como `V3`), nunca editando una migración ya aplicada.

## Consultas implementadas

| Consulta | Mecanismo | Justificación |
|---|---|---|
| Venue por código | Query Method | Condición simple sobre un campo |
| Evento por `eventCode` | Query Method | Condición simple sobre un campo |
| Eventos `PUBLISHED` ordenados por fecha | Query Method | Un filtro + un orden, expresable por nombre |
| Eventos de un venue por código | Query Method (navega relación) | Navegación de una sola relación |
| Tickets de un usuario por email y status | Query Method (navega relación) | Navegación de una sola relación |
| Usuario por email (case-insensitive) | Query Method | `IgnoreCase` soportado nativamente |
| Eventos por artista | JPQL (`@Query`) | Requiere JOIN explícito sobre colección `@ManyToMany` + `DISTINCT` |
| Eventos por ciudad y artista | JPQL (`@Query`) | Múltiples asociaciones combinadas |
| Eventos recomendados | JPQL (`@Query`) | Filtros combinados + `DISTINCT` + orden + `LIKE` case-insensitive |
| Conteo de tickets `PAID` por evento | JPQL (`@Query` con `COUNT`) | Agregación, no expresable como Query Method simple |
| Tickets PAID de un evento por eventCode | Query Method (navega relación) | Un filtro por estado navegando la relación Ticket → Event |
| Tickets de eventos futuros | JPQL (`@Query`) | Filtro sobre la fecha del evento asociado + orden cronológico |

**Criterio de elección:** se usan Query Methods cuando la consulta se expresa por nombre (un filtro, un orden o la navegación de una sola relación); se usa JPQL cuando hay JOIN explícito, varias asociaciones combinadas o agregaciones.

## Pruebas de integración

Las pruebas corren contra un **PostgreSQL real en contenedor** (Testcontainers), aplicando las migraciones Flyway igual que en un entorno real. Esto valida que el esquema, las entidades y las consultas funcionan en conjunto.

| Clase de prueba | Qué valida |
|---|---|
| `AbstractIntegrationTest` | Clase base con la configuración compartida de Testcontainers y PostgreSQL |
| `VenueEventRelationTest` | Relación Venue 1—N Event |
| `EventArtistTest` | Relación Event N—M Artist mediante `event_artists` |
| `UserProfileTest` | Relación User 1—1 UserProfile y restricción de unicidad |
| `TicketRepositoryTest` | Consultas sobre tickets y relaciones con User y Event |

En conjunto validan las relaciones entre entidades, las restricciones de unicidad y las consultas JPQL.

## Cómo ejecutar las pruebas

### Requisitos

- **JDK 21**
- **Docker** instalado y en ejecución (Testcontainers lo necesita para levantar PostgreSQL)

No hace falta instalar PostgreSQL de forma local.

### Comandos

Clonar el repositorio:

```bash
git clone https://github.com/Sharay0607/pulsepass-tercer-taller.git
cd pulsepass-tercer-taller
```

Ejecutar todas las pruebas con el Maven Wrapper:

```bash
# Linux / macOS
./mvnw test

# Windows
mvnw.cmd test
```

Ejecutar una clase de prueba específica:

```bash
./mvnw test -Dtest=TicketRepositoryTest
```

Al finalizar correctamente, Maven muestra `BUILD SUCCESS`.

> La primera ejecución puede tardar más porque Docker descarga la imagen de PostgreSQL.

---

# Capa de servicios

Frontera entre las futuras capas de exposición (controllers) y el modelo persistente. Aplica reglas de negocio, coordina repositories, controla transacciones y **nunca devuelve entidades JPA**: siempre DTOs (`record`) construidos con MapStruct.

```
Controller (futuro) → DTOs → Service (interface + impl) → Repository → Entity → PostgreSQL
                                   ├── Mapper (MapStruct)
                                   └── Reglas de negocio + @Transactional
```

## Servicios

| Servicio | Operaciones | Reglas principales |
|---|---|---|
| `VenueService` | `findByCode`, `findActiveVenues` | BR-VENUE-001..002 |
| `ArtistService` | `findById`, `findByStageName`, `findActiveArtists` | BR-ARTIST-001..002 |
| `EventService` | `create`, `findByCode`, `findPublishedEvents`, `publish`, `addArtist`, `findByArtist` | BR-EVENT-001..011 |
| `UserService` | `register` (User + UserProfile), `findByEmail`, `findByUsername` | BR-USER-001..005 |
| `TicketService` | `purchase`, `findByCode`, `findByUserEmail`, `findPaidTicketsByEvent`, `cancel`, `markAsUsed` | BR-TICKET-001..014 |

## Excepciones

| Excepción | Cuándo |
|---|---|
| `ResourceNotFoundException` | El recurso no existe (`Event not found: CMF-2026`) |
| `DuplicateResourceException` | Conflicto de unicidad (`Username already exists: andrea`) |
| `BusinessRuleException` | El recurso existe pero la operación no es válida (`User does not meet minimum age.`) |

## Flujo de compra (`TicketService.purchase`)

Una sola transacción (`@Transactional`): si cualquier paso falla se hace rollback completo.

```
Buscar User → ¿activo? → Buscar Event → ¿PUBLISHED? → ¿fecha futura? → ¿cumple edad (a la fecha del evento)?
→ ¿paidTickets < capacity? → Calcular precio → Crear Ticket (PAID) → save
→ si paidTickets + 1 == capacity → Event = SOLD_OUT (misma transacción) → TicketResponse
```

## Estrategia de precios

El precio **nunca** viene del cliente. `TicketPriceCalculator` (encapsulado y con tests propios) calcula `BigDecimal` = precio base × multiplicador:

| Tipo | Multiplicador |
|---|---|
| `GENERAL` | 1.00 |
| `STUDENT` | 0.70 |
| `VIP` | 2.00 |
| `BACKSTAGE` | 3.50 |

El precio base se configura con `pulsepass.pricing.base-price` (por defecto `50000.00`).

## Cambios en repositories (aditivos)

Se agregaron métodos que la capa de servicios necesita; no se modificó ninguno existente:
`VenueRepository.findByActiveTrueOrderByNameAsc`, `ArtistRepository.findByStageNameIgnoreCase` / `findByActiveTrueOrderByStageNameAsc`, `EventRepository.existsByEventCode`, `UserRepository.existsByUsername` / `existsByEmailIgnoreCase`, `TicketRepository.findByUser_EmailIgnoreCaseOrderByPurchaseDateDesc`.

## Unit tests de servicios

Arquitectura: `JUnit 5 → Service real → Repository mock + Mapper mock` (sin `@SpringBootTest`, sin PostgreSQL, sin Testcontainers). Cada test sigue ARRANGE / ACT / ASSERT y usa `when`, `verify`, `verify(..., never())`, `any()`, `eq()`.

| Clase | Cubre |
|---|---|
| `EventServiceImplTest` | TEST-EVENT-001..008 + duplicados, edad mínima, `addArtist` (BR-EVENT-010..011) |
| `UserServiceImplTest` | TEST-USER-001..004 + normalización de email, consultas |
| `TicketServiceImplTest` | TEST-TICKET-001..012 + edad en fecha del evento + escenario AC-004..AC-009 (aforo 3) |
| `TicketPriceCalculatorTest` | Multiplicadores, redondeo, precio nunca negativo |
| `VenueServiceImplTest`, `ArtistServiceImplTest` | Consultas y errores |

```bash
# Solo unit tests de servicios (no requieren Docker)
./mvnw test -Dtest='*ServiceImplTest,TicketPriceCalculatorTest'

# Todo (los tests de persistencia sí requieren Docker)
./mvnw clean test
```
