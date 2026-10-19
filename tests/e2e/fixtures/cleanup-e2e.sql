-- =====================================================================
-- cleanup-e2e.sql — deja la BD E2E lista para repetir la corrida.
-- Borra SOLO datos de prueba (eventos E2E-*, usuarios @example.test y sus tickets/perfiles/credenciales/roles).
-- NO borra venues ni artistas. Protegido: aborta si la BD no es pulsepass_e2e.
-- =====================================================================
DO $$
BEGIN
    IF current_database() <> 'pulsepass_e2e' THEN
        RAISE EXCEPTION 'Este script SOLO se ejecuta en la BD pulsepass_e2e (BD actual: %)', current_database();
    END IF;
END $$;

DELETE FROM tickets
 WHERE user_id  IN (SELECT id FROM users  WHERE email LIKE '%@example.test')
    OR event_id IN (SELECT id FROM events WHERE event_code LIKE 'E2E-%');

DELETE FROM event_artists
 WHERE event_id IN (SELECT id FROM events WHERE event_code LIKE 'E2E-%');

DELETE FROM events WHERE event_code LIKE 'E2E-%';

DELETE FROM user_profiles
 WHERE user_id IN (SELECT id FROM users WHERE email LIKE '%@example.test');

-- user_credentials y user_roles se eliminan en cascada (V4)
DELETE FROM users WHERE email LIKE '%@example.test';

SELECT (SELECT count(*) FROM users   WHERE email LIKE '%@example.test') AS usuarios_e2e_restantes,
       (SELECT count(*) FROM events  WHERE event_code LIKE 'E2E-%')      AS eventos_e2e_restantes;
