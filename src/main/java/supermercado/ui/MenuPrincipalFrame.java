package supermercado.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import supermercado.servicio.SistemaFacturacion;

public class MenuPrincipalFrame extends JFrame {

        private static final Color AZUL = new Color(34, 85, 153);
        private static final Color VERDE = new Color(30, 130, 76);
        private static final Color NARANJA = new Color(200, 100, 20);
        private static final Color ROJO = new Color(170, 40, 40);

        private final CardLayout cardLayout = new CardLayout();
        private final JPanel panelContenido = new JPanel(cardLayout);

        public MenuPrincipalFrame() {
                String cajero = SistemaFacturacion.getInstance().getCajeroActivo().getNombre();
                setTitle("Supermercado - Menu Principal  |  Cajero: " + cajero);
                // Aumentar tamaño para que los modulos se vean completos
                setSize(1150, 780);
                setMinimumSize(new Dimension(1000, 700));
                setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                setIconImage(AppIcon.getIcon());
                construirUI(cajero);
                // Centrar en la pantalla DESPUÉS de establecer el tamaño
                setLocationRelativeTo(null);
        }

        private void construirUI(String cajero) {
                JPanel root = new JPanel(new BorderLayout());
                root.setBackground(Color.WHITE);

                // ---- BANNER ----
                JPanel banner = new JPanel(new BorderLayout());
                banner.setBackground(AZUL);
                banner.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

                JLabel lblTitulo = new JLabel("SUPERMERCADO EL EXITO", SwingConstants.CENTER);
                lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));
                lblTitulo.setForeground(Color.WHITE);

                JButton btnInicio = new JButton("← Menú principal");
                btnInicio.setFocusPainted(false);
                btnInicio.setBorderPainted(false);
                btnInicio.setOpaque(true);
                btnInicio.setBackground(Color.WHITE);
                btnInicio.setForeground(AZUL);
                btnInicio.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btnInicio.addActionListener(e -> mostrarVista("menu"));

                banner.add(btnInicio, BorderLayout.WEST);
                banner.add(lblTitulo, BorderLayout.CENTER);
                root.add(banner, BorderLayout.NORTH);

                panelContenido.setBackground(Color.WHITE);
                panelContenido.add(crearVistaMenu(cajero), "menu");
                panelContenido.add(new DashboardPanel(), "dashboard");
                panelContenido.add(crearVistaModulo(new NuevaVentaFrame(this), "Nueva Venta"), "nuevaVenta");
                panelContenido.add(crearVistaModulo(new InventarioFrame(), "Inventario"), "inventario");
                panelContenido.add(crearVistaModulo(new ClienteFrame(), "Clientes"), "clientes");
                panelContenido.add(crearVistaModulo(new ReportesFrame(), "Reportes"), "reportes");
                panelContenido.add(crearVistaModulo(new BuscarFacturaFrame(), "Buscar Factura"), "buscarFactura");
                // Forzar preferencia de tamaño del area de contenido
                panelContenido.setPreferredSize(new Dimension(1100, 640));

                // ---- BARRA LATERAL (DASHBOARD) ----
                JPanel sidebar = new JPanel();
                sidebar.setBackground(new Color(245, 250, 245));
                sidebar.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
                sidebar.setLayout(new javax.swing.BoxLayout(sidebar, javax.swing.BoxLayout.Y_AXIS));

                JButton sNuevaVenta = botonMenu("Nueva Venta", "", AZUL);
                JButton sInventario = botonMenu("Inventario", "", VERDE);
                JButton sClientes = botonMenu("Clientes", "", VERDE);
                JButton sReportes = botonMenu("Reportes", "", NARANJA);
                JButton sBuscar = botonMenu("Buscar Factura", "", new Color(80, 80, 150));
                JButton sSalir = botonMenu("Cerrar Sesion", "", ROJO);

                sNuevaVenta.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
                sInventario.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
                sClientes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
                sReportes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
                sBuscar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
                sSalir.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

                sidebar.add(sNuevaVenta);
                sidebar.add(Box.createVerticalStrut(8));
                sidebar.add(sInventario);
                sidebar.add(Box.createVerticalStrut(8));
                sidebar.add(sClientes);
                sidebar.add(Box.createVerticalStrut(8));
                sidebar.add(sReportes);
                sidebar.add(Box.createVerticalStrut(8));
                sidebar.add(sBuscar);
                sidebar.add(Box.createVerticalStrut(12));
                sidebar.add(sSalir);

                sNuevaVenta.addActionListener(e -> mostrarVista("nuevaVenta"));
                sInventario.addActionListener(e -> mostrarVista("inventario"));
                sClientes.addActionListener(e -> mostrarVista("clientes"));
                sReportes.addActionListener(e -> mostrarVista("reportes"));
                sBuscar.addActionListener(e -> mostrarVista("buscarFactura"));
                // Dashboard button (first view)
                JButton sDashboard = botonMenu("Dashboard", "", new Color(60, 60, 60));
                sDashboard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
                sidebar.add(Box.createVerticalStrut(8));
                sidebar.add(sDashboard);
                sDashboard.addActionListener(e -> mostrarVista("dashboard"));
                sSalir.addActionListener(e -> {
                        int r = JOptionPane.showConfirmDialog(this,
                                        "¿Desea cerrar la sesion?", "Confirmar",
                                        JOptionPane.YES_NO_OPTION);
                        if (r == JOptionPane.YES_OPTION) {
                                SistemaFacturacion.getInstance().logout();
                                dispose();
                                new LoginFrame().setVisible(true);
                        }
                });

                root.add(sidebar, BorderLayout.WEST);
                root.add(panelContenido, BorderLayout.CENTER);
                add(root);

                mostrarVista("dashboard");
        }

        private JPanel crearVistaMenu(String cajero) {
                JPanel panel = new JPanel(new BorderLayout());
                panel.setBackground(Color.WHITE);

                JLabel lblBienvenida = new JLabel("Bienvenido, " + cajero + "  —  Turno: "
                                + SistemaFacturacion.getInstance().getCajeroActivo().getTurno(),
                                SwingConstants.CENTER);
                lblBienvenida.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                lblBienvenida.setBorder(BorderFactory.createEmptyBorder(12, 0, 4, 0));
                panel.add(lblBienvenida, BorderLayout.NORTH);

                JPanel grid = new JPanel(new GridLayout(2, 3, 16, 16));
                grid.setBackground(Color.WHITE);
                grid.setBorder(BorderFactory.createEmptyBorder(20, 40, 30, 40));

                JButton btnNuevaVenta = botonMenu("Nueva Venta",
                                "Registrar una nueva factura", AZUL);
                JButton btnInventario = botonMenu("Inventario",
                                "Ver y gestionar productos", VERDE);
                JButton btnClientes = botonMenu("Clientes",
                                "Registrar o buscar clientes", VERDE);
                JButton btnReportes = botonMenu("Reportes PDF",
                                "Generar reportes de ventas", NARANJA);
                JButton btnBuscarFact = botonMenu("Buscar Factura",
                                "Consultar o anular facturas", new Color(80, 80, 150));
                JButton btnSalir = botonMenu("Cerrar Sesion",
                                "Salir del sistema", ROJO);

                grid.add(btnNuevaVenta);
                grid.add(btnInventario);
                grid.add(btnClientes);
                grid.add(btnReportes);
                grid.add(btnBuscarFact);
                grid.add(btnSalir);

                panel.add(grid, BorderLayout.CENTER);

                btnNuevaVenta.addActionListener(e -> mostrarVista("nuevaVenta"));
                btnInventario.addActionListener(e -> mostrarVista("inventario"));
                btnClientes.addActionListener(e -> mostrarVista("clientes"));
                btnReportes.addActionListener(e -> mostrarVista("reportes"));
                btnBuscarFact.addActionListener(e -> mostrarVista("buscarFactura"));

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

                return panel;
        }

        private JPanel crearVistaModulo(JFrame frame, String nombreVista) {
                JPanel panel = new JPanel(new BorderLayout());
                panel.setBackground(Color.WHITE);
                panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

                if (frame != null) {
                        // Try to extract a root panel if the frame exposes getRootPanel()
                        JPanel moduleRoot = null;
                        try {
                                java.lang.reflect.Method m = frame.getClass().getMethod("getRootPanel");
                                Object res = m.invoke(frame);
                                if (res instanceof JPanel)
                                        moduleRoot = (JPanel) res;
                        } catch (Exception ignored) {
                        }

                        if (moduleRoot != null) {
                                // Remove from any existing parent (e.g., the JFrame)
                                if (moduleRoot.getParent() instanceof java.awt.Container) {
                                        ((java.awt.Container) moduleRoot.getParent()).remove(moduleRoot);
                                }
                                panel.add(moduleRoot, BorderLayout.CENTER);
                                // Dispose the original frame to free resources
                                frame.dispose();
                        } else {
                                frame.setVisible(false);
                                panel.add(frame.getContentPane(), BorderLayout.CENTER);
                        }
                }

                return panel;
        }

        private void mostrarVista(String nombreVista) {
                cardLayout.show(panelContenido, nombreVista);
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
                        public void mouseEntered(java.awt.event.MouseEvent e) {
                                btn.setBackground(hover);
                        }

                        public void mouseExited(java.awt.event.MouseEvent e) {
                                btn.setBackground(color);
                        }
                });
                return btn;
        }
}
