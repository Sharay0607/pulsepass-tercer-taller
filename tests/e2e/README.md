# PulsePass — Pruebas E2E manuales con Postman (Fase 5)

Guía para ejecutar **a mano** las 22 rutas de PulsePass (20 de negocio + `POST /api/auth/login` + `GET /api/auth/me`)
contra la API real y PostgreSQL, sin mocks. Las pruebas se hacen **desde Postman con el botón Send**, comparando la
respuesta real con la esperada y anotando el resultado en `registro-ejecucion-manual.md`.

> Los resultados esperados salen de leer el código de la rama `feature/security-fase4` (controllers, services,
> `SecurityConfig`, `AuthorizationService`, DTOs). **Esta guía no es un reporte de ejecución**: nada está marcado como PASS
> hasta que tú lo ejecutes y lo anotes.

## 1. ¿Hay que cambiar el proyecto?

**No se toca ningún archivo de `src/`.** Un E2E prueba la API desplegada tal como está. Lo único que se agrega al repo es
esta carpeta `tests/e2e/`:

```
tests/e2e/
├── README.md                      ← esta guía
├── checklist-e2e-manual.md        ← orden de ejecución + matriz de 22 rutas + trazabilidad TC-*
├── registro-ejecucion-manual.md   ← tabla para anotar esperado / observado / PASS-FAIL-BLOCKED
├── postman/
│   ├── PulsePass-Manual-E2E.postman_collection.json   ← 163 solicitudes, SIN scripts
│   └── PulsePass-local.postman_environment.json       ← variables (sin tokens)
├── fixtures/
│   ├── seed-e2e.sql               ← venues + roles STAFF/ADMIN + usuario inactivo
│   ├── revocar-staff-e2e.sql      ← opcional (TC-SEC-017)
│   ├── cleanup-e2e.sql            ← para repetir la corrida
│   └── README.md
└── .gitignore
```

Lo que sí hay que **preparar** (no es código del producto):

1. Una base de datos **separada** `pulsepass_e2e` (para no tocar tus datos de desarrollo).
2. Un secreto JWT (`PULSEPASS_JWT_SECRET_BASE64`); sin él la app **no arranca**.
3. Venues de prueba: la migración V2 solo siembra artistas, **no hay venues**, y la API no tiene `POST /api/venues`.
4. Los roles `STAFF`/`ADMIN` no se pueden asignar por HTTP (por diseño): se promueven con SQL en la BD de pruebas.

## 2. Requisitos

Java 21, PostgreSQL con `psql` en el PATH, Postman Desktop (o Web con Agent) y este repo clonado en la rama `feature/security-fase4`.

## 3. Crear la BD y el secreto

```bash
psql -U postgres -c "CREATE DATABASE pulsepass_e2e;"
openssl rand -base64 32          # copia el resultado: es tu PULSEPASS_JWT_SECRET_BASE64
```
*(Windows sin openssl, en PowerShell: `[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Maximum 256 }))`; sirve para una BD de pruebas.)*

## 4. Arrancar la API contra la BD E2E

`application.yml` apunta a la BD `pulsepass`, pero Spring Boot permite sobreescribirlo con variables de entorno, así que no hay que editarlo.

**Bash / Git Bash**
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/pulsepass_e2e
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export PULSEPASS_JWT_SECRET_BASE64=<tu_secreto>
./mvnw spring-boot:run
```

**PowerShell**
```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/pulsepass_e2e"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="postgres"
$env:PULSEPASS_JWT_SECRET_BASE64="<tu_secreto>"
.\mvnw.cmd spring-boot:run
```

Flyway aplica V1–V4 al arrancar. Cuando veas la app escuchando en `:8080`, **primera ejecución del seed** (crea los venues):

```bash
psql -U postgres -d pulsepass_e2e -f tests/e2e/fixtures/seed-e2e.sql
```

## 5. Importar en Postman

1. *Import* → selecciona los dos JSON de `tests/e2e/postman/`.
2. Arriba a la derecha elige el entorno **PulsePass-local**.
3. Revisa las variables del entorno:
   - `baseUrl` = `http://localhost:8080`.
   - `eventDate` = `2027-06-15T20:00:00` → **debe ser una fecha futura**; cámbiala si ya pasó.
   - `artistId` = `1` → en una BD limpia *Solar Beat* es el id 1; confírmalo con la solicitud `ART-03a` y ajusta si difiere.
   - Las contraseñas del entorno son **ejemplos desechables** (solo existen en la BD E2E). No uses contraseñas reales.
4. Los tokens (`userAToken`, `staffToken`, …) empiezan vacíos y son tipo *secret*.

## 6. Cómo se ejecuta cada solicitud

1. Abre la solicitud (están numeradas `001 · [E2E-…]` y el nombre ya dice actor y HTTP esperado).
2. Lee su **Description**: precondición, qué inspeccionar y qué copiar.
3. Pulsa **Send**.
4. Compara: **código HTTP exacto** (200 ≠ 201, no vale “cualquier 2xx”), cuerpo JSON y, si falla, que sea un `ErrorResponse`
   (`timestamp, status, error, message, details`) sin trazas ni datos internos. Un `500` en un caso nominal es FAIL.
5. Anota en `registro-ejecucion-manual.md`: HTTP observado y estado `PASS` / `FAIL` / `BLOCKED`.

**Copiar tokens y códigos a mano** (no hay scripts):
- En la respuesta del login copia el valor de `accessToken` → Environment → variable del actor (p. ej. `userAToken`) → columna **Current value** (no *Initial value*, para que no se sincronice ni se exporte).
- Tras comprar un ticket copia `ticketCode` a `ticketT1`, `ticketT2`, `ticketT3`, `ticketB` según indique la solicitud.
- El JWT dura **15 minutos**: si de pronto todo da `401`, repite los logins (`AUTH-01a`, `01c`, `01e`, `01f`) y actualiza los tokens. No hace falta volver a registrar.

## 7. Orden de ejecución (resumen)

Sigue el orden numérico. Hitos importantes:

| Paso | Qué hacer |
|---|---|
| 001–009 | Flujo A público (venues, artistas). |
| Carpeta 03 | Registrar A, B, STAFF, ADMIN, INACTIVE → login A, B, INACTIVE (**copiar tokens**). |
| **PAUSA SQL** | **2.ª ejecución** de `seed-e2e.sql` (promueve STAFF/ADMIN y desactiva INACTIVE). |
| Resto de 03 | Login STAFF y ADMIN (**copiar tokens**), login INACTIVE ahora → 401, `me` de los 4 actores. |
| Carpeta 04 | Propiedad de usuarios. |
| Carpeta 05 | Flujo C: ADMIN crea, asocia artistas y publica; visibilidad de borradores. |
| Carpeta 06 | Flujo D: compras, propiedad, cancelar y usar tickets (**copiar `ticketCode`**). |
| Carpetas 07–08 | Seguridad negativa y regresión de errores. |

El detalle paso a paso con casillas está en `checklist-e2e-manual.md`.

## 8. Casos que necesitan preparación especial

- **`SEC-03` JWT alterado:** copia el valor de `userAToken` a `tamperedToken` y cambia **un carácter de la firma** (la parte final, tras el último punto).
- **`SEC-04` JWT expirado:** guarda un token en `expiredToken` y espera más de 15 minutos (o genera uno vencido con una clave **de test**). Si no es viable, márcalo `BLOCKED`.
- **`SEC-05` token con otra clave:** solo si lo construyes offline con una clave de test **que no sea la de la app**. Si no, `BLOCKED`.
- **`SEC-08`:** exige haber obtenido `inactiveToken` *antes* de la pausa SQL.
- **`SEC-09/10/11`:** si `SEC-09` responde 400 (rechaza el campo `role`), también es aceptable; lo inaceptable es que el usuario quede ADMIN. En ese caso marca 10 y 11 como `N/A`.
- **`SEC-12`** (opcional, TC-SEC-017): `revocar-staff-e2e.sql` y luego la solicitud; al final restaura con `seed-e2e.sql`.

## 9. Políticas que la colección da por esperadas (según el código)

- Recurso **ajeno** (ticket, usuario por email/username, cancelar ticket) → **403**, incluso si el recurso no existe (anti-enumeración). STAFF/ADMIN que consultan algo inexistente reciben **404**.
- **Borrador** de evento para anónimo/USER → **404** (oculto). STAFF y ADMIN sí lo ven.
- Sin token en ruta protegida, token inválido/expirado/inactivo → **401**; un Bearer inválido también da 401 en rutas públicas.
- `ADMIN`: crear, publicar y asociar artistas. `STAFF`/`ADMIN`: marcar ticket como usado y listar tickets pagados. Cancelar: propietario o ADMIN (STAFF **no**).
- El rol de `STAFF` es global (no por evento): es una limitación conocida del MVP, no un diseño para producción.
- Compra válida → ticket **PAID**; al llenar el aforo el evento pasa a **SOLD_OUT**. Reglas de negocio incumplidas → **409**.

Si lo que observas difiere de esto, **no cambies la API para acomodar la colección**: anótalo como FAIL con esperado vs observado.

## 10. Repetir la corrida desde cero

```bash
psql -U postgres -d pulsepass_e2e -f tests/e2e/fixtures/cleanup-e2e.sql   # borra datos E2E (no venues/artistas)
```
Vacía los tokens y `ticket*` del entorno, y vuelve a empezar (el seed es idempotente). Los scripts abortan si la BD no se llama `pulsepass_e2e`.

## 11. Seguridad de la evidencia

No subas al repo ni compartas tokens, contraseñas ni exports del entorno con *Current value*. En capturas, oculta el header `Authorization`.

## 12. Problemas frecuentes

| Síntoma | Causa probable |
|---|---|
| La app no arranca | Falta `PULSEPASS_JWT_SECRET_BASE64`. |
| `404` en `VEN-01a` | No se ejecutó la 1.ª vez `seed-e2e.sql` (o BD distinta). |
| `409 Event date must be in the future` al crear eventos | `eventDate` ya pasó: actualízala. |
| `401` en todo de repente | Token de 15 min vencido: repetir logins. |
| Login de STAFF/ADMIN sin privilegios | No se ejecutó la 2.ª vez `seed-e2e.sql` antes del login, o los usernames no son `e2e_staff` / `e2e_admin`. |
| `409 Username/Email already exists` al registrar | Ya corriste antes: usa `cleanup-e2e.sql`. |
| `ART-01a` no devuelve Solar Beat | El id no es 1: ajusta `artistId`. |
