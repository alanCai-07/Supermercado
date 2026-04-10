-- ================================================================
--  SCRIPT DE INSTALACION - SUPERMERCADO FACTURACION
--  Ejecutar en MySQL Workbench o desde la terminal:
--  mysql -u root -p < setup.sql
-- ================================================================

CREATE DATABASE IF NOT EXISTS supermercado_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_spanish_ci;

USE supermercado_db;

-- ---- CATEGORIAS ----
CREATE TABLE IF NOT EXISTS categorias (
    id_categoria  INT AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(60)     NOT NULL,
    impuesto      DECIMAL(5,4)    NOT NULL DEFAULT 0.0000
                  COMMENT '0.19 = IVA 19%, 0.00 = exento'
) ENGINE=InnoDB;

-- ---- PRODUCTOS ----
CREATE TABLE IF NOT EXISTS productos (
    id_producto   VARCHAR(20)     PRIMARY KEY,
    nombre        VARCHAR(100)    NOT NULL,
    precio        DECIMAL(12,2)   NOT NULL,
    stock         INT             NOT NULL DEFAULT 0,
    id_categoria  INT             NOT NULL,
    activo        TINYINT(1)      NOT NULL DEFAULT 1,
    FOREIGN KEY (id_categoria) REFERENCES categorias(id_categoria)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---- CLIENTES ----
CREATE TABLE IF NOT EXISTS clientes (
    nit           VARCHAR(20)     PRIMARY KEY,
    nombre        VARCHAR(100)    NOT NULL,
    telefono      VARCHAR(15)     DEFAULT '',
    email         VARCHAR(100)    DEFAULT '',
    direccion     VARCHAR(200)    DEFAULT '',
    creado_en     DATETIME        DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---- CAJEROS ----
CREATE TABLE IF NOT EXISTS cajeros (
    id_cajero       VARCHAR(20)   PRIMARY KEY,
    nombre          VARCHAR(100)  NOT NULL,
    turno           ENUM('MAÑANA','TARDE','NOCHE') NOT NULL DEFAULT 'MAÑANA',
    contrasena_hash VARCHAR(64)   NOT NULL COMMENT 'SHA-256 hex',
    rol             ENUM('ADMIN','CAJERO') NOT NULL DEFAULT 'CAJERO'
                    COMMENT 'ADMIN puede editar inventario; CAJERO solo ventas',
    activo          TINYINT(1)    NOT NULL DEFAULT 1,
    creado_en       DATETIME      DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---- FACTURAS ----
CREATE TABLE IF NOT EXISTS facturas (
    numero_factura  VARCHAR(20)   PRIMARY KEY,
    fecha           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    nit_cliente     VARCHAR(20)   NOT NULL,
    id_cajero       VARCHAR(20)   NOT NULL,
    estado          ENUM('PENDIENTE','PAGADA','ANULADA') DEFAULT 'PENDIENTE',
    subtotal        DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    iva             DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    metodo_pago     ENUM('EFECTIVO','TARJETA_DEBITO','TARJETA_CREDITO','NEQUI','DAVIPLATA')
                    DEFAULT 'EFECTIVO',
    FOREIGN KEY (nit_cliente) REFERENCES clientes(nit)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (id_cajero)   REFERENCES cajeros(id_cajero)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---- ITEMS DE FACTURA ----
CREATE TABLE IF NOT EXISTS items_factura (
    id_item           INT AUTO_INCREMENT PRIMARY KEY,
    numero_factura    VARCHAR(20)   NOT NULL,
    id_producto       VARCHAR(20)   NOT NULL,
    cantidad          INT           NOT NULL,
    precio_unitario   DECIMAL(12,2) NOT NULL,
    subtotal_item     DECIMAL(12,2) NOT NULL,
    iva_item          DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    FOREIGN KEY (numero_factura) REFERENCES facturas(numero_factura)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (id_producto)    REFERENCES productos(id_producto)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---- INDICES PARA REPORTES ----
CREATE INDEX IF NOT EXISTS idx_fact_fecha   ON facturas(fecha);
CREATE INDEX IF NOT EXISTS idx_fact_estado  ON facturas(estado);
CREATE INDEX IF NOT EXISTS idx_fact_cajero  ON facturas(id_cajero);
CREATE INDEX IF NOT EXISTS idx_items_prod   ON items_factura(id_producto);

-- ================================================================
--  DATOS INICIALES
-- ================================================================

INSERT IGNORE INTO categorias VALUES
  (1, 'Granos y cereales', 0.0000),
  (2, 'Lacteos',           0.0000),
  (3, 'Bebidas',           0.1900),
  (4, 'Aseo personal',     0.1900),
  (5, 'Carnes',            0.0000),
  (6, 'Panaderia',         0.0000),
  (7, 'Snacks',            0.1900);

INSERT IGNORE INTO productos VALUES
  ('P001', 'Arroz Diana 1kg',       3500,  50, 1, 1),
  ('P002', 'Arroz Roa 500g',        1800,  40, 1, 1),
  ('P003', 'Leche Alpina 1L',       2900,  30, 2, 1),
  ('P004', 'Leche en polvo 400g',   9500,  20, 2, 1),
  ('P005', 'Gaseosa Postobón 2L',   5200,  25, 3, 1),
  ('P006', 'Agua cristal 600ml',    1800,  60, 3, 1),
  ('P007', 'Jugo Hit 1L',           4300,  35, 3, 1),
  ('P008', 'Jabon Palmolive x3',    7800,  40, 4, 1),
  ('P009', 'Shampoo Head x200ml',  12500,  18, 4, 1),
  ('P010', 'Pechuga de pollo 1kg',  9800,  15, 5, 1),
  ('P011', 'Pan tajado Bimbo',      4200,  22, 6, 1),
  ('P012', 'Papas Margarita 250g',  3500,  45, 7, 1);

-- Cliente por defecto (consumidor final)
INSERT IGNORE INTO clientes VALUES
  ('222222222', 'Consumidor Final', '', '', '', NOW());

-- Cajero admin (contrasena: admin123  ->  SHA-256)
-- Para calcular tu propio hash: SELECT SHA2('tucontrasena', 256);
INSERT IGNORE INTO cajeros VALUES
  ('C001', 'Administrador', 'MAÑANA',
   '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
   'ADMIN', 1, NOW());

-- Cajero de prueba (contrasena: 1234)
INSERT IGNORE INTO cajeros VALUES
  ('C002', 'Juan Perez', 'TARDE',
   '03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4',
   'CAJERO', 1, NOW());

-- ================================================================
--  VERIFICACION FINAL
-- ================================================================
SELECT 'categorias' AS tabla, COUNT(*) AS registros FROM categorias
UNION ALL SELECT 'productos',  COUNT(*) FROM productos
UNION ALL SELECT 'clientes',   COUNT(*) FROM clientes
UNION ALL SELECT 'cajeros',    COUNT(*) FROM cajeros;
