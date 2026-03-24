package supermercado.ui;

import supermercado.servicio.SistemaFacturacion;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private JTextField  txtId;
    private JPasswordField txtPass;
    private JButton     btnLogin;
    private JLabel      lblError;

    public LoginFrame() {
        setTitle("Supermercado - Inicio de sesion");
        setSize(400, 320);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        construirUI();
    }

    private void construirUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);

        // Titulo
        JLabel titulo = new JLabel("SUPERMERCADO EL EXITO", SwingConstants.CENTER);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titulo.setForeground(new Color(34, 85, 153));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titulo, gbc);

        JLabel sub = new JLabel("Sistema de Facturacion", SwingConstants.CENTER);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(Color.GRAY);
        gbc.gridy = 1;
        panel.add(sub, gbc);

        JSeparator sep = new JSeparator();
        gbc.gridy = 2; gbc.insets = new Insets(4, 0, 12, 0);
        panel.add(sep, gbc);
        gbc.insets = new Insets(6, 0, 6, 0);

        // ID Cajero
        gbc.gridwidth = 1; gbc.gridy = 3; gbc.gridx = 0; gbc.weightx = 0.4;
        panel.add(new JLabel("ID Cajero:"), gbc);
        txtId = new JTextField(12);
        estilizarCampo(txtId);
        gbc.gridx = 1; gbc.weightx = 0.6;
        panel.add(txtId, gbc);

        // Contrasena
        gbc.gridy = 4; gbc.gridx = 0; gbc.weightx = 0.4;
        panel.add(new JLabel("Contrasena:"), gbc);
        txtPass = new JPasswordField(12);
        estilizarCampo(txtPass);
        gbc.gridx = 1; gbc.weightx = 0.6;
        panel.add(txtPass, gbc);

        // Error
        lblError = new JLabel(" ", SwingConstants.CENTER);
        lblError.setForeground(Color.RED);
        lblError.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        gbc.gridy = 5; gbc.gridx = 0; gbc.gridwidth = 2;
        panel.add(lblError, gbc);

        // Boton
        btnLogin = new JButton("Ingresar");
        estilizarBoton(btnLogin, new Color(34, 85, 153));
        gbc.gridy = 6;
        panel.add(btnLogin, gbc);

        add(panel);

        // Acciones
        btnLogin.addActionListener(e -> intentarLogin());
        txtPass.addActionListener(e -> intentarLogin());
        txtId.addActionListener(e -> txtPass.requestFocus());
    }

    private void intentarLogin() {
        String id   = txtId.getText().trim();
        String pass = new String(txtPass.getPassword());

        if (id.isEmpty() || pass.isEmpty()) {
            lblError.setText("Ingrese ID y contrasena.");
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Verificando...");

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override protected Boolean doInBackground() throws Exception {
                return SistemaFacturacion.getInstance().login(id, pass);
            }
            @Override protected void done() {
                try {
                    if (get()) {
                        dispose();
                        new MenuPrincipalFrame().setVisible(true);
                    } else {
                        lblError.setText("ID o contrasena incorrectos.");
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Ingresar");
                        txtPass.setText("");
                    }
                } catch (Exception ex) {
                    lblError.setText("Error de conexion: " + ex.getMessage());
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Ingresar");
                }
            }
        };
        worker.execute();
    }

    static void estilizarCampo(JTextField campo) {
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 190, 210)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }

    static void estilizarBoton(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 36));
    }

    public static void main(String[] args) {
        // Punto de entrada para prueba rapida del login
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new LoginFrame().setVisible(true);
        });
    }
}
