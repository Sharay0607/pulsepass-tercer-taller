-- =====================================================================
-- V4: credenciales y roles (Fase 4 - Seguridad)
-- >>> ANTES DE COPIAR <<<  revisa que V4 sea realmente la SIGUIENTE versión libre:
--     ls src/main/resources/db/migration     (si ya existe V4, renombra a V5, V6...)
--     NUNCA edites una migración que ya se ejecutó: Flyway detecta el cambio y falla.
-- >>> ADAPTAR <<< Se asume users(id BIGINT). Si tu id es UUID, cambia BIGINT por UUID aquí
--     y Long por UUID en el código Java (ver GUIA, sección "Si tu ID es UUID").
-- =====================================================================

-- Credencial 1:1 con users. El hash vive aparte para que nunca se serialice por accidente.
CREATE TABLE user_credentials (
    user_id             BIGINT       PRIMARY KEY
                        REFERENCES users (id) ON DELETE CASCADE,
    password_hash       VARCHAR(100) NOT NULL,
    password_changed_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- Defensa en profundidad: la BD rechaza hashes que no parezcan BCrypt ($2a$, $2b$, $2y$).
    CONSTRAINT ck_user_credentials_bcrypt CHECK (password_hash LIKE '$2%')
);

-- Roles N:1 con users. PK compuesta => un usuario no puede tener el mismo rol dos veces.
CREATE TABLE user_roles (
    user_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role    VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT ck_user_roles_role CHECK (role IN ('USER', 'STAFF', 'ADMIN'))
);

-- ---------------------------------------------------------------------
-- Usuarios LEGADOS (creados en fases 1-3 sin contraseña):
--  * Reciben el rol USER para que, cuando se les active la cuenta de forma segura, ya lo tengan.
--  * NO reciben credencial: NO pueden iniciar sesión. Jamás se inventa una contraseña por defecto.
-- ---------------------------------------------------------------------
INSERT INTO user_roles (user_id, role)
SELECT id, 'USER' FROM users
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------
-- Estado activo: si tu tabla users YA tiene una columna de estado, NO ejecutes esto
-- y ajusta USER_ACTIVE en SecurityQueryRepository. Si NO la tiene, descomenta:
-- ---------------------------------------------------------------------
-- ALTER TABLE users ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;
