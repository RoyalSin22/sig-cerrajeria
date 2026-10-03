-- Secuencia dedicada para numero_orden, ya que la columna es NOT NULL
-- y no podemos depender del id autogenerado (se conoce recien tras el INSERT).
CREATE SEQUENCE numero_orden_seq START WITH 1 INCREMENT BY 1;
