package supermercado.db;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Lee la configuracion del archivo config.properties.
 *
 * El archivo se busca en este orden de prioridad:
 * 1. Carpeta donde se ejecuta el programa: ./config.properties
 * 2. Carpeta del usuario: ~/supermercado/config.properties
 * 3. Classpath (src/main/resources/) solo para desarrollo en VS Code
 *
 * De esta forma las credenciales NUNCA van dentro del JAR.
 */
public class ConfiguracionApp {

    private static final String ARCHIVO = "config.properties";
    private static ConfiguracionApp instancia;
    private final Properties props = new Properties();

    // ---- Rutas de busqueda en orden de prioridad ----
    private static final Path[] RUTAS_BUSQUEDA = {
            Paths.get(ARCHIVO), // 1. Junto al JAR
            Paths.get(System.getProperty("user.home"), "supermercado", ARCHIVO), // 2. Carpeta usuario
            Paths.get("src", "main", "resources", ARCHIVO) // 3. Dev en VS Code
    };

    private ConfiguracionApp() {
        cargar();
    }

    public static ConfiguracionApp getInstance() {
        if (instancia == null)
            instancia = new ConfiguracionApp();
        return instancia;
    }

    // =========================================================
    // CARGA DEL ARCHIVO
    // =========================================================
    private void cargar() {
        // Intentar cada ruta en orden
        for (Path ruta : RUTAS_BUSQUEDA) {
            if (Files.exists(ruta)) {
                try (InputStream is = Files.newInputStream(ruta)) {
                    props.load(is);
                    System.out.println("[Config] Configuracion cargada desde: "
                            + ruta.toAbsolutePath());
                    return;
                } catch (IOException e) {
                    System.err.println("[Config] Error leyendo " + ruta + ": " + e.getMessage());
                }
            }
        }

        // Si no se encontro en disco, intentar classpath (solo desarrollo)
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream(ARCHIVO)) {
            if (is != null) {
                props.load(is);
                System.out.println("[Config] Configuracion cargada desde classpath (modo desarrollo).");
                System.out.println("[Config] ADVERTENCIA: Para produccion, coloque config.properties");
                System.out.println("[Config] junto al archivo JAR o en ~/supermercado/");
                return;
            }
        } catch (IOException e) {
            System.err.println("[Config] Error leyendo classpath: " + e.getMessage());
        }

        // No se encontro en ninguna parte
        throw new RuntimeException(
                "\n╔══════════════════════════════════════════════════════╗\n" +
                        "║          ARCHIVO DE CONFIGURACION NO ENCONTRADO      ║\n" +
                        "╠══════════════════════════════════════════════════════╣\n" +
                        "║  Crea el archivo config.properties con tus datos     ║\n" +
                        "║  de conexion a la base de datos.                     ║\n" +
                        "║                                                      ║\n" +
                        "║  Ubicaciones validas:                                ║\n" +
                        "║  1. Junto al programa:  ./config.properties          ║\n" +
                        "║  2. Carpeta usuario:    ~/supermercado/config.prop.. ║\n" +
                        "║                                                      ║\n" +
                        "║  Usa config.properties.template como guia.           ║\n" +
                        "╚══════════════════════════════════════════════════════╝");
    }

    // =========================================================
    // GETTERS TIPADOS
    // =========================================================
    public String get(String clave) {
        String val = props.getProperty(clave);
        if (val == null)
            throw new RuntimeException("[Config] Clave no encontrada en config.properties: " + clave);
        return val.trim();
    }

    public String get(String clave, String valorPorDefecto) {
        return props.getProperty(clave, valorPorDefecto).trim();
    }

    public int getInt(String clave, int defecto) {
        try {
            return Integer.parseInt(get(clave, String.valueOf(defecto)));
        } catch (NumberFormatException e) {
            return defecto;
        }
    }

    public boolean getBoolean(String clave, boolean defecto) {
        return Boolean.parseBoolean(get(clave, String.valueOf(defecto)));
    }

    // ---- Accesos directos de base de datos ----
    public String getMotor() {
        return get("db.motor", "postgresql");
    }

    public String getHost() {
        return get("db.host", "localhost");
    }

    public int getPuerto() {
        return getInt("db.puerto", 5432);
    }

    public String getNombreDB() {
        return get("db.nombre", "supermercado_db");
    }

    public String getUsuario() {
        return get("db.usuario", "postgres");
    }

    public String getPassword() {
        return get("db.password", "");
    }

    public String getZonaHoraria() {
        return get("db.zona_horaria", "America/Bogota");
    }

    public boolean isSsl() {
        return getBoolean("db.ssl", false);
    }

    public int getTimeoutConex() {
        return getInt("db.timeout_conexion", 30);
    }

    public int getConexMax() {
        return getInt("db.conexiones_max", 10);
    }

    public String getSchema() {
        return get("db.schema", "public");
    }

    // ---- Accesos directos de empresa ----
    public String getEmpresaNombre() {
        return get("empresa.nombre", "SUPERMERCADO");
    }

    public String getEmpresaNit() {
        return get("empresa.nit", "");
    }

    public String getEmpresaDireccion() {
        return get("empresa.direccion", "");
    }

    public String getEmpresaTelefono() {
        return get("empresa.telefono", "");
    }

    public String getEmpresaEmail() {
        return get("empresa.email", "");
    }

    public String getEmpresaRegimen() {
        return get("empresa.regimen", "Regimen Simplificado");
    }

    // ---- Accesos directos de rutas ----
    public String getRutaFacturas() {
        return get("rutas.facturas", "reportes/facturas");
    }

    public String getRutaReportes() {
        return get("rutas.reportes", "reportes");
    }

    /** Construye la URL JDBC segun el motor configurado. */
    public String buildJdbcUrl() {
        String motor = getMotor().toLowerCase();
        return switch (motor) {
            case "postgresql", "postgres" -> String.format(
                    "jdbc:postgresql://%s:%d/%s?sslmode=%s&currentSchema=%s&TimeZone=%s&connectTimeout=%d",
                    getHost(), getPuerto(), getNombreDB(),
                    isSsl() ? "require" : "disable",
                    getSchema(),
                    getZonaHoraria(), getTimeoutConex());

            case "mysql" -> String.format(
                    "jdbc:mysql://%s:%d/%s?useSSL=%s&serverTimezone=%s&useUnicode=true&characterEncoding=UTF-8",
                    getHost(), getPuerto(), getNombreDB(),
                    isSsl(), getZonaHoraria());

            default -> throw new RuntimeException(
                    "[Config] Motor no soportado: " + motor +
                            ". Use 'postgresql' o 'mysql' en config.properties");
        };
    }

    /** Devuelve el nombre completo del driver JDBC segun el motor. */
    public String getDriverClass() {
        return switch (getMotor().toLowerCase()) {
            case "postgresql", "postgres" -> "org.postgresql.Driver";
            case "mysql" -> "com.mysql.cj.jdbc.Driver";
            default -> throw new RuntimeException(
                    "[Config] Driver desconocido para motor: " + getMotor());
        };
    }
}
