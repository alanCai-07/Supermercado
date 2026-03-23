package supermercado;

import supermercado.db.ConexionDB;
import supermercado.ui.LoginFrame;

import javax.swing.*;

/**
 * Punto de entrada principal del sistema de facturacion.
 * Ejecutar con: mvn compile exec:java  o  desde VS Code con el boton Run.
 */
public class Main {

    public static void main(String[] args) {
        // Usar apariencia del sistema operativo
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Verificar conexion a la base de datos antes de abrir la UI
        SwingUtilities.invokeLater(() -> {
            try {
                ConexionDB.getConexion();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "No se pudo conectar a MySQL.\n\n" +
                        "Verifique que:\n" +
                        "  1. MySQL este corriendo en localhost:3306\n" +
                        "  2. La base de datos 'supermercado_db' exista\n" +
                        "  3. El usuario y password en ConexionDB.java sean correctos\n\n" +
                        "Error: " + e.getMessage(),
                        "Error de conexion", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }

            // Agregar hook de cierre para cerrar la conexion BD
            Runtime.getRuntime().addShutdownHook(new Thread(ConexionDB::cerrar));

            // Mostrar pantalla de login
            new LoginFrame().setVisible(true);
        });
    }
}
