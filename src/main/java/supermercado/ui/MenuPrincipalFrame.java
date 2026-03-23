package supermercado.ui;

import supermercado.servicio.SistemaFacturacion;

import javax.swing.*;
import java.awt.*;

public class MenuPrincipalFrame extends JFrame {

    private static final Color AZUL    = new Color(34, 85, 153);
    private static final Color VERDE   = new Color(30, 130, 76);
    private static final Color NARANJA = new Color(200, 100, 20);
    private static final Color ROJO    = new Color(170, 40, 40);

    public MenuPrincipalFrame() {
        String cajero = SistemaFacturacion.getInstance().getCajeroActivo().getNombre();
        setTitle("Supermercado - Menu Principal  |  Cajero: " + cajero);
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        construirUI(cajero);
    }

    private void construirUI(String cajero) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // ---- BANNER ----
        JPanel banner = new JPanel();
        banner.setBackground(AZUL);
        banner.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        JLabel lblTitulo = new JLabel("SUPERMERCADO EL EXITO", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitulo.setForeground(Color.WHITE);
        banner.add(lblTitulo);
        root.add(banner, BorderLayout.NORTH);

        // ---- BIENVENIDA ----
        JLabel lblBienvenida = new JLabel("Bienvenido, " + cajero + "  —  Turno: "
                + SistemaFacturacion.getInstance().getCajeroActivo().getTurno(),
                SwingConstants.CENTER);
        lblBienvenida.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblBienvenida.setBorder(BorderFactory.createEmptyBorder(12, 0, 4, 0));
        root.add(lblBienvenida, BorderLayout.CENTER);

        // ---- BOTONES DE MENU ----
        JPanel grid = new JPanel(new GridLayout(2, 3, 16, 16));
        grid.setBackground(Color.WHITE);
        grid.setBorder(BorderFactory.createEmptyBorder(20, 40, 30, 40));

        JButton btnNuevaVenta = botonMenu("Nueva Venta",
                "Registrar una nueva factura", AZUL);
        JButton btnInventario = botonMenu("Inventario",
                "Ver y gestionar productos", VERDE);
        JButton btnClientes   = botonMenu("Clientes",
                "Registrar o buscar clientes", VERDE);
        JButton btnReportes   = botonMenu("Reportes PDF",
                "Generar reportes de ventas", NARANJA);
        JButton btnBuscarFact = botonMenu("Buscar Factura",
                "Consultar o anular facturas", new Color(80, 80, 150));
        JButton btnSalir      = botonMenu("Cerrar Sesion",
                "Salir del sistema", ROJO);

        grid.add(btnNuevaVenta);
        grid.add(btnInventario);
        grid.add(btnClientes);
        grid.add(btnReportes);
        grid.add(btnBuscarFact);
        grid.add(btnSalir);

        root.add(grid, BorderLayout.SOUTH);
        add(root);

        // ---- ACCIONES ----
        btnNuevaVenta.addActionListener(e ->
                new NuevaVentaFrame(this).setVisible(true));

        btnInventario.addActionListener(e ->
                new InventarioFrame().setVisible(true));

        btnClientes.addActionListener(e ->
                new ClienteFrame().setVisible(true));

        btnReportes.addActionListener(e ->
                new ReportesFrame().setVisible(true));

        btnBuscarFact.addActionListener(e ->
                new BuscarFacturaFrame().setVisible(true));

        btnSalir.addActionListener(e -> {
            int r = JOptionPane.showConfirmDialog(this,
                    "¿Desea cerrar la sesion?", "Confirmar",
                    JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.YES_OPTION) {
                SistemaFacturacion.getInstance().logout();
                dispose();
                new LoginFrame().setVisible(true);
            }
        });
    }

    private JButton botonMenu(String titulo, String subtitulo, Color color) {
        JButton btn = new JButton("<html><center><b>" + titulo + "</b><br>"
                + "<span style='font-size:9px;color:#ddd'>" + subtitulo + "</span>"
                + "</center></html>");
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(160, 90));

        // Efecto hover
        Color hover = color.brighter();
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(hover); }
            public void mouseExited (java.awt.event.MouseEvent e) { btn.setBackground(color); }
        });
        return btn;
    }
}
