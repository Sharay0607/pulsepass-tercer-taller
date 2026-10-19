# Fixtures E2E

| Archivo | Cuándo | Qué hace |
|---|---|---|
| `seed-e2e.sql` | **Dos veces**: antes y después de registrar usuarios | Crea 3 venues (`E2E-VEN-01` cap. 500, `E2E-VEN-SMALL` cap. 1, `E2E-VEN-OFF` inactivo); promueve `e2e_staff`→STAFF y `e2e_admin`→ADMIN; desactiva `e2e_inactive`. |
| `revocar-staff-e2e.sql` | Opcional (TC-SEC-017) | Quita el rol STAFF sin cambiar el token. |
| `cleanup-e2e.sql` | Para repetir la corrida | Borra eventos `E2E-*` y usuarios `@example.test` con sus tickets. |

Todos abortan si la base de datos no se llama `pulsepass_e2e`. **Nunca** los ejecutes contra `pulsepass`.

Datos que NO necesitan seed porque se crean por HTTP durante la prueba: eventos (los crea ADMIN), usuarios (registro público) y tickets (compra).
Los 5 artistas ya vienen de la migración `V2__insert_initial_artists.sql`.
