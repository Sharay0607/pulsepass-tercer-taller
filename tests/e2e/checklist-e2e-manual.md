# Checklist E2E manual — PulsePass

Marca cada paso cuando lo ejecutes. El **orden numérico es obligatorio** (hay dependencias: tokens, códigos de ticket, estados irreversibles).
Resultado real → `registro-ejecucion-manual.md`. Un 2xx por sí solo no es PASS.

## 0. Antes de empezar

- [ ] BD `pulsepass_e2e` creada y vacía; API arrancada contra ella (README §3-4).
- [ ] `seed-e2e.sql` ejecutado (1.ª vez: venues).
- [ ] Colección y entorno importados; entorno **PulsePass-local** seleccionado (README §5).
- [ ] Si repites la corrida: `cleanup-e2e.sql` ejecutado antes.

## 1. Secuencia

### 00 Preparación / conectividad

_Confirma que la API responde antes de empezar. No existe /health en el proyecto, por eso se usa una consulta publica._

| ✔ | Paso | ID | Actor | Esperado | Qué comprobar |
|---|---|---|---|---|---|
| ☐ | 001 | `E2E-PRE-01` | ANÓNIMO | 200 | La API responde. Lista con los 5 artistas sembrados por V2 (Solar Beat, Neon Waves, Caribbean Sound, Ocean Drive, Digital Pulse). |

### 01 Venues (2 rutas)

_Requiere venues del fixture (seed-e2e.sql, parte 1)._

| ✔ | Paso | ID | Actor | Esperado | Qué comprobar |
|---|---|---|---|---|---|
| ☐ | 002 | `E2E-VEN-01a` | ANÓNIMO | 200 | code=E2E-VEN-01, capacity=500, active=true. |
| ☐ | 003 | `E2E-VEN-01b` | ANÓNIMO | 404 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 004 | `E2E-VEN-02a` | ANÓNIMO | 200 | Incluye E2E-VEN-01 y E2E-VEN-SMALL; NO incluye E2E-VEN-OFF. Todos con active=true. |

### 02 Artists (3 rutas)

_Los artistas vienen de la migración V2 (no hay POST de artistas)._

| ✔ | Paso | ID | Actor | Esperado | Qué comprobar |
|---|---|---|---|---|---|
| ☐ | 005 | `E2E-ART-01a` | ANÓNIMO | 200 | id y stageName coinciden con el fixture (id 1 = Solar Beat en BD limpia). |
| ☐ | 006 | `E2E-ART-01b` | ANÓNIMO | 404 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 007 | `E2E-ART-02a` | ANÓNIMO | 200 | stageName=Solar Beat, active=true. |
| ☐ | 008 | `E2E-ART-02b` | ANÓNIMO | 404 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 009 | `E2E-ART-03a` | ANÓNIMO | 200 | Lista; todos con active=true. |

### 03 Auth / usuarios (registro, login, me)

_Orden obligatorio: registrar los 5 usuarios → login A, B, INACTIVE → PAUSA: ejecutar seed-e2e.sql parte 2 (roles/estado) → login STAFF, ADMIN → me._

| ✔ | Paso | ID | Actor | Esperado | Qué comprobar |
|---|---|---|---|---|---|
| ☐ | 010 | `E2E-USR-01a` | ANÓNIMO | 201 | Sin JWT en la respuesta. Campos de UserResponse; NO debe aparecer password/hash. active=true. |
| ☐ | 011 | `E2E-USR-01b` | ANÓNIMO | 201 | Sin JWT en la respuesta. Campos de UserResponse; NO debe aparecer password/hash. |
| ☐ | 012 | `E2E-USR-01c` | ANÓNIMO | 201 | Sin JWT en la respuesta. Campos de UserResponse; NO debe aparecer password/hash. |
| ☐ | 013 | `E2E-USR-01d` | ANÓNIMO | 201 | Sin JWT en la respuesta. Campos de UserResponse; NO debe aparecer password/hash. |
| ☐ | 014 | `E2E-USR-01e` | ANÓNIMO | 201 | Sin JWT en la respuesta. Campos de UserResponse; NO debe aparecer password/hash. |
| ☐ | 015 | `E2E-AUTH-01a` | ANÓNIMO | 200 | accessToken no vacío, tokenType=Bearer, expiresIn=900. 📋 Copiar: accessToken → variable userAToken. |
| ☐ | 016 | `E2E-AUTH-01b` | ANÓNIMO | 200 | 200 con token. NO es necesario copiarlo; en la siguiente solicitud (me) usa el userAToken del paso anterior. |
| ☐ | 017 | `E2E-AUTH-01c` | ANÓNIMO | 200 | accessToken Bearer. 📋 Copiar: accessToken → variable userBToken. |
| ☐ | 018 | `E2E-AUTH-01d` | ANÓNIMO | 200 | 200 con token (la cuenta se desactiva más adelante). 📋 Copiar: accessToken → variable inactiveToken. |
| ☐ | — | **MANUAL** | — | — | **PAUSA SQL: ejecutar fixtures/seed-e2e.sql PARTE 2 (promueve e2e_staff→STAFF, e2e_admin→ADMIN y desactiva e2e_inactive).** |
| ☐ | 019 | `E2E-AUTH-01e` | ANÓNIMO | 200 | accessToken Bearer. 📋 Copiar: accessToken → variable staffToken. |
| ☐ | 020 | `E2E-AUTH-01f` | ANÓNIMO | 200 | accessToken Bearer. 📋 Copiar: accessToken → variable adminToken. |
| ☐ | 021 | `E2E-AUTH-01g` | ANÓNIMO | 401 | 401 genérico (mismo mensaje que credenciales malas). Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 022 | `E2E-AUTH-01h` | ANÓNIMO | 401 | 401 genérico, sin revelar si el usuario existe. |
| ☐ | 023 | `E2E-AUTH-01i` | ANÓNIMO | 401 | 401 con el MISMO mensaje que AUTH-01h (sin enumeración). |
| ☐ | 024 | `E2E-AUTH-01j` | ANÓNIMO | 400 | 400 Bean Validation; details con identifier y password. |
| ☐ | 025 | `E2E-AUTH-02a` | USER_A | 200 | userId, username=e2e_usera, roles=[USER]. Sin email, hash ni secretos. |
| ☐ | 026 | `E2E-AUTH-02b` | USER_B | 200 | Identidad de B (distinta de A), roles=[USER]. |
| ☐ | 027 | `E2E-AUTH-02c` | STAFF | 200 | roles incluye STAFF. |
| ☐ | 028 | `E2E-AUTH-02d` | ADMIN | 200 | roles incluye ADMIN. |

### 04 Usuarios / consulta y propiedad (2 rutas)

_Política demostrada: recurso ajeno → 403 siempre (también si no existe). Solo el propio usuario o ADMIN._

| ✔ | Paso | ID | Actor | Esperado | Qué comprobar |
|---|---|---|---|---|---|
| ☐ | 029 | `E2E-USR-02a` | USER_A | 200 | Datos de A. Sin password/hash. |
| ☐ | 030 | `E2E-USR-02b` | USER_B | 403 | 403 + ErrorResponse. NO hay datos personales de A. |
| ☐ | 031 | `E2E-USR-02c` | ANÓNIMO | 401 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 032 | `E2E-USR-02d` | STAFF | 403 | STAFF no es ADMIN ni propietario → 403. |
| ☐ | 033 | `E2E-USR-02e` | ADMIN | 200 | Datos de A. |
| ☐ | 034 | `E2E-USR-02f` | ADMIN | 404 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 035 | `E2E-USR-02g` | USER_A | 403 | 403 (no distingue 'no existe' de 'es de otro'). |
| ☐ | 036 | `E2E-USR-03a` | USER_A | 200 | Datos de A. |
| ☐ | 037 | `E2E-USR-03b` | USER_B | 403 | 403; sin datos de A. |
| ☐ | 038 | `E2E-USR-03c` | ANÓNIMO | 401 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 039 | `E2E-USR-03d` | ADMIN | 200 | Datos de A. |
| ☐ | 040 | `E2E-USR-03e` | ADMIN | 404 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |

### 05 Events / administración y visibilidad (6 rutas)

_Flujo C. Requiere adminToken. Los eventos nacen en DRAFT; E2E-EVT-DRAFT se queda en borrador toda la prueba._

| ✔ | Paso | ID | Actor | Esperado | Qué comprobar |
|---|---|---|---|---|---|
| ☐ | 041 | `E2E-EVT-01a` | ADMIN | 201 | status=DRAFT, venueCode=E2E-VEN-01, artists=[]. |
| ☐ | 042 | `E2E-EVT-01b` | ADMIN | 201 | status=DRAFT, minimumAge=18. |
| ☐ | 043 | `E2E-EVT-01c` | ADMIN | 201 | status=DRAFT, venueCode=E2E-VEN-SMALL. |
| ☐ | 044 | `E2E-EVT-01d` | ADMIN | 201 | status=DRAFT, minimumAge=99. |
| ☐ | 045 | `E2E-EVT-02a` | ADMIN | 200 | status=DRAFT visible para ADMIN. |
| ☐ | 046 | `E2E-EVT-02b` | STAFF | 200 | status=DRAFT visible para STAFF. |
| ☐ | 047 | `E2E-EVT-02c` | ANÓNIMO | 404 | 404 'Recurso no encontrado'. NO se filtra ningún dato del borrador. |
| ☐ | 048 | `E2E-EVT-02d` | USER_A | 404 | 404; sin datos del borrador. |
| ☐ | 049 | `E2E-EVT-03a` | ANÓNIMO | 200 | NO aparece ningún E2E-EVT-* (todos siguen en DRAFT). |
| ☐ | 050 | `E2E-EVT-05a` | ADMIN | 200 | artists contiene el artista (id={{artistId}}). |
| ☐ | 051 | `E2E-EVT-05b` | ADMIN | 200 | artists contiene el artista. |
| ☐ | 052 | `E2E-EVT-05c` | ADMIN | 409 | 409 'already associated'. Sin duplicar la relación. |
| ☐ | 053 | `E2E-EVT-05d` | USER_A | 403 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 054 | `E2E-EVT-05e` | ANÓNIMO | 401 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 055 | `E2E-EVT-05f` | STAFF | 403 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 056 | `E2E-EVT-05g` | ADMIN | 404 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 057 | `E2E-EVT-05h` | ADMIN | 404 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 058 | `E2E-EVT-06a` | ANÓNIMO | 200 | Lista SIN los borradores (vacía o sin E2E-EVT-*). |
| ☐ | 059 | `E2E-EVT-06b` | STAFF | 200 | Incluye E2E-EVT-DRAFT y E2E-EVT-PUB (STAFF ve todos los estados). |
| ☐ | 060 | `E2E-EVT-04a` | ADMIN | 200 | status=PUBLISHED. |
| ☐ | 061 | `E2E-EVT-04b` | ADMIN | 200 | status=PUBLISHED. |
| ☐ | 062 | `E2E-EVT-04c` | ADMIN | 200 | status=PUBLISHED. |
| ☐ | 063 | `E2E-EVT-04d` | ADMIN | 409 | 409 'Only DRAFT events can be published'. |
| ☐ | 064 | `E2E-EVT-04e` | USER_A | 403 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 065 | `E2E-EVT-04f` | STAFF | 403 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 066 | `E2E-EVT-04g` | ANÓNIMO | 401 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 067 | `E2E-EVT-04h` | ADMIN | 404 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 068 | `E2E-EVT-02e` | ANÓNIMO | 200 | status=PUBLISHED, artists con el artista asociado, venueCode/venueName. |
| ☐ | 069 | `E2E-EVT-02f` | ANÓNIMO | 404 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 070 | `E2E-EVT-02g` | ANÓNIMO | 404 | Tras publicar los otros, el DRAFT sigue oculto. |
| ☐ | 071 | `E2E-EVT-03b` | ANÓNIMO | 200 | Incluye E2E-EVT-PUB, E2E-EVT-SMALL, E2E-EVT-AGE99; NO incluye E2E-EVT-DRAFT. |
| ☐ | 072 | `E2E-EVT-06c` | ANÓNIMO | 200 | Incluye E2E-EVT-PUB; NO incluye E2E-EVT-DRAFT. |
| ☐ | 073 | `E2E-EVT-06d` | STAFF | 200 | Incluye E2E-EVT-PUB (PUBLISHED) y E2E-EVT-DRAFT (DRAFT). |
| ☐ | 074 | `E2E-EVT-06e` | ANÓNIMO | 400 | 400; details indica stageName requerido. |
| ☐ | 075 | `E2E-EVT-06f` | ANÓNIMO | 200 | 200 con lista vacía (verificar y anotar lo observado). |
| ☐ | 076 | `E2E-EVT-01e` | ANÓNIMO | 401 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 077 | `E2E-EVT-01f` | USER_A | 403 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 078 | `E2E-EVT-01g` | STAFF | 403 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 079 | `E2E-EVT-01h` | ADMIN | 409 | 409 'Event code already exists'. |
| ☐ | 080 | `E2E-EVT-01i` | ADMIN | 404 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 081 | `E2E-EVT-01j` | ADMIN | 409 | 409 'Venue is not active'. |
| ☐ | 082 | `E2E-EVT-01k` | ADMIN | 409 | 409 'Event date must be in the future'. |
| ☐ | 083 | `E2E-EVT-02h` | ADMIN | 404 | 404: ninguno de los intentos 401/403/404/409 creó el evento E2E-EVT-NEG. |

### 06 Tickets / compra, consulta y ciclo de vida (6 rutas)

_Flujo D. T1 se cancela, T2 se usa, T3 sirve para probar denegaciones y luego lo cancela ADMIN. Una denegación se prueba ANTES de cambiar el estado._

| ✔ | Paso | ID | Actor | Esperado | Qué comprobar |
|---|---|---|---|---|---|
| ☐ | 084 | `E2E-TKT-01a` | USER_A | 201 | status=PAID, userEmail=A, eventCode=E2E-EVT-PUB, price > 0 (lo calcula el sistema). 📋 Copiar: ticketCode → variable ticketT1. |
| ☐ | 085 | `E2E-TKT-01b` | USER_A | 201 | status=PAID, type=VIP. 📋 Copiar: ticketCode → variable ticketT2. |
| ☐ | 086 | `E2E-TKT-01c` | USER_A | 201 | status=PAID, type=STUDENT. 📋 Copiar: ticketCode → variable ticketT3. |
| ☐ | 087 | `E2E-TKT-01d` | USER_B | 201 | status=PAID, userEmail=B. 📋 Copiar: ticketCode → variable ticketB. |
| ☐ | 088 | `E2E-TKT-02a` | USER_A | 200 | ticketCode=T1, userEmail=A, status=PAID. |
| ☐ | 089 | `E2E-TKT-02b` | USER_B | 403 | 403; sin datos del ticket. |
| ☐ | 090 | `E2E-TKT-02c` | ANÓNIMO | 401 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 091 | `E2E-TKT-02d` | STAFF | 200 | STAFF puede verificar entradas. |
| ☐ | 092 | `E2E-TKT-02e` | USER_A | 403 | 403; sin datos. |
| ☐ | 093 | `E2E-TKT-02f` | USER_A | 403 | 403 (no distingue 'no existe' de 'ajeno'). |
| ☐ | 094 | `E2E-TKT-02g` | STAFF | 404 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 095 | `E2E-TKT-03a` | USER_A | 200 | Contiene T1, T2, T3; NO contiene ticketB. |
| ☐ | 096 | `E2E-TKT-03b` | USER_B | 403 | 403; sin información. |
| ☐ | 097 | `E2E-TKT-03c` | ANÓNIMO | 401 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 098 | `E2E-TKT-03d` | ADMIN | 200 | Lista de A. |
| ☐ | 099 | `E2E-TKT-03e` | STAFF | 403 | STAFF no es ADMIN ni propietario. |
| ☐ | 100 | `E2E-TKT-03f` | USER_A | 400 | 400; details indica email requerido. |
| ☐ | 101 | `E2E-TKT-03g` | USER_B | 200 | Contiene solo ticketB. |
| ☐ | 102 | `E2E-TKT-04a` | STAFF | 200 | Incluye T1, T2, T3 y ticketB; todos PAID. |
| ☐ | 103 | `E2E-TKT-04b` | ADMIN | 200 | Misma lista. |
| ☐ | 104 | `E2E-TKT-04c` | USER_A | 403 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 105 | `E2E-TKT-04d` | ANÓNIMO | 401 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 106 | `E2E-TKT-01e` | ANÓNIMO | 401 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 107 | `E2E-TKT-01f` | USER_A | 403 | 403; NO se crea ticket. |
| ☐ | 108 | `E2E-TKT-03h` | USER_B | 200 | Solo ticketB: la compra denegada (TKT-01f) no creó nada. |
| ☐ | 109 | `E2E-TKT-01g` | USER_A | 409 | 409 'only be purchased for PUBLISHED events'. |
| ☐ | 110 | `E2E-TKT-01h` | USER_A | 404 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 111 | `E2E-TKT-01i` | USER_A | 409 | 409 'does not meet minimum age'. |
| ☐ | 112 | `E2E-TKT-01j` | USER_A | 400 | 400 'Malformed or invalid JSON request' (enum inexistente). |
| ☐ | 113 | `E2E-TKT-01k` | USER_A | 400 | 400 Validation failed; details.userEmail. |
| ☐ | 114 | `E2E-TKT-01l` | USER_A | 400 | 400; details con campos obligatorios. |
| ☐ | 115 | `E2E-TKT-01m` | USER_A | 201 | 201 status=PAID. 📋 Copiar: ticketCode → variable ticketSmall (opcional). |
| ☐ | 116 | `E2E-TKT-02h` | ANÓNIMO | 200 | status=SOLD_OUT (se llenó el aforo de 1). |
| ☐ | 117 | `E2E-TKT-01n` | USER_B | 409 | 409; sin ticket nuevo (el evento ya no está PUBLISHED). |
| ☐ | 118 | `E2E-TKT-06a` | USER_B | 403 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 119 | `E2E-TKT-06b` | USER_A | 403 | 403: USER no puede marcar usado. |
| ☐ | 120 | `E2E-TKT-06c` | ANÓNIMO | 401 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 121 | `E2E-TKT-02i` | USER_A | 200 | status=PAID: los 401/403 no mutaron nada. |
| ☐ | 122 | `E2E-TKT-05a` | USER_B | 403 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 123 | `E2E-TKT-05b` | STAFF | 403 | Solo propietario o ADMIN. |
| ☐ | 124 | `E2E-TKT-05c` | ANÓNIMO | 401 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 125 | `E2E-TKT-02j` | USER_A | 200 | status=PAID. |
| ☐ | 126 | `E2E-TKT-05d` | USER_A | 200 | status=CANCELLED. |
| ☐ | 127 | `E2E-TKT-05e` | USER_A | 409 | 409 'Only PAID tickets can be cancelled'. |
| ☐ | 128 | `E2E-TKT-02k` | USER_A | 200 | status=CANCELLED. |
| ☐ | 129 | `E2E-TKT-06d` | STAFF | 409 | 409 'CANCELLED ticket cannot be used'. |
| ☐ | 130 | `E2E-TKT-06e` | STAFF | 200 | status=USED. |
| ☐ | 131 | `E2E-TKT-06f` | STAFF | 409 | 409 'Only PAID tickets can be used'. |
| ☐ | 132 | `E2E-TKT-05f` | USER_A | 409 | 409; el ticket USED no se cancela. |
| ☐ | 133 | `E2E-TKT-02l` | STAFF | 200 | status=USED. |
| ☐ | 134 | `E2E-TKT-05g` | ADMIN | 200 | status=CANCELLED (ADMIN autorizado). |
| ☐ | 135 | `E2E-TKT-05h` | ADMIN | 404 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 136 | `E2E-TKT-06g` | STAFF | 404 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 137 | `E2E-TKT-04e` | STAFF | 200 | Solo ticketB (T1 cancelado, T2 usado, T3 cancelado ya no están). |

### 07 Seguridad / escenarios negativos

_Tokens manipulados e inyección de rol. Los tokens manipulados se preparan a mano (ver README §8)._

| ✔ | Paso | ID | Actor | Esperado | Qué comprobar |
|---|---|---|---|---|---|
| ☐ | 138 | `E2E-SEC-01` | ANÓNIMO | 401 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 139 | `E2E-SEC-02` | Bearer inválido | 401 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 140 | `E2E-SEC-03` | JWT alterado | 401 | Cuerpo ErrorResponse con timestamp, status, error, message, details. |
| ☐ | 141 | `E2E-SEC-04` | JWT expirado | 401 | Cuerpo ErrorResponse con timestamp, status, error, message, details. ⚠️ Precondición: Requiere un token vencido (esperar 15 min tras un login, o generarlo con clave de test). Si no es viable: BLOCKED. |
| ☐ | 142 | `E2E-SEC-05` | JWT otra clave | 401 | Cuerpo ErrorResponse con timestamp, status, error, message, details. ⚠️ Precondición: Token creado offline con una clave de test (nunca la real). Si no es viable: BLOCKED. |
| ☐ | 143 | `E2E-SEC-06` | ANÓNIMO | 401 | 401: el servidor solo acepta Bearer en el header. |
| ☐ | 144 | `E2E-SEC-07` | Bearer inválido | 401 | 401: un Bearer inválido se rechaza aunque la ruta sea pública (filtro JWT). |
| ☐ | 145 | `E2E-SEC-08` | INACTIVE | 401 | 401: el filtro recarga el usuario y está inactivo. ⚠️ Precondición: Parte 2 del seed ejecutada; inactiveToken obtenido ANTES de desactivar. |
| ☐ | 146 | `E2E-SEC-09` | ANÓNIMO | 201 | 201 (el campo extra se ignora) o 400 (si lo rechaza). Nunca debe quedar ADMIN. |
| ☐ | 147 | `E2E-SEC-10` | ANÓNIMO | 200 | accessToken Bearer. 📋 Copiar: accessToken → variable injectToken. |
| ☐ | 148 | `E2E-SEC-11` | USER inyectado | 200 | roles = [USER] únicamente (sin ADMIN ni STAFF). |
| ☐ | — | **MANUAL** | — | — | **OPCIONAL (TC-SEC-017): ejecutar fixtures/revocar-staff-e2e.sql para quitar el rol STAFF a e2e_staff SIN pedir un nuevo login.** |
| ☐ | 149 | `E2E-SEC-12` | STAFF | 403 | 403: el filtro recarga los roles desde la BD en cada petición; el token viejo ya no da privilegios STAFF. |
| ☐ | — | **MANUAL** | — | — | **OPCIONAL: restaurar el rol ejecutando de nuevo fixtures/seed-e2e.sql (parte de roles) y repetir AUTH-01e si hace falta.** |

### 08 Validaciones y errores HTTP

_Regresión de errores (Flujo E). Todo error debe traer ErrorResponse. Ningún escenario nominal debe devolver 500._

| ✔ | Paso | ID | Actor | Esperado | Qué comprobar |
|---|---|---|---|---|---|
| ☐ | 150 | `E2E-VAL-01` | ANÓNIMO | 400 | 400 Validation failed; details por cada campo obligatorio. |
| ☐ | 151 | `E2E-VAL-02` | ANÓNIMO | 400 | 400; details.email. |
| ☐ | 152 | `E2E-VAL-03` | ANÓNIMO | 400 | 400; details.password. |
| ☐ | 153 | `E2E-VAL-04` | ANÓNIMO | 409 | 409 'Birth date cannot be in the future'. |
| ☐ | 154 | `E2E-VAL-05` | ANÓNIMO | 409 | 409 'Username already exists'. |
| ☐ | 155 | `E2E-VAL-06` | ANÓNIMO | 409 | 409 'Email already exists'. |
| ☐ | 156 | `E2E-VAL-07` | ANÓNIMO | 400 | 400 'Malformed or invalid JSON request'. |
| ☐ | 157 | `E2E-VAL-08` | ADMIN | 400 | 400; details con eventCode, name, category, etc. |
| ☐ | 158 | `E2E-VAL-09` | ADMIN | 400 | 400 'Malformed or invalid JSON request'. |
| ☐ | 159 | `E2E-VAL-10` | ADMIN | 400 | 400; details.minimumAge. |
| ☐ | 160 | `E2E-VAL-11` | ADMIN | 400 | 400 'Malformed or invalid JSON request'. |
| ☐ | 161 | `E2E-VAL-12` | ANÓNIMO | 400 | 400 'Invalid request parameter'. |
| ☐ | 162 | `E2E-VAL-13` | ADMIN | 404 | 404 'Resource not found' en formato ErrorResponse. |
| ☐ | 163 | `E2E-VAL-14` | ADMIN | 405 | 405 'HTTP method not supported'. |

### 09 Flujos de negocio

No hay solicitudes propias; ver el orden numérico anterior (Flujo A: 001-009, B: carpeta 03+07, C: carpeta 05, D: carpeta 06, E: carpeta 08).

## 2. Matriz de cobertura: 22 rutas

| Ruta | Endpoint | Casos (IDs) | Actores usados | Positivos | Negativos |
|---|---|---|---|---|---|
| VEN-01 | `GET /api/venues/{code}` | VEN-01a, VEN-01b | ANÓNIMO | 1 | 1 |
| VEN-02 | `GET /api/venues/active` | VEN-02a, SEC-07 | ANÓNIMO, Bearer inválido | 1 | 1 |
| EVT-01 | `POST /api/events` | EVT-01a, EVT-01b, EVT-01c, EVT-01d, EVT-01e, EVT-01f, EVT-01g, EVT-01h, EVT-01i, EVT-01j, EVT-01k, VAL-08, VAL-09, VAL-10, VAL-11 | ADMIN, ANÓNIMO, STAFF, USER_A | 4 | 11 |
| EVT-02 | `GET /api/events/{eventCode}` | EVT-02a, EVT-02b, EVT-02c, EVT-02d, EVT-02e, EVT-02f, EVT-02g, EVT-02h, TKT-02h | ADMIN, ANÓNIMO, STAFF, USER_A | 4 | 5 |
| EVT-03 | `GET /api/events/published` | EVT-03a, EVT-03b | ANÓNIMO | 2 | 0 |
| EVT-04 | `PATCH /api/events/{eventCode}/publish` | EVT-04a, EVT-04b, EVT-04c, EVT-04d, EVT-04e, EVT-04f, EVT-04g, EVT-04h | ADMIN, ANÓNIMO, STAFF, USER_A | 3 | 5 |
| EVT-05 | `POST /api/events/{eventCode}/artists/{artistId}` | EVT-05a, EVT-05b, EVT-05c, EVT-05d, EVT-05e, EVT-05f, EVT-05g, EVT-05h | ADMIN, ANÓNIMO, STAFF, USER_A | 2 | 6 |
| EVT-06 | `GET /api/events/by-artist?stageName=` | EVT-06a, EVT-06b, EVT-06c, EVT-06d, EVT-06e, EVT-06f | ANÓNIMO, STAFF | 5 | 1 |
| ART-01 | `GET /api/artists/{id}` | ART-01a, ART-01b, VAL-12 | ANÓNIMO | 1 | 2 |
| ART-02 | `GET /api/artists/by-stage-name?stageName=` | ART-02a, ART-02b | ANÓNIMO | 1 | 1 |
| ART-03 | `GET /api/artists/active` | PRE-01, ART-03a | ANÓNIMO | 2 | 0 |
| USR-01 | `POST /api/users` | USR-01a, USR-01b, USR-01c, USR-01d, USR-01e, SEC-09, VAL-01, VAL-02, VAL-03, VAL-04, VAL-05, VAL-06, VAL-07 | ANÓNIMO | 6 | 7 |
| USR-02 | `GET /api/users/by-email?email=` | USR-02a, USR-02b, USR-02c, USR-02d, USR-02e, USR-02f, USR-02g | ADMIN, ANÓNIMO, STAFF, USER_A, USER_B | 2 | 5 |
| USR-03 | `GET /api/users/by-username?username=` | USR-03a, USR-03b, USR-03c, USR-03d, USR-03e | ADMIN, ANÓNIMO, USER_A, USER_B | 2 | 3 |
| TKT-01 | `POST /api/tickets` | TKT-01a, TKT-01b, TKT-01c, TKT-01d, TKT-01e, TKT-01f, TKT-01g, TKT-01h, TKT-01i, TKT-01j, TKT-01k, TKT-01l, TKT-01m, TKT-01n | ANÓNIMO, USER_A, USER_B | 5 | 9 |
| TKT-02 | `GET /api/tickets/{ticketCode}` | TKT-02a, TKT-02b, TKT-02c, TKT-02d, TKT-02e, TKT-02f, TKT-02g, TKT-02i, TKT-02j, TKT-02k, TKT-02l | ANÓNIMO, STAFF, USER_A, USER_B | 6 | 5 |
| TKT-03 | `GET /api/tickets/by-user?email=` | TKT-03a, TKT-03b, TKT-03c, TKT-03d, TKT-03e, TKT-03f, TKT-03g, TKT-03h | ADMIN, ANÓNIMO, STAFF, USER_A, USER_B | 4 | 4 |
| TKT-04 | `GET /api/events/{eventCode}/tickets/paid` | TKT-04a, TKT-04b, TKT-04c, TKT-04d, TKT-04e, SEC-12 | ADMIN, ANÓNIMO, STAFF, USER_A | 3 | 3 |
| TKT-05 | `PATCH /api/tickets/{ticketCode}/cancel` | TKT-05a, TKT-05b, TKT-05c, TKT-05d, TKT-05e, TKT-05f, TKT-05g, TKT-05h | ADMIN, ANÓNIMO, STAFF, USER_A, USER_B | 2 | 6 |
| TKT-06 | `PATCH /api/tickets/{ticketCode}/use` | TKT-06a, TKT-06b, TKT-06c, TKT-06d, TKT-06e, TKT-06f, TKT-06g | ANÓNIMO, STAFF, USER_A, USER_B | 1 | 6 |
| AUTH-01 | `POST /api/auth/login` | AUTH-01a, AUTH-01b, AUTH-01c, AUTH-01d, AUTH-01e, AUTH-01f, AUTH-01g, AUTH-01h, AUTH-01i, AUTH-01j, SEC-10 | ANÓNIMO | 7 | 4 |
| AUTH-02 | `GET /api/auth/me` | AUTH-02a, AUTH-02b, AUTH-02c, AUTH-02d, SEC-01, SEC-02, SEC-03, SEC-04, SEC-05, SEC-06, SEC-08, SEC-11 | ADMIN, ANÓNIMO, Bearer inválido, INACTIVE, JWT alterado, JWT expirado, JWT otra clave, STAFF, USER inyectado, USER_A, USER_B | 5 | 7 |

Total: **22/22** rutas con al menos un caso exitoso y uno de denegación/error (ver columnas).

## 3. Trazabilidad con los casos TC-* del PRD

| Caso PRD | Solicitudes | Nota |
|---|---|---|
| TC-AUTH-001 | USR-01a |  |
| TC-AUTH-002 | AUTH-01a |  |
| TC-AUTH-003 | AUTH-01b |  |
| TC-AUTH-004 | AUTH-01h |  |
| TC-AUTH-005 | AUTH-01i |  |
| TC-AUTH-006 | AUTH-02a |  |
| TC-AUTH-007 | SEC-01 |  |
| TC-AUTH-008 | SEC-02 |  |
| TC-AUTH-009 | SEC-03 |  |
| TC-AUTH-010 | SEC-04 | Requiere token vencido; si no se puede generar → BLOCKED. |
| TC-AUTH-011 | SEC-05 | Requiere token firmado con clave de test; si no se puede → BLOCKED. |
| TC-AUTH-012 | SEC-06 |  |
| TC-AUTH-013 | AUTH-01g, SEC-08 |  |
| TC-AUTH-014 | — | Cubierto por EVT-01f / EVT-04e / EVT-05d (USER contra endpoint ADMIN → 403). |
| TC-AUTH-015 | SEC-07 |  |
| TC-SEC-001 | EVT-01e, EVT-01f |  |
| TC-SEC-002 | EVT-04e, EVT-04f |  |
| TC-SEC-003 | EVT-05d |  |
| TC-SEC-004 | EVT-02c |  |
| TC-SEC-005 | EVT-06a |  |
| TC-SEC-006 | USR-02b |  |
| TC-SEC-007 | USR-03b |  |
| TC-SEC-008 | TKT-01f |  |
| TC-SEC-009 | TKT-02b |  |
| TC-SEC-010 | TKT-03b |  |
| TC-SEC-011 | TKT-05a, TKT-02j |  |
| TC-SEC-012 | TKT-06a |  |
| TC-SEC-013 | TKT-04c |  |
| TC-SEC-014 | TKT-06e |  |
| TC-SEC-015 | EVT-02a |  |
| TC-SEC-016 | SEC-09, SEC-11 |  |
| TC-SEC-017 | SEC-12 | Opcional: usa revocar-staff-e2e.sql. |
| TC-BIZ-001 | VEN-01a, VEN-02a |  |
| TC-BIZ-002 | ART-01a, ART-02a, ART-03a |  |
| TC-BIZ-003 | EVT-01a, EVT-01b |  |
| TC-BIZ-004 | EVT-04a, EVT-03b |  |
| TC-BIZ-005 | EVT-05a, EVT-05b, EVT-06c |  |
| TC-BIZ-006 | USR-01a, USR-01b, USR-02a, VAL-05, VAL-06 |  |
| TC-BIZ-007 | TKT-01a |  |
| TC-BIZ-008 | TKT-02a, TKT-03a |  |
| TC-BIZ-009 | TKT-04a, TKT-04e |  |
| TC-BIZ-010 | TKT-05d, TKT-02k |  |
| TC-BIZ-011 | TKT-05e |  |
| TC-BIZ-012 | TKT-06e, TKT-02l |  |
| TC-BIZ-013 | TKT-06f |  |
| TC-BIZ-014 | TKT-01g, TKT-01i, TKT-01n |  |
| TC-BIZ-015 | EVT-01i, TKT-01h, VAL-11 |  |
