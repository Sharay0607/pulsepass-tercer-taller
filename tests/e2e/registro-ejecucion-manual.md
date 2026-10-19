# Registro de ejecución manual — PulsePass E2E

| Campo | Valor |
|---|---|
| Ejecutor | |
| Fecha / hora | |
| Commit / rama probada | `feature/security-fase4` @ ____ |
| BD usada | `pulsepass_e2e` |
| Versión de Postman | |
| Run ID / notas | |

**Estados:** `PASS` (HTTP y contenido correctos) · `FAIL` (difiere de lo esperado) · `BLOCKED` (un prerrequisito falló) · `N/A` (paso opcional no ejecutado).
Si un prerrequisito falla, los dependientes van `BLOCKED`, nunca `PASS`. **No pegues tokens ni contraseñas** en Observación.

## 00 Preparación / conectividad

| Paso | ID | Actor | HTTP esperado | HTTP observado | Estado | Observación / evidencia |
|---|---|---|---|---|---|---|
| 001 | `E2E-PRE-01` | ANÓNIMO | 200 |      200      |PASS    |Lista con 5 artistas (Solar Beat, Neon Waves, ...). |

## 01 Venues (2 rutas)

| Paso | ID | Actor | HTTP esperado | HTTP observado | Estado | Observación / evidencia |
|---|---|---|---|---|---|---|
| 002 | `E2E-VEN-01a` | ANÓNIMO | 200 |      200      |PASS    |code=E2E-VEN-01, capacity=500, active=true.| |
| 003 | `E2E-VEN-01b` | ANÓNIMO | 404 |      404      |PASS    |404 con ErrorResponse completo (5 campos), sin trazas.| | |
| 004 | `E2E-VEN-02a` | ANÓNIMO | 200 |      200      |PASS    |Incluye E2E-VEN-01 y E2E-VEN-SMALL; no aparece E2E-VEN-OFF.| | |

## 02 Artists (3 rutas)

| Paso | ID | Actor | HTTP esperado | HTTP observado | Estado | Observación / evidencia |
|---|---|---|---|---|---|---|
| 005 | `E2E-ART-01a` | ANÓNIMO | 200 |      200      |PASS    |id=1, stageName=Solar Beat.| | |
| 006 | `E2E-ART-01b` | ANÓNIMO | 404 |      404      |PASS    || | |
| 007 | `E2E-ART-02a` | ANÓNIMO | 200 |      200      |PASS    || | |
| 008 | `E2E-ART-02b` | ANÓNIMO | 404 |      404      |PASS    || | |
| 009 | `E2E-ART-03a` | ANÓNIMO | 200 |      200      |PASS    || | |

## 03 Auth / usuarios (registro, login, me)

| Paso | ID | Actor | HTTP esperado | HTTP observado | Estado | Observación / evidencia |
|---|---|---|---|---|---|---|
| 010 | `E2E-USR-01a` | ANÓNIMO | 201 | 201 |PASS| |
| 011 | `E2E-USR-01b` | ANÓNIMO | 201 | 201 |PASS | |
| 012 | `E2E-USR-01c` | ANÓNIMO | 201 | 201|PASS | |
| 013 | `E2E-USR-01d` | ANÓNIMO | 201 | 201 |PASS | |
| 014 | `E2E-USR-01e` | ANÓNIMO | 201 | 201|PASS | |
| 015 | `E2E-AUTH-01a` | ANÓNIMO | 200 |200|PASS | |
| 016 | `E2E-AUTH-01b` | ANÓNIMO | 200 |200|PASS | |
| 017 | `E2E-AUTH-01c` | ANÓNIMO | 200 |200|PASS | |
| 018 | `E2E-AUTH-01d` | ANÓNIMO | 200 |200 |PASS | |
| — | **MANUAL** | — | — | — | ☐ hecho | PAUSA SQL: ejecutar fixtures/seed-e2e.sql PARTE 2 (promueve e2e_staff→STAFF, e2e_admin→ADMIN y desactiva e2e_inactive). |
| 019 | `E2E-AUTH-01e` | ANÓNIMO | 200 |200| PASS| |
| 020 | `E2E-AUTH-01f` | ANÓNIMO | 200 |200 |PASS | |
| 021 | `E2E-AUTH-01g` | ANÓNIMO | 401 |401|PASS | |
| 022 | `E2E-AUTH-01h` | ANÓNIMO | 401 |401 |PASS | |
| 023 | `E2E-AUTH-01i` | ANÓNIMO | 401 |401 |PASS | |
| 024 | `E2E-AUTH-01j` | ANÓNIMO | 400 |400 |PASS | |
| 025 | `E2E-AUTH-02a` | USER_A | 200 |200 |PASS | |
| 026 | `E2E-AUTH-02b` | USER_B | 200 |200 |PASS | |
| 027 | `E2E-AUTH-02c` | STAFF | 200 |200|PASS | |
| 028 | `E2E-AUTH-02d` | ADMIN | 200 |200 |PASS | |

## 04 Usuarios / consulta y propiedad (2 rutas)

| Paso | ID | Actor | HTTP esperado | HTTP observado | Estado | Observación / evidencia |
|---|---|---|---|---|---|---|
| 029 | `E2E-USR-02a` | USER_A | 200 | | | |
| 030 | `E2E-USR-02b` | USER_B | 403 | | | |
| 031 | `E2E-USR-02c` | ANÓNIMO | 401 | | | |
| 032 | `E2E-USR-02d` | STAFF | 403 | | | |
| 033 | `E2E-USR-02e` | ADMIN | 200 | | | |
| 034 | `E2E-USR-02f` | ADMIN | 404 | | | |
| 035 | `E2E-USR-02g` | USER_A | 403 | | | |
| 036 | `E2E-USR-03a` | USER_A | 200 | | | |
| 037 | `E2E-USR-03b` | USER_B | 403 | | | |
| 038 | `E2E-USR-03c` | ANÓNIMO | 401 | | | |
| 039 | `E2E-USR-03d` | ADMIN | 200 | | | |
| 040 | `E2E-USR-03e` | ADMIN | 404 | | | |

## 05 Events / administración y visibilidad (6 rutas)

| Paso | ID | Actor | HTTP esperado | HTTP observado | Estado | Observación / evidencia |
|---|---|---|---|---|---|---|
| 041 | `E2E-EVT-01a` | ADMIN | 201 | | | |
| 042 | `E2E-EVT-01b` | ADMIN | 201 | | | |
| 043 | `E2E-EVT-01c` | ADMIN | 201 | | | |
| 044 | `E2E-EVT-01d` | ADMIN | 201 | | | |
| 045 | `E2E-EVT-02a` | ADMIN | 200 | | | |
| 046 | `E2E-EVT-02b` | STAFF | 200 | | | |
| 047 | `E2E-EVT-02c` | ANÓNIMO | 404 | | | |
| 048 | `E2E-EVT-02d` | USER_A | 404 | | | |
| 049 | `E2E-EVT-03a` | ANÓNIMO | 200 | | | |
| 050 | `E2E-EVT-05a` | ADMIN | 200 | | | |
| 051 | `E2E-EVT-05b` | ADMIN | 200 | | | |
| 052 | `E2E-EVT-05c` | ADMIN | 409 | | | |
| 053 | `E2E-EVT-05d` | USER_A | 403 | | | |
| 054 | `E2E-EVT-05e` | ANÓNIMO | 401 | | | |
| 055 | `E2E-EVT-05f` | STAFF | 403 | | | |
| 056 | `E2E-EVT-05g` | ADMIN | 404 | | | |
| 057 | `E2E-EVT-05h` | ADMIN | 404 | | | |
| 058 | `E2E-EVT-06a` | ANÓNIMO | 200 | | | |
| 059 | `E2E-EVT-06b` | STAFF | 200 | | | |
| 060 | `E2E-EVT-04a` | ADMIN | 200 | | | |
| 061 | `E2E-EVT-04b` | ADMIN | 200 | | | |
| 062 | `E2E-EVT-04c` | ADMIN | 200 | | | |
| 063 | `E2E-EVT-04d` | ADMIN | 409 | | | |
| 064 | `E2E-EVT-04e` | USER_A | 403 | | | |
| 065 | `E2E-EVT-04f` | STAFF | 403 | | | |
| 066 | `E2E-EVT-04g` | ANÓNIMO | 401 | | | |
| 067 | `E2E-EVT-04h` | ADMIN | 404 | | | |
| 068 | `E2E-EVT-02e` | ANÓNIMO | 200 | | | |
| 069 | `E2E-EVT-02f` | ANÓNIMO | 404 | | | |
| 070 | `E2E-EVT-02g` | ANÓNIMO | 404 | | | |
| 071 | `E2E-EVT-03b` | ANÓNIMO | 200 | | | |
| 072 | `E2E-EVT-06c` | ANÓNIMO | 200 | | | |
| 073 | `E2E-EVT-06d` | STAFF | 200 | | | |
| 074 | `E2E-EVT-06e` | ANÓNIMO | 400 | | | |
| 075 | `E2E-EVT-06f` | ANÓNIMO | 200 | | | |
| 076 | `E2E-EVT-01e` | ANÓNIMO | 401 | | | |
| 077 | `E2E-EVT-01f` | USER_A | 403 | | | |
| 078 | `E2E-EVT-01g` | STAFF | 403 | | | |
| 079 | `E2E-EVT-01h` | ADMIN | 409 | | | |
| 080 | `E2E-EVT-01i` | ADMIN | 404 | | | |
| 081 | `E2E-EVT-01j` | ADMIN | 409 | | | |
| 082 | `E2E-EVT-01k` | ADMIN | 409 | | | |
| 083 | `E2E-EVT-02h` | ADMIN | 404 | | | |

## 06 Tickets / compra, consulta y ciclo de vida (6 rutas)

| Paso | ID | Actor | HTTP esperado | HTTP observado | Estado | Observación / evidencia |
|---|---|---|---|---|---|---|
| 084 | `E2E-TKT-01a` | USER_A | 201 | | | |
| 085 | `E2E-TKT-01b` | USER_A | 201 | | | |
| 086 | `E2E-TKT-01c` | USER_A | 201 | | | |
| 087 | `E2E-TKT-01d` | USER_B | 201 | | | |
| 088 | `E2E-TKT-02a` | USER_A | 200 | | | |
| 089 | `E2E-TKT-02b` | USER_B | 403 | | | |
| 090 | `E2E-TKT-02c` | ANÓNIMO | 401 | | | |
| 091 | `E2E-TKT-02d` | STAFF | 200 | | | |
| 092 | `E2E-TKT-02e` | USER_A | 403 | | | |
| 093 | `E2E-TKT-02f` | USER_A | 403 | | | |
| 094 | `E2E-TKT-02g` | STAFF | 404 | | | |
| 095 | `E2E-TKT-03a` | USER_A | 200 | | | |
| 096 | `E2E-TKT-03b` | USER_B | 403 | | | |
| 097 | `E2E-TKT-03c` | ANÓNIMO | 401 | | | |
| 098 | `E2E-TKT-03d` | ADMIN | 200 | | | |
| 099 | `E2E-TKT-03e` | STAFF | 403 | | | |
| 100 | `E2E-TKT-03f` | USER_A | 400 | | | |
| 101 | `E2E-TKT-03g` | USER_B | 200 | | | |
| 102 | `E2E-TKT-04a` | STAFF | 200 | | | |
| 103 | `E2E-TKT-04b` | ADMIN | 200 | | | |
| 104 | `E2E-TKT-04c` | USER_A | 403 | | | |
| 105 | `E2E-TKT-04d` | ANÓNIMO | 401 | | | |
| 106 | `E2E-TKT-01e` | ANÓNIMO | 401 | | | |
| 107 | `E2E-TKT-01f` | USER_A | 403 | | | |
| 108 | `E2E-TKT-03h` | USER_B | 200 | | | |
| 109 | `E2E-TKT-01g` | USER_A | 409 | | | |
| 110 | `E2E-TKT-01h` | USER_A | 404 | | | |
| 111 | `E2E-TKT-01i` | USER_A | 409 | | | |
| 112 | `E2E-TKT-01j` | USER_A | 400 | | | |
| 113 | `E2E-TKT-01k` | USER_A | 400 | | | |
| 114 | `E2E-TKT-01l` | USER_A | 400 | | | |
| 115 | `E2E-TKT-01m` | USER_A | 201 | | | |
| 116 | `E2E-TKT-02h` | ANÓNIMO | 200 | | | |
| 117 | `E2E-TKT-01n` | USER_B | 409 | | | |
| 118 | `E2E-TKT-06a` | USER_B | 403 | | | |
| 119 | `E2E-TKT-06b` | USER_A | 403 | | | |
| 120 | `E2E-TKT-06c` | ANÓNIMO | 401 | | | |
| 121 | `E2E-TKT-02i` | USER_A | 200 | | | |
| 122 | `E2E-TKT-05a` | USER_B | 403 | | | |
| 123 | `E2E-TKT-05b` | STAFF | 403 | | | |
| 124 | `E2E-TKT-05c` | ANÓNIMO | 401 | | | |
| 125 | `E2E-TKT-02j` | USER_A | 200 | | | |
| 126 | `E2E-TKT-05d` | USER_A | 200 | | | |
| 127 | `E2E-TKT-05e` | USER_A | 409 | | | |
| 128 | `E2E-TKT-02k` | USER_A | 200 | | | |
| 129 | `E2E-TKT-06d` | STAFF | 409 | | | |
| 130 | `E2E-TKT-06e` | STAFF | 200 | | | |
| 131 | `E2E-TKT-06f` | STAFF | 409 | | | |
| 132 | `E2E-TKT-05f` | USER_A | 409 | | | |
| 133 | `E2E-TKT-02l` | STAFF | 200 | | | |
| 134 | `E2E-TKT-05g` | ADMIN | 200 | | | |
| 135 | `E2E-TKT-05h` | ADMIN | 404 | | | |
| 136 | `E2E-TKT-06g` | STAFF | 404 | | | |
| 137 | `E2E-TKT-04e` | STAFF | 200 | | | |

## 07 Seguridad / escenarios negativos

| Paso | ID | Actor | HTTP esperado | HTTP observado | Estado | Observación / evidencia |
|---|---|---|---|---|---|---|
| 138 | `E2E-SEC-01` | ANÓNIMO | 401 | | | |
| 139 | `E2E-SEC-02` | Bearer inválido | 401 | | | |
| 140 | `E2E-SEC-03` | JWT alterado | 401 | | | |
| 141 | `E2E-SEC-04` | JWT expirado | 401 | | | |
| 142 | `E2E-SEC-05` | JWT otra clave | 401 | | | |
| 143 | `E2E-SEC-06` | ANÓNIMO | 401 | | | |
| 144 | `E2E-SEC-07` | Bearer inválido | 401 | | | |
| 145 | `E2E-SEC-08` | INACTIVE | 401 | | | |
| 146 | `E2E-SEC-09` | ANÓNIMO | 201 | | | |
| 147 | `E2E-SEC-10` | ANÓNIMO | 200 | | | |
| 148 | `E2E-SEC-11` | USER inyectado | 200 | | | |
| — | **MANUAL** | — | — | — | ☐ hecho | OPCIONAL (TC-SEC-017): ejecutar fixtures/revocar-staff-e2e.sql para quitar el rol STAFF a e2e_staff SIN pedir un nuevo login. |
| 149 | `E2E-SEC-12` | STAFF | 403 | | | |
| — | **MANUAL** | — | — | — | ☐ hecho | OPCIONAL: restaurar el rol ejecutando de nuevo fixtures/seed-e2e.sql (parte de roles) y repetir AUTH-01e si hace falta. |

## 08 Validaciones y errores HTTP

| Paso | ID | Actor | HTTP esperado | HTTP observado | Estado | Observación / evidencia |
|---|---|---|---|---|---|---|
| 150 | `E2E-VAL-01` | ANÓNIMO | 400 | | | |
| 151 | `E2E-VAL-02` | ANÓNIMO | 400 | | | |
| 152 | `E2E-VAL-03` | ANÓNIMO | 400 | | | |
| 153 | `E2E-VAL-04` | ANÓNIMO | 409 | | | |
| 154 | `E2E-VAL-05` | ANÓNIMO | 409 | | | |
| 155 | `E2E-VAL-06` | ANÓNIMO | 409 | | | |
| 156 | `E2E-VAL-07` | ANÓNIMO | 400 | | | |
| 157 | `E2E-VAL-08` | ADMIN | 400 | | | |
| 158 | `E2E-VAL-09` | ADMIN | 400 | | | |
| 159 | `E2E-VAL-10` | ADMIN | 400 | | | |
| 160 | `E2E-VAL-11` | ADMIN | 400 | | | |
| 161 | `E2E-VAL-12` | ANÓNIMO | 400 | | | |
| 162 | `E2E-VAL-13` | ADMIN | 404 | | | |
| 163 | `E2E-VAL-14` | ADMIN | 405 | | | |

## Defectos y bloqueos encontrados

| # | ID de caso | Severidad | Descripción (esperado vs observado) | ¿Regresión Fases 1-4? |
|---|---|---|---|---|
| 1 | | | | |

## Resumen

| Total | PASS | FAIL | BLOCKED | N/A |
|---|---|---|---|---|
| 163 | | | | |
