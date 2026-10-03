-- Vincula un telefono de WhatsApp a cada usuario (opcional). Es la forma en
-- que el sistema sabe QUIEN esta escribiendo desde WhatsApp, de modo que
-- las reglas de negocio 1, 2 y 3 (caja del usuario, Kardex, auditoria) se
-- apliquen exactamente igual que cuando se opera desde la app.
ALTER TABLE usuarios
  ADD COLUMN telefono_whatsapp VARCHAR(20) UNIQUE;
