package supermercado.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton para la conexion a MySQL.
 * Edita URL, USUARIO y PASSWORD segun tu configuracion local.
 */
public class ConexionDB {

    // ===== CONFIGURA AQUI TUS DATOS DE MYSQL =====
    private static final String URL      = "jdbc:mysql://localhost:3306/supermercado_db"
                                         + "?useSSL=false&serverTimezone=America/Bogota"
                                         + "&useUnicode=true&characterEncoding=UTF-8";
    private static final String USUARIO  = "root";
    private static final String PASSWORD = "@lanCai07!"; // <-- cambia esto
    // =============================================

    private static Connection conexion;

    private ConexionDB() {}

    public static Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                conexion = DriverManager.getConnection(URL, USUARIO, PASSWORD);
                conexion.setAutoCommit(true);
                System.out.println("[DB] Conexion establecida con MySQL.");
            } catch (ClassNotFoundException e) {
                throw new SQLException("Driver MySQL no encontrado: " + e.getMessage());
            }
        }
        return conexion;
    }

    public static void cerrar() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
                System.out.println("[DB] Conexion cerrada.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Error al cerrar: " + e.getMessage());
        }
    }
}
