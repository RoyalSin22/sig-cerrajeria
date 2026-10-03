-- Usuario administrador inicial para pruebas locales.
-- Usuario: admin | Contraseña: admin123 (CAMBIAR antes de cualquier uso real)
INSERT INTO usuarios (username, password, nombre, role, activo)
VALUES (
  'admin',
  '$2b$10$gykHQtNMOIkmj.6ujcHmmu9fs46QxUdRR1hbLagcfoP7m9YXcw6u6',
  'Administrador',
  'ROLE_ADMIN',
  true
);
