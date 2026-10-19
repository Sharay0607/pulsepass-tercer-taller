-- =====================================================================
-- seed-e2e.sql  —  EXCLUSIVO para la BD de pruebas E2E (pulsepass_e2e)
-- Es idempotente: se ejecuta DOS veces (ver tests/e2e/README.md):
--   1) ANTES de registrar usuarios por HTTP  → crea los venues del fixture.
--   2) DESPUÉS de registrar usuarios por HTTP → promueve STAFF/ADMIN y desactiva al inactivo.
-- Los roles privilegiados NUNCA se asignan por HTTP (NFR-SEC009).
-- Uso:  psql -U postgres -d pulsepass_e2e -f tests/e2e/fixtures/seed-e2e.sql
-- =====================================================================
DO $$
BEGIN
    IF current_database() <> 'pulsepass_e2e' THEN
        RAISE EXCEPTION 'Este script SOLO se ejecuta en la BD pulsepass_e2e (BD actual: %)', current_database();
    END IF;
END $$;

-- ---- PARTE 1: venues (V2 solo siembra artistas; no existen venues por defecto) ----
INSERT INTO venues (code, name, city, address, capacity, active) VALUES
    ('E2E-VEN-01',    'E2E Arena Principal', 'Bogota', 'Calle 1 # 2-3', 500, TRUE),
    ('E2E-VEN-SMALL', 'E2E Sala Pequena',    'Bogota', 'Calle 4 # 5-6',   1, TRUE),
    ('E2E-VEN-OFF',   'E2E Sede Inactiva',   'Bogota', 'Calle 7 # 8-9', 100, FALSE)
ON CONFLICT (code) DO NOTHING;

-- ---- PARTE 2: roles y estado (no hace nada si los usuarios aún no están registrados) ----
INSERT INTO user_roles (user_id, role)
SELECT id, 'STAFF' FROM users WHERE lower(username) = 'e2e_staff'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM users WHERE lower(username) = 'e2e_admin'
ON CONFLICT DO NOTHING;

UPDATE users SET active = FALSE WHERE lower(username) = 'e2e_inactive';

-- ---- Verificación ----
SELECT code, capacity, active FROM venues WHERE code LIKE 'E2E-%' ORDER BY code;
SELECT u.username, u.active, string_agg(r.role, ',' ORDER BY r.role) AS roles
  FROM users u LEFT JOIN user_roles r ON r.user_id = u.id
 WHERE u.username LIKE 'e2e\_%' GROUP BY u.username, u.active ORDER BY u.username;
