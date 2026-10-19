-- OPCIONAL (TC-SEC-017). Solo BD E2E. Quita el rol STAFF a e2e_staff para comprobar que
-- un token emitido ANTES pierde el privilegio en la siguiente petición.
DO $$
BEGIN
    IF current_database() <> 'pulsepass_e2e' THEN
        RAISE EXCEPTION 'Este script SOLO se ejecuta en la BD pulsepass_e2e (BD actual: %)', current_database();
    END IF;
END $$;

DELETE FROM user_roles
 WHERE role = 'STAFF' AND user_id IN (SELECT id FROM users WHERE lower(username) = 'e2e_staff');
-- Restaurar: volver a ejecutar seed-e2e.sql
