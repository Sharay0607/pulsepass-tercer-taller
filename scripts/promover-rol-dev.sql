-- SOLO DESARROLLO / DEMO. NO está en db/migration a propósito (no se ejecuta en producción).
-- Los roles privilegiados NUNCA se asignan por HTTP (NFR-SEC009); se provisionan por proceso controlado.
-- Primero registra la cuenta con POST /api/users y luego ejecútalo manualmente:
--   psql -d pulsepass -f scripts/promover-rol-dev.sql

-- Admin de demostración
INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM users WHERE lower(username) = lower('admin')
ON CONFLICT DO NOTHING;

-- Staff de demostración (Laura)
INSERT INTO user_roles (user_id, role)
SELECT id, 'STAFF' FROM users WHERE lower(username) = lower('laura')
ON CONFLICT DO NOTHING;

-- Verificación
SELECT u.username, string_agg(r.role, ',') AS roles
  FROM users u JOIN user_roles r ON r.user_id = u.id
 GROUP BY u.username ORDER BY u.username;
