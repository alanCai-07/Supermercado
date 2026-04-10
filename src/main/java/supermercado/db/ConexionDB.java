package supermercado.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton de conexion a la base de datos.
 *
 * Lee TODOS los parametros desde ConfiguracionApp,
 * que a su vez los obtiene de config.properties.
 * Ningun dato de conexion esta hardcodeado aqui.
 */
public class ConexionDB {

    private static Connection conexion;

    private ConexionDB() {}

    public static Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            conectar();
        }
        return conexion;
    }

    private static void conectar() throws SQLException {
        ConfiguracionApp cfg = ConfiguracionApp.getInstance();
        try {
            // Cargar driver dinamicamente segun el motor configurado
            Class.forName(cfg.getDriverClass());

            String url      = cfg.buildJdbcUrl();
            String usuario  = cfg.getUsuario();
            String password = cfg.getPassword();

            conexion = DriverManager.getConnection(url, usuario, password);
            conexion.setAutoCommit(true);

            System.out.println("[DB] Conectado a " + cfg.getMotor().toUpperCase()
                    + " en " + cfg.getHost() + ":" + cfg.getPuerto()
                    + "/" + cfg.getNombreDB());

        } catch (ClassNotFoundException e) {
            throw new SQLException(
                "[DB] Driver JDBC no encontrado para motor: " + cfg.getMotor() +
                "\nVerifica que la dependencia este en pom.xml.\n" + e.getMessage());
        }
    }

    public static void cerrar() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
                System.out.println("[DB] Conexion cerrada correctamente.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error al cerrar conexion: " + e.getMessage());
        }
    }

    /** Verifica si la conexion esta activa (util para health-checks). */
    public static boolean isConectado() {
        try {
            return conexion != null && !conexion.isClosed() && conexion.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
}
