package supermercado.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;
import java.util.Currency;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import java.awt.Paint;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import javax.swing.Box;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import supermercado.servicio.SistemaFacturacion;
import supermercado.dao.FacturaDAO;

public class DashboardPanel extends JPanel {

    private final SistemaFacturacion sistema = SistemaFacturacion.getInstance();
    private final LocalDate desde;
    private final LocalDate hasta;
    private boolean actualizando = false;

    public DashboardPanel() {
        this(LocalDate.now(), LocalDate.now());
    }

    public DashboardPanel(LocalDate desde, LocalDate hasta) {
        this.desde = desde;
        this.hasta = hasta;
        inicializarUI();
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                refresh();
            }
        });
    }

    private void inicializarUI() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(Color.WHITE);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel titulo = new JLabel("Dashboard", SwingConstants.LEFT);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setForeground(new Color(34, 85, 153));
        titulo.setBorder(BorderFactory.createEmptyBorder(4, 8, 10, 8));
        header.add(titulo, BorderLayout.NORTH);

        JLabel rangoLabel = new JLabel("Rango: " + desde + " a " + hasta, SwingConstants.LEFT);
        rangoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rangoLabel.setForeground(new Color(100, 100, 100));
        rangoLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        header.add(rangoLabel, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);

        JPanel main = new JPanel(new GridBagLayout());
        main.setOpaque(true);
        main.setBackground(Color.WHITE);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);

        // Tarjetas superiores: resumen rapido
        JPanel tarjetas = new JPanel();
        tarjetas.setLayout(new BoxLayout(tarjetas, BoxLayout.X_AXIS));
        tarjetas.setOpaque(false);

        java.text.NumberFormat defaultFormat = java.text.NumberFormat
                .getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        defaultFormat.setMaximumFractionDigits(0);
        defaultFormat.setMinimumFractionDigits(0);
        try {
            defaultFormat.setCurrency(Currency.getInstance("COP"));
        } catch (Exception ignored) {
        }
        JPanel cardTotal = tarjetaResumen("Total Ventas (hoy)", defaultFormat.format(0), new Color(30, 120, 220));
        JPanel cardFacturas = tarjetaResumen("Facturas (hoy)", "0", new Color(30, 120, 220));
        tarjetas.add(cardTotal);
        tarjetas.add(cardFacturas);

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        main.add(tarjetas, c);

        // Area central: resumen textual (gráficos deshabilitados)
        // mantenemos estas estructuras fuera del try para poder usar sus valores
        Map<String, Double> porHora = new HashMap<>();
        Map<String, Integer> pagos = new HashMap<>();
        double total = 0;

        try {
            FacturaDAO dao = sistema.getFacturaDAO();
            List<String[]> filas = dao.ventasRango(desde, hasta);

            // rellenar mapas por hora y metodos de pago
            for (String[] f : filas) {
                String hora = f[1];
                String metodo = f[5];
                String totalStr = f[4].replaceAll("[,\\.]", "");
                double t = 0;
                try {
                    t = Double.parseDouble(totalStr);
                } catch (Exception ignored) {
                }
                porHora.put(hora, porHora.getOrDefault(hora, 0.0) + t);
                pagos.put(metodo, pagos.getOrDefault(metodo, 0) + 1);
                total += t;
            }

            // ejemplo secundario: porcentaje de pagos en efectivo vs tarjeta
            int efectivo = pagos.getOrDefault("EFECTIVO", 0);
            int tarjeta = pagos.values().stream().mapToInt(Integer::intValue).sum() - efectivo;

            // actualizar tarjetas (formato COP sin decimales)
            NumberFormat nf = defaultFormat;
            ((JLabel) cardTotal.getClientProperty("valor")).setText(nf.format(total));
            ((JLabel) cardFacturas.getClientProperty("valor")).setText(String.valueOf(filas.size()));

        } catch (Exception ex) {
            JPanel aviso = new JPanel(new BorderLayout());
            aviso.setOpaque(false);
            aviso.add(new JLabel("No se pudieron cargar datos: " + ex.getMessage()), BorderLayout.CENTER);
            add(aviso, BorderLayout.SOUTH);
        }

        // Reintroducimos gráficos: barras en el centro y un pastel en el lado derecho
        DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
        DefaultPieDataset<String> pieDataset = new DefaultPieDataset<>();
        porHora.keySet().stream().sorted().forEach(h -> barDataset.addValue(porHora.get(h), "Ventas", h));
        pagos.forEach((metodo, cantidad) -> pieDataset.setValue(metodo, cantidad));

        JFreeChart chartBar = ChartFactory.createBarChart("Ventas por Hora", "Hora", "Total",
                barDataset, PlotOrientation.VERTICAL, false, true, false);
        CategoryPlot categoryPlot = chartBar.getCategoryPlot();
        categoryPlot.setBackgroundPaint(Color.WHITE);
        categoryPlot.setRangeGridlinePaint(new Color(220, 220, 220));
        BarRenderer renderer = new BarRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setSeriesPaint(0, new Color(30, 120, 220));
        renderer.setShadowVisible(false);
        categoryPlot.setRenderer(renderer);
        try {
            NumberAxis rangeAxis = (NumberAxis) categoryPlot.getRangeAxis();
            rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            rangeAxis
                    .setNumberFormatOverride(java.text.NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO")));
        } catch (Exception ignored) {
        }
        ChartPanel cpBar = new ChartPanel(chartBar);
        cpBar.setPreferredSize(new Dimension(1, 420));
        cpBar.setBackground(Color.WHITE);
        cpBar.setMouseWheelEnabled(false);
        cpBar.setPopupMenu(null);

        JFreeChart chartPie = ChartFactory.createPieChart("Métodos de Pago", pieDataset, true, true, false);
        @SuppressWarnings("unchecked")
        PiePlot<String> piePlot = (PiePlot<String>) chartPie.getPlot();
        piePlot.setSimpleLabels(true);
        piePlot.setBackgroundPaint(Color.WHITE);
        piePlot.setOutlineVisible(false);
        piePlot.setLabelBackgroundPaint(new Color(255, 255, 255, 200));
        piePlot.setSectionPaint("EFECTIVO", new Color(30, 120, 220));
        piePlot.setSectionPaint("TARJETA", new Color(200, 60, 80));
        piePlot.setSectionPaint("TARJETA/Otros", new Color(200, 60, 80));
        piePlot.setSectionPaint("CHEQUE", new Color(180, 60, 80));
        piePlot.setSectionPaint("OTROS", new Color(160, 80, 100));
        piePlot.setNoDataMessage("Sin datos de pagos");
        ChartPanel cpPie = new ChartPanel(chartPie);
        cpPie.setPreferredSize(new Dimension(1, 420));
        cpPie.setMouseWheelEnabled(false);
        cpPie.setPopupMenu(null);

        JPanel charts = new JPanel(new GridLayout(1, 2, 16, 0));
        charts.setOpaque(false);
        charts.add(cpBar);
        charts.add(cpPie);

        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.fill = GridBagConstraints.BOTH;
        main.add(charts, c);

        add(main, BorderLayout.CENTER);
    }

    public void refresh() {
        if (actualizando)
            return;
        actualizando = true;
        SwingUtilities.invokeLater(() -> {
            removeAll();
            inicializarUI();
            revalidate();
            repaint();
            actualizando = false;
        });
    }

    private JPanel tarjetaResumen(String titulo, String valor, Color color) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        p.setPreferredSize(new Dimension(240, 80));

        JLabel lTitulo = new JLabel(titulo);
        lTitulo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lTitulo.setForeground(new Color(80, 80, 80));

        JLabel lValor = new JLabel(valor, SwingConstants.RIGHT);
        lValor.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lValor.setForeground(color);

        p.add(lTitulo, BorderLayout.NORTH);
        p.add(lValor, BorderLayout.CENTER);

        p.putClientProperty("valor", lValor);
        p.setOpaque(true);
        return p;
    }
}
