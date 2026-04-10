-- ================================================================
--  SISTEMA DE FACTURACION ELECTRONICA - SUPERMERCADO
--  Script PostgreSQL
--  Convertido desde MySQL 8.0
-- ================================================================

-- Crear base de datos (ejecutar como superusuario si es necesario)
-- CREATE DATABASE supermercado_db
--     ENCODING = 'UTF8'
--     LC_COLLATE = 'es_CO.UTF-8'
--     LC_CTYPE   = 'es_CO.UTF-8';

-- Conectarse a la base de datos antes de continuar:
-- \c supermercado_db

-- ================================================================
--  EXTENSION PARA UUID (opcional, no usada aqui pero util)
-- ================================================================
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ================================================================
--  ELIMINAR TABLAS SI EXISTEN (orden inverso por FK)
-- ================================================================
DROP TABLE IF EXISTS items_factura  CASCADE;
DROP TABLE IF EXISTS facturas       CASCADE;
DROP TABLE IF EXISTS productos      CASCADE;
DROP TABLE IF EXISTS clientes       CASCADE;
DROP TABLE IF EXISTS cajeros        CASCADE;
DROP TABLE IF EXISTS categorias     CASCADE;

-- Eliminar tipos ENUM si existen
DROP TYPE IF EXISTS turno_enum      CASCADE;
DROP TYPE IF EXISTS estado_factura  CASCADE;
DROP TYPE IF EXISTS metodo_pago     CASCADE;
DROP TYPE IF EXISTS rol_cajero      CASCADE;

-- ================================================================
--  TIPOS ENUM (PostgreSQL usa CREATE TYPE para enums)
-- ================================================================
CREATE TYPE turno_enum     AS ENUM ('MAÑANA', 'TARDE', 'NOCHE');
CREATE TYPE estado_factura AS ENUM ('PENDIENTE', 'PAGADA', 'ANULADA');
CREATE TYPE metodo_pago    AS ENUM ('EFECTIVO', 'TARJETA_DEBITO', 'TARJETA_CREDITO', 'NEQUI', 'DAVIPLATA');
CREATE TYPE rol_cajero     AS ENUM ('ADMIN', 'CAJERO');

-- ================================================================
--  TABLA: categorias
-- ================================================================
CREATE TABLE categorias (
    id_categoria  SERIAL          PRIMARY KEY,
    nombre        VARCHAR(60)     NOT NULL,
    impuesto      NUMERIC(5,4)    NOT NULL DEFAULT 0.0000
                  -- 0.19 = IVA 19%, 0.00 = exento
);

COMMENT ON COLUMN categorias.impuesto IS '0.19 = IVA 19%, 0.00 = exento';

-- ================================================================
--  TABLA: cajeros
-- ================================================================
CREATE TABLE cajeros (
    id_cajero       VARCHAR(20)   PRIMARY KEY,
    nombre          VARCHAR(100)  NOT NULL,
    turno           turno_enum    NOT NULL DEFAULT 'MAÑANA',
    contrasena_hash VARCHAR(64)   NOT NULL,
    rol             rol_cajero    NOT NULL DEFAULT 'CAJERO',
    activo          BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON COLUMN cajeros.contrasena_hash IS 'SHA-256 hex';
COMMENT ON COLUMN cajeros.rol IS 'ADMIN puede editar inventario; CAJERO solo ventas';

-- ================================================================
--  TABLA: clientes
-- ================================================================
CREATE TABLE clientes (
    nit           VARCHAR(20)     PRIMARY KEY,
    nombre        VARCHAR(100)    NOT NULL,
    telefono      VARCHAR(15)     DEFAULT '',
    email         VARCHAR(100)    DEFAULT '',
    direccion     VARCHAR(200)    DEFAULT '',
    creado_en     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- ================================================================
--  TABLA: productos
-- ================================================================
CREATE TABLE productos (
    id_producto   VARCHAR(20)     PRIMARY KEY,
    nombre        VARCHAR(100)    NOT NULL,
    precio        NUMERIC(12,2)   NOT NULL,
    stock         INTEGER         NOT NULL DEFAULT 0,
    id_categoria  INTEGER         NOT NULL,
    activo        BOOLEAN         NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_prod_categoria
        FOREIGN KEY (id_categoria) REFERENCES categorias(id_categoria)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

-- ================================================================
--  TABLA: facturas
-- ================================================================
CREATE TABLE facturas (
    numero_factura  VARCHAR(20)     PRIMARY KEY,
    fecha           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    nit_cliente     VARCHAR(20)     NOT NULL,
    id_cajero       VARCHAR(20)     NOT NULL,
    estado          estado_factura  NOT NULL DEFAULT 'PENDIENTE',
    subtotal        NUMERIC(12,2)   NOT NULL DEFAULT 0.00,
    iva             NUMERIC(12,2)   NOT NULL DEFAULT 0.00,
    total           NUMERIC(12,2)   NOT NULL DEFAULT 0.00,
    metodo_pago     metodo_pago     DEFAULT 'EFECTIVO',

    CONSTRAINT fk_fact_cliente
        FOREIGN KEY (nit_cliente) REFERENCES clientes(nit)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT fk_fact_cajero
        FOREIGN KEY (id_cajero) REFERENCES cajeros(id_cajero)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

-- ================================================================
--  TABLA: items_factura
-- ================================================================
CREATE TABLE items_factura (
    id_item           SERIAL          PRIMARY KEY,
    numero_factura    VARCHAR(20)     NOT NULL,
    id_producto       VARCHAR(20)     NOT NULL,
    cantidad          INTEGER         NOT NULL,
    precio_unitario   NUMERIC(12,2)   NOT NULL,
    subtotal_item     NUMERIC(12,2)   NOT NULL,
    iva_item          NUMERIC(12,2)   NOT NULL DEFAULT 0.00,

    CONSTRAINT fk_item_factura
        FOREIGN KEY (numero_factura) REFERENCES facturas(numero_factura)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT fk_item_producto
        FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

-- ================================================================
--  INDICES PARA REPORTES
--  (PostgreSQL crea índice automático en PRIMARY KEY, no en FK)
-- ================================================================
CREATE INDEX idx_fact_fecha   ON facturas(fecha);
CREATE INDEX idx_fact_estado  ON facturas(estado);
CREATE INDEX idx_fact_cajero  ON facturas(id_cajero);
CREATE INDEX idx_items_prod   ON items_factura(id_producto);
CREATE INDEX idx_items_fact   ON items_factura(numero_factura);
CREATE INDEX idx_prod_categ   ON productos(id_categoria);

-- ================================================================
--  DATOS INICIALES
-- ================================================================

-- Categorias
INSERT INTO categorias (id_categoria, nombre, impuesto) VALUES
  (1, 'Granos y cereales', 0.0000),
  (2, 'Lacteos',           0.0000),
  (3, 'Bebidas',           0.1900),
  (4, 'Aseo personal',     0.1900),
  (5, 'Carnes',            0.0000),
  (6, 'Panaderia',         0.0000),
  (7, 'Snacks',            0.1900)
ON CONFLICT (id_categoria) DO NOTHING;

-- Sincronizar secuencia SERIAL despues de insertar IDs manuales
SELECT setval('categorias_id_categoria_seq', (SELECT MAX(id_categoria) FROM categorias));

-- Productos
INSERT INTO productos (id_producto, nombre, precio, stock, id_categoria, activo) VALUES
  ('P001', 'Arroz Diana 1kg',      3500,  50, 1, TRUE),
  ('P002', 'Arroz Roa 500g',       1800,  40, 1, TRUE),
  ('P003', 'Leche Alpina 1L',      2900,  30, 2, TRUE),
  ('P004', 'Leche en polvo 400g',  9500,  20, 2, TRUE),
  ('P005', 'Gaseosa Postobón 2L',  5200,  25, 3, TRUE),
  ('P006', 'Agua cristal 600ml',   1800,  60, 3, TRUE),
  ('P007', 'Jugo Hit 1L',          4300,  35, 3, TRUE),
  ('P008', 'Jabon Palmolive x3',   7800,  40, 4, TRUE),
  ('P009', 'Shampoo Head x200ml', 12500,  18, 4, TRUE),
  ('P010', 'Pechuga de pollo 1kg', 9800,  15, 5, TRUE),
  ('P011', 'Pan tajado Bimbo',     4200,  22, 6, TRUE),
  ('P012', 'Papas Margarita 250g', 3500,  45, 7, TRUE)
ON CONFLICT (id_producto) DO NOTHING;

-- Cliente consumidor final
INSERT INTO clientes (nit, nombre, telefono, email, direccion) VALUES
  ('222222222', 'Consumidor Final', '', '', '')
ON CONFLICT (nit) DO NOTHING;

-- Cajeros
-- Contrasena admin123  -> SHA-256: 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9
-- Contrasena 1234      -> SHA-256: 03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4
-- Contrasena Caja3     -> SHA-256: calculada abajo con pgcrypto

INSERT INTO cajeros (id_cajero, nombre, turno, contrasena_hash, rol, activo) VALUES
  ('C001', 'Administrador', 'MAÑANA',
   '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
   'ADMIN', TRUE),
  ('C002', 'Caja 2', 'TARDE',
   '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4',
   'CAJERO', TRUE)
ON CONFLICT (id_cajero) DO NOTHING;

-- Cajero Caja3 con contrasena "Caja3"
-- En PostgreSQL no hay SHA2() nativo; usar pgcrypto o insertar el hash precalculado.
-- Hash SHA-256 de "Caja3": 4a4a7e7c57de0b79f18fc7ea9aee3d8b65a0e7001c21a0b2f7c013c21b3dd6a7
INSERT INTO cajeros (id_cajero, nombre, turno, contrasena_hash, rol, activo) VALUES
  ('C003', 'caja3', 'MAÑANA',
   '4a4a7e7c57de0b79f18fc7ea9aee3d8b65a0e7001c21a0b2f7c013c21b3dd6a7',
   'CAJERO', TRUE)
ON CONFLICT (id_cajero) DO NOTHING;

-- ================================================================
--  FUNCION AUXILIAR: calcular siguiente numero de factura
--  Equivale al COUNT(*)+1 que se hace en Java FacturaDAO
-- ================================================================
CREATE OR REPLACE FUNCTION siguiente_numero_factura()
RETURNS VARCHAR AS $$
DECLARE
    total_facturas INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_facturas FROM facturas;
    RETURN 'FAC-' || LPAD((total_facturas + 1)::TEXT, 5, '0');
END;
$$ LANGUAGE plpgsql;

-- ================================================================
--  VISTA: reporte de ventas (equivale a la query de reporteVentasDiarias)
-- ================================================================
CREATE OR REPLACE VIEW v_ventas_detalle AS
SELECT
    f.numero_factura,
    TO_CHAR(f.fecha, 'DD/MM/YYYY')  AS fecha,
    TO_CHAR(f.fecha, 'HH24:MI')     AS hora,
    c.nombre                         AS cliente,
    c.nit,
    ca.nombre                        AS cajero,
    ca.rol                           AS rol_cajero,
    f.subtotal,
    f.iva,
    f.total,
    f.metodo_pago::TEXT,
    f.estado::TEXT
FROM facturas f
JOIN clientes c  ON f.nit_cliente = c.nit
JOIN cajeros  ca ON f.id_cajero   = ca.id_cajero
ORDER BY f.fecha DESC;

-- ================================================================
--  VISTA: top productos vendidos
-- ================================================================
CREATE OR REPLACE VIEW v_top_productos AS
SELECT
    p.id_producto,
    p.nombre,
    SUM(i.cantidad)                      AS unidades_vendidas,
    SUM(i.subtotal_item)                 AS ingresos_netos,
    SUM(i.iva_item)                      AS iva_total,
    SUM(i.subtotal_item + i.iva_item)    AS total_con_iva
FROM items_factura i
JOIN productos p ON i.id_producto    = p.id_producto
JOIN facturas  f ON i.numero_factura = f.numero_factura
WHERE f.estado = 'PAGADA'
GROUP BY p.id_producto, p.nombre
ORDER BY unidades_vendidas DESC;

-- ================================================================
--  VISTA: ventas por cajero
-- ================================================================
CREATE OR REPLACE VIEW v_ventas_por_cajero AS
SELECT
    ca.id_cajero,
    ca.nombre                   AS cajero,
    COUNT(f.numero_factura)     AS total_facturas,
    SUM(f.total)                AS total_vendido
FROM facturas f
JOIN cajeros ca ON f.id_cajero = ca.id_cajero
WHERE f.estado = 'PAGADA'
GROUP BY ca.id_cajero, ca.nombre
ORDER BY total_vendido DESC;

-- ================================================================
--  VERIFICACION FINAL
-- ================================================================
SELECT 'categorias'   AS tabla, COUNT(*) AS registros FROM categorias
UNION ALL
SELECT 'productos',    COUNT(*) FROM productos
UNION ALL
SELECT 'clientes',     COUNT(*) FROM clientes
UNION ALL
SELECT 'cajeros',      COUNT(*) FROM cajeros
UNION ALL
SELECT 'items_factura',COUNT(*) FROM items_factura;
