package supermercado.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import supermercado.reporte.GeneradorReportePDF;
import supermercado.servicio.SistemaFacturacion;
import supermercado.reporte.GeneradorReporteExcel;

public class ReportesFrame extends JFrame {

    private final SistemaFacturacion sistema = SistemaFacturacion.getInstance();
    private JTextField txtDesde, txtHasta;
    private JLabel lblEstado;
    private JPanel rootPanel;

    public ReportesFrame() {
        setTitle("Generar Reportes PDF");
        setSize(520, 420);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setIconImage(AppIcon.getIcon());
        setResizable(false);
        construirUI();
        // Centrar en la pantalla DESPUÉS de establecer el tamaño
        setLocationRelativeTo(null);
    }

    private void construirUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        root.setBackground(Color.WHITE);
        this.rootPanel = root;

        // Titulo
        JLabel titulo = new JLabel("Generacion de Reportes", SwingConstants.CENTER);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titulo.setForeground(new Color(34, 85, 153));
        root.add(titulo, BorderLayout.NORTH);

        // Fechas
        JPanel panelFechas = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 6));
        panelFechas.setBackground(Color.WHITE);
        panelFechas.setBorder(BorderFactory.createTitledBorder("Rango de fechas (yyyy-MM-dd)"));

        String hoy = LocalDate.now().toString();
        String primerDiaMes = LocalDate.now().withDayOfMonth(1).toString();

        txtDesde = new JTextField(primerDiaMes, 12);
        txtHasta = new JTextField(hoy, 12);
        UIUtils.estilizarCampo(txtDesde);
        UIUtils.estilizarCampo(txtHasta);

        panelFechas.add(new JLabel("Desde:"));
        panelFechas.add(txtDesde);
        panelFechas.add(new JLabel("Hasta:"));
        panelFechas.add(txtHasta);
        root.add(panelFechas, BorderLayout.CENTER);

        // Botones de reportes
        JPanel panelBotones = new JPanel(new GridLayout(4, 1, 0, 10));
        panelBotones.setBackground(Color.WHITE);

        JButton btnDiario = boton("Reporte de Ventas del Dia (hoy)", new Color(34, 85, 153));
        JButton btnProductos = boton("Top 20 Productos Mas Vendidos (rango)", new Color(30, 130, 76));
        JButton btnCajero = boton("Ventas por Cajero (rango)", new Color(140, 80, 10));

        lblEstado = new JLabel(" ", SwingConstants.CENTER);
        lblEstado.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblEstado.setForeground(new Color(30, 130, 76));

        panelBotones.add(btnDiario);
        panelBotones.add(btnProductos);
        panelBotones.add(btnCajero);
        panelBotones.add(lblEstado);
        root.add(panelBotones, BorderLayout.SOUTH);

        add(root);

        // ---- ACCIONES ----
        btnDiario.addActionListener(e -> generarReporte("DIARIO"));
        btnProductos.addActionListener(e -> generarReporte("PRODUCTOS"));
        btnCajero.addActionListener(e -> generarReporte("CAJERO"));
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    private void generarReporte(String tipo) {
        LocalDate desde, hasta;
        try {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            desde = LocalDate.parse(txtDesde.getText().trim(), fmt);
            hasta = LocalDate.parse(txtHasta.getText().trim(), fmt);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Formato de fecha invalido. Use: yyyy-MM-dd", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        lblEstado.setText("Generando PDF...");
        lblEstado.setForeground(new Color(34, 85, 153));
        final LocalDate d = desde, h = hasta;

        SwingWorker<String[], Void> worker = new SwingWorker<>() {
            @Override
            protected String[] doInBackground() throws Exception {
                String rutaPdf, rutaExcel;
                switch (tipo) {
                    case "DIARIO" -> {
                        rutaPdf = GeneradorReportePDF.reporteVentasDiarias(LocalDate.now(), sistema.getFacturaDAO());
                        rutaExcel = GeneradorReporteExcel.reporteVentasDiariasExcel(LocalDate.now(),
                                sistema.getFacturaDAO());
                    }
                    case "PRODUCTOS" -> {
                        rutaPdf = GeneradorReportePDF.reporteTopProductos(d, h, sistema.getFacturaDAO());
                        rutaExcel = GeneradorReporteExcel.reporteTopProductosExcel(d, h, sistema.getFacturaDAO());
                    }
                    case "CAJERO" -> {
                        rutaPdf = GeneradorReportePDF.reporteVentasPorCajero(d, h, sistema.getFacturaDAO());
                        rutaExcel = GeneradorReporteExcel.reporteVentasPorCajeroExcel(d, h, sistema.getFacturaDAO());
                    }
                    default -> throw new Exception("Tipo desconocido");
                }
                return new String[] { rutaPdf, rutaExcel };
            }

            @Override
            protected void done() {
                try {
                    String[] rutas = get();
                    String rutaPdf = rutas[0];
                    String rutaExcel = rutas[1];

                    lblEstado.setText("PDF y Excel generados correctamente.");
                    lblEstado.setForeground(new Color(30, 130, 76));

                    String[] opciones = { "Abrir PDF", "Abrir Excel", "Cerrar" };
                    int resp = JOptionPane.showOptionDialog(ReportesFrame.this,
                            "Reporte generado exitosamente.\n\n" +
                                    "PDF:   " + rutaPdf + "\n" +
                                    "Excel: " + rutaExcel,
                            "Exito", JOptionPane.DEFAULT_OPTION,
                            JOptionPane.INFORMATION_MESSAGE, null, opciones, opciones[0]);

                    if (resp == 0)
                        Desktop.getDesktop().open(new File(rutaPdf));
                    else if (resp == 1)
                        Desktop.getDesktop().open(new File(rutaExcel));

                } catch (Exception ex) {
                    lblEstado.setText("Error: " + ex.getMessage());
                    lblEstado.setForeground(Color.RED);
                    JOptionPane.showMessageDialog(ReportesFrame.this,
                            "Error al generar reportes: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();

    }

    private JButton boton(String texto, Color color) {
        JButton b = new JButton(texto);
        UIUtils.estilizarBoton(b, color);
        b.setPreferredSize(new Dimension(0, 42));
        return b;
    }
}
