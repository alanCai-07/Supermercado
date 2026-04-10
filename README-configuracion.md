# Guia de configuracion — Base de datos

## Archivos incluidos

| Archivo | Proposito | Va al repositorio |
|---|---|---|
| `config.properties.template` | Plantilla sin credenciales | SI |
| `config.properties` | Configuracion real con password | NO (en .gitignore) |
| `ConfiguracionApp.java` | Lee el archivo de configuracion | SI |
| `ConexionDB.java` | Conexion a la BD usando la config | SI |
| `.gitignore` | Protege config.properties | SI |

---

## Configuracion inicial (primera vez)

### Paso 1 — Crear tu archivo de configuracion

Copia la plantilla y renombrala:

```bash
# Desde la raiz del proyecto:
cp src/main/resources/config.properties.template \
   src/main/resources/config.properties
```

### Paso 2 — Editar con tus datos reales

Abre `src/main/resources/config.properties` y cambia:

```properties
# Para PostgreSQL:
db.motor=postgresql
db.host=localhost
db.puerto=5432
db.nombre=supermercado_db
db.usuario=postgres
db.password=TU_PASSWORD_REAL_AQUI

# Datos de tu empresa (salen en las facturas PDF):
empresa.nombre=MI SUPERMERCADO
empresa.nit=123.456.789-0
empresa.direccion=Calle 10 # 5-20, Cali
empresa.telefono=(602) 555-1234
```

### Paso 3 — Ejecutar el script SQL

En pgAdmin o psql, ejecuta el archivo `supermercado_postgresql.sql` para crear las tablas.

### Paso 4 — Ejecutar el programa

Desde VS Code haz clic en **Run** sobre `Main.java`, o desde terminal:

```bash
mvn compile exec:java -Dexec.mainClass="supermercado.Main"
```

---

## Cambiar de PostgreSQL a MySQL

Edita `config.properties`:

```properties
db.motor=mysql
db.puerto=3306
db.usuario=root
db.password=tu_password_mysql
```

Y en `pom.xml`, comenta el driver de PostgreSQL y descomenta el de MySQL.

---

## Donde se busca el archivo config.properties

El programa lo busca en este orden:

1. `./config.properties` — junto al JAR o en la raiz del proyecto
2. `~/supermercado/config.properties` — carpeta del usuario del sistema
3. Classpath — solo funciona en desarrollo desde VS Code

Para produccion (cuando entregas el programa), coloca el `config.properties` en la misma carpeta que el archivo `.jar`.

---

## Agregar un nuevo cajero con password

En pgAdmin ejecuta:

```sql
-- PostgreSQL: necesitas pgcrypto para calcular el hash
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO cajeros (id_cajero, nombre, turno, contrasena_hash, rol)
VALUES (
    'C004',
    'Maria Lopez',
    'MAÑANA',
    encode(digest('mi_contrasena', 'sha256'), 'hex'),
    'CAJERO'
);
```

O si no tienes pgcrypto, calcula el hash SHA-256 en cualquier herramienta online
e insertalo directamente como texto en `contrasena_hash`.
