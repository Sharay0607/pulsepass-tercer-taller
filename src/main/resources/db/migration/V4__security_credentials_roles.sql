ALTER TABLE users ADD COLUMN password_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Asegurar valor por defecto para usuarios existentes
UPDATE users SET role = 'USER' WHERE role IS NULL;