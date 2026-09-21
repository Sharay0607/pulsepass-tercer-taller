# PulsePass

Plataforma académica de eventos, artistas y entradas. Capa de persistencia implementada con Spring Boot 4, Spring Data JPA, Flyway y PostgreSQL, validada con pruebas de integración usando Testcontainers.

## Modelo de dominio

- **Venue 1 --- N Event**: un venue alberga muchos eventos; cada evento pertenece a exactamente un venue (`@ManyToOne` en Event, `@OneToMany` en Venue).
- **Event N --- M Artist**: relación pura sin datos propios, modelada con `@ManyToMany` y tabla intermedia `event_artists` (PK compuesta `event_id, artist_id`).
- **User 1 --- 1 UserProfile**: relación `@OneToOne`, reforzada con `UNIQUE` sobre la FK `user_id` en `user_profiles`.
- **User 1 --- N Ticket** y **Event 1 --- N Ticket**: Ticket se modela como entidad propia (no `@ManyToMany`) porque contiene atributos propios de la asociación: `ticketCode`, `type`, `price`, `status`, `purchaseDate` (regla BR-006).

## Migraciones Flyway

| Migración | Propósito |
|---|---|
| `V1__create_schema.sql` | Crea las 7 tablas del modelo con PK, FK, UNIQUE, CHECK e índices. |
| `V2__insert_initial_artists.sql` | Inserta el catálogo semilla de 5 artistas de prueba. |
| `V3__add_streaming_url_to_event.sql` | Agrega `streaming_url` (nullable) a `events`, sin modificar V1 ya aplicada. |

Hibernate corre en modo `ddl-auto: validate`: nunca crea ni modifica el esquema, solo verifica que las entidades coincidan con lo que Flyway construyó. Flyway es la única fuente de verdad del esquema (NFR-002).

## Consultas implementadas

| Consulta | Mecanismo | Justificación |
|---|---|---|
| Venue por código | Query Method | Condición simple sobre un campo |
| Evento por eventCode | Query Method | Condición simple sobre un campo |
| Eventos PUBLISHED ordenados por fecha | Query Method | Un filtro + un orden, expresable por nombre |
| Eventos de un venue por código | Query Method (navega relación) | Navegación de una sola relación |
| Tickets de un usuario por email y status | Query Method (navega relación) | Navegación de una sola relación |
| Usuario por email (case-insensitive) | Query Method | `IgnoreCase` soportado nativamente |
| Eventos por artista | JPQL (`@Query`) | Requiere JOIN explícito sobre colección `@ManyToMany` + `DISTINCT` |
| Eventos por ciudad y artista | JPQL (`@Query`) | Múltiples asociaciones combinadas |
| Eventos recomendados | JPQL (`@Query`) | Filtros combinados + `DISTINCT` + orden + `LIKE` case-insensitive |
| Conteo de tickets PAID por evento | JPQL (`@Query` con `COUNT`) | Agregación, no expresable como Query Method simple |

## Cómo ejecutar las pruebas