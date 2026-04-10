package supermercado.ui;

import supermercado.dao.CajeroDAO;
import supermercado.db.ConexionDB;
import supermercado.servicio.SistemaFacturacion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class LoginFrame extends JFrame {

    private JComboBox<String> cmbNombre;
    private JPasswordField    txtPass;
    private JButton           btnLogin;
    private JButton           btnRecargar;
    private JLabel            lblError;

    public LoginFrame() {
        setTitle("Supermercado — Inicio de sesion");
        setSize(420, 360);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        construirUI();
        // Cargar nombres DESPUES de construir la UI,
        // en un hilo aparte para no bloquear el EDT
        cargarNombresAsync();
    }

    private void construirUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 44, 20, 44));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(6, 0, 6, 0);

        // Titulo
        JLabel titulo = new JLabel("SUPERMERCADO EL EXITO", SwingConstants.CENTER);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titulo.setForeground(new Color(34, 85, 153));
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        panel.add(titulo, g);

        JLabel sub = new JLabel("Sistema de Facturacion", SwingConstants.CENTER);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(Color.GRAY);
        g.gridy = 1;
        panel.add(sub, g);

        JSeparator sep = new JSeparator();
        g.gridy = 2;
        g.insets = new Insets(4, 0, 14, 0);
        panel.add(sep, g);
        g.insets = new Insets(6, 0, 6, 0);

        // Combo usuario
        g.gridwidth = 1; g.gridy = 3; g.gridx = 0; g.weightx = 0.38;
        panel.add(new JLabel("Usuario:"), g);

        cmbNombre = new JComboBox<>(new String[]{"Cargando..."});
        cmbNombre.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbNombre.setEnabled(false);
        g.gridx = 1; g.weightx = 0.62;
        panel.add(cmbNombre, g);

        // Contrasena
        g.gridy = 4; g.gridx = 0; g.weightx = 0.38;
        panel.add(new JLabel("Contrasena:"), g);

        txtPass = new JPasswordField();
        estilizarCampo(txtPass);
        g.gridx = 1; g.weightx = 0.62;
        panel.add(txtPass, g);

        // Label error / info
        lblError = new JLabel("Conectando a la base de datos...", SwingConstants.CENTER);
        lblError.setForeground(new Color(100, 100, 100));
        lblError.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        g.gridy = 5; g.gridx = 0; g.gridwidth = 2;
        panel.add(lblError, g);

        // Panel botones: Recargar + Ingresar
        JPanel panelBtns = new JPanel(new GridLayout(1, 2, 8, 0));
        panelBtns.setBackground(Color.WHITE);

        btnRecargar = new JButton("Recargar");
        btnLogin    = new JButton("Ingresar");
        estilizarBoton(btnRecargar, new Color(100, 100, 100));
        estilizarBoton(btnLogin,    new Color(34, 85, 153));
        btnLogin   .setEnabled(false);
        btnRecargar.setEnabled(false);

        panelBtns.add(btnRecargar);
        panelBtns.add(btnLogin);

        g.gridy = 6;
        panel.add(panelBtns, g);

        add(panel);

        // Acciones
        btnLogin   .addActionListener(e -> intentarLogin());
        btnRecargar.addActionListener(e -> cargarNombresAsync());
        txtPass    .addActionListener(e -> intentarLogin());
    }

    // =========================================================
    //  CARGA DE CAJEROS EN HILO SEPARADO
    // =========================================================
    private void cargarNombresAsync() {
        // Resetear estado visual
        cmbNombre  .setEnabled(false);
        btnLogin   .setEnabled(false);
        btnRecargar.setEnabled(false);
        lblError.setText("Conectando a la base de datos...");
        lblError.setForeground(new Color(100, 100, 100));
        cmbNombre.removeAllItems();
        cmbNombre.addItem("Cargando...");

        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                // Forzar nueva conexion para verificar que la BD responde
                ConexionDB.cerrar();
                ConexionDB.getConexion(); // lanza SQLException si falla
                return new CajeroDAO().listarNombres();
            }

            @Override
            protected void done() {
                try {
                    List<String> nombres = get();
                    cmbNombre.removeAllItems();

                    if (nombres.isEmpty()) {
                        cmbNombre.addItem("(Sin cajeros registrados)");
                        lblError.setText("No hay cajeros activos en la BD.");
                        lblError.setForeground(Color.ORANGE.darker());
                    } else {
                        for (String n : nombres) cmbNombre.addItem(n);
                        cmbNombre.setEnabled(true);
                        btnLogin.setEnabled(true);
                        lblError.setText("Seleccione su usuario e ingrese la contrasena.");
                        lblError.setForeground(new Color(60, 120, 60));
                        txtPass.requestFocus();
                    }
                } catch (Exception ex) {
                    // Mostrar error detallado para ayudar a diagnosticar
                    cmbNombre.removeAllItems();
                    cmbNombre.addItem("(Sin conexion)");

                    String msg = ex.getCause() != null
                            ? ex.getCause().getMessage()
                            : ex.getMessage();

                    lblError.setText("<html><center>Error de conexion.<br>"
                            + "<font size='2'>" + truncar(msg, 55) + "</font></center></html>");
                    lblError.setForeground(Color.RED);

                    // Mostrar dialogo con el error completo
                    JOptionPane.showMessageDialog(LoginFrame.this,
                            "No se pudo conectar a la base de datos.\n\n"
                            + "Verifique:\n"
                            + "  1. Que PostgreSQL / MySQL este corriendo\n"
                            + "  2. Que el archivo config.properties exista\n"
                            + "  3. Que el usuario y password sean correctos\n\n"
                            + "Detalle: " + msg,
                            "Error de conexion",
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnRecargar.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    // =========================================================
    //  LOGIN
    // =========================================================
    private void intentarLogin() {
        String nombre = (String) cmbNombre.getSelectedItem();
        String pass   = new String(txtPass.getPassword());

        if (nombre == null || nombre.startsWith("(") || pass.isEmpty()) {
            lblError.setText("Seleccione un usuario e ingrese la contrasena.");
            lblError.setForeground(Color.RED);
            return;
        }

        btnLogin   .setEnabled(false);
        btnLogin   .setText("Verificando...");
        btnRecargar.setEnabled(false);
        lblError   .setText(" ");

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override protected Boolean doInBackground() throws Exception {
                return SistemaFacturacion.getInstance().login(nombre, pass);
            }
            @Override protected void done() {
                try {
                    if (get()) {
                        dispose();
                        new MenuPrincipalFrame().setVisible(true);
                    } else {
                        lblError.setText("Contrasena incorrecta. Intente de nuevo.");
                        lblError.setForeground(Color.RED);
                        txtPass.setText("");
                        txtPass.requestFocus();
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Ingresar");
                        btnRecargar.setEnabled(true);
                    }
                } catch (Exception ex) {
                    lblError.setText("Error: " + ex.getMessage());
                    lblError.setForeground(Color.RED);
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Ingresar");
                    btnRecargar.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    // =========================================================
    //  UTILIDADES ESTATICAS (usadas por otras pantallas)
    // =========================================================
    public static void estilizarCampo(JTextField campo) {
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 190, 210)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }

    public static void estilizarBoton(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 36));
    }

    private static String truncar(String s, int max) {
        if (s == null) return "Sin detalles";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new LoginFrame().setVisible(true);
        });
    }
}
