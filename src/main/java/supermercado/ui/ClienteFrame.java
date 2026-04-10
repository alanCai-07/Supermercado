package supermercado.ui;

import supermercado.dao.ClienteDAO;
import supermercado.modelo.Cliente;

import javax.swing.*;
import java.awt.*;

public class ClienteFrame extends JFrame {

    private JTextField txtNit, txtNombre, txtTel, txtEmail, txtDir;
    private JLabel lblMsg;

    public ClienteFrame() {
        setTitle("Registrar / Buscar Cliente");
        setSize(440, 380);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        construirUI();
        // Centrar en la pantalla DESPUÉS de establecer el tamaño
        setLocationRelativeTo(null);
    }

    private void construirUI() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        root.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 4, 5, 4);
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel titulo = new JLabel("Gestion de Clientes", SwingConstants.CENTER);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titulo.setForeground(new Color(34, 85, 153));
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        root.add(titulo, g);

        String[] labels = { "NIT / CC:", "Nombre:", "Telefono:", "Email:", "Direccion:" };
        txtNit = campo();
        txtNombre = campo();
        txtTel = campo();
        txtEmail = campo();
        txtDir = campo();
        JTextField[] campos = { txtNit, txtNombre, txtTel, txtEmail, txtDir };

        for (int i = 0; i < labels.length; i++) {
            g.gridwidth = 1;
            g.gridy = i + 1;
            g.gridx = 0;
            g.weightx = 0.3;
            root.add(new JLabel(labels[i]), g);
            g.gridx = 1;
            g.weightx = 0.7;
            root.add(campos[i], g);
        }

        lblMsg = new JLabel(" ", SwingConstants.CENTER);
        lblMsg.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        g.gridy = 6;
        g.gridx = 0;
        g.gridwidth = 2;
        root.add(lblMsg, g);

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        btnPanel.setBackground(Color.WHITE);
        JButton btnBuscar = new JButton("Buscar por NIT");
        JButton btnGuardar = new JButton("Guardar");
        LoginFrame.estilizarBoton(btnBuscar, new Color(80, 80, 150));
        LoginFrame.estilizarBoton(btnGuardar, new Color(30, 130, 76));
        btnPanel.add(btnBuscar);
        btnPanel.add(btnGuardar);
        g.gridy = 7;
        root.add(btnPanel, g);

        add(root);

        btnBuscar.addActionListener(e -> {
            String nit = txtNit.getText().trim();
            if (nit.isEmpty()) {
                lblMsg.setText("Ingrese el NIT.");
                return;
            }
            try {
                Cliente c = new ClienteDAO().buscar(nit);
                if (c != null) {
                    txtNombre.setText(c.getNombre());
                    txtTel.setText(c.getTelefono());
                    txtEmail.setText(c.getEmail());
                    txtDir.setText(c.getDireccion());
                    lblMsg.setForeground(new Color(30, 130, 76));
                    lblMsg.setText("Cliente encontrado.");
                } else {
                    lblMsg.setForeground(Color.ORANGE.darker());
                    lblMsg.setText("Cliente no encontrado. Puede registrarlo.");
                }
            } catch (Exception ex) {
                lblMsg.setForeground(Color.RED);
                lblMsg.setText("Error: " + ex.getMessage());
            }
        });

        btnGuardar.addActionListener(e -> {
            if (txtNit.getText().isBlank() || txtNombre.getText().isBlank()) {
                lblMsg.setForeground(Color.RED);
                lblMsg.setText("NIT y Nombre son obligatorios.");
                return;
            }
            try {
                Cliente c = new Cliente(txtNit.getText().trim(),
                        txtNombre.getText().trim(), txtTel.getText().trim(),
                        txtEmail.getText().trim(), txtDir.getText().trim());
                new ClienteDAO().guardar(c);
                lblMsg.setForeground(new Color(30, 130, 76));
                lblMsg.setText("Cliente guardado correctamente.");
            } catch (Exception ex) {
                lblMsg.setForeground(Color.RED);
                lblMsg.setText("Error: " + ex.getMessage());
            }
        });
    }

    private JTextField campo() {
        JTextField t = new JTextField(18);
        LoginFrame.estilizarCampo(t);
        return t;
    }
}
