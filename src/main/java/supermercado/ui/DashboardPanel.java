package supermercado.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import javax.swing.Box;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import supermercado.servicio.SistemaFacturacion;
import supermercado.dao.FacturaDAO;

public class DashboardPanel extends JPanel {

    private final SistemaFacturacion sistema = SistemaFacturacion.getInstance();

    public DashboardPanel() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setBackground(new Color(245, 247, 250));

        JLabel titulo = new JLabel("Dashboard", SwingConstants.LEFT);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titulo.setBorder(BorderFactory.createEmptyBorder(4, 8, 10, 8));
        add(titulo, BorderLayout.NORTH);

        JPanel main = new JPanel(new GridBagLayout());
        main.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);

        // Tarjetas superiores: resumen rapido
        JPanel tarjetas = new JPanel();
        tarjetas.setLayout(new BoxLayout(tarjetas, BoxLayout.X_AXIS));
        tarjetas.setOpaque(false);

        JPanel cardTotal = tarjetaResumen("Total Ventas (hoy)", "$0", new Color(60, 130, 200));
        JPanel cardFacturas = tarjetaResumen("Facturas (hoy)", "0", new Color(80, 200, 150));
        tarjetas.add(cardTotal);
        tarjetas.add(cardFacturas);

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        main.add(tarjetas, c);

        // Area central: grafico de barras
        DefaultCategoryDataset bar = new DefaultCategoryDataset();
        // Area derecha: indicadores/gráficos circulares
        DefaultPieDataset<String> pie1 = new DefaultPieDataset<String>();
        DefaultPieDataset<String> pie2 = new DefaultPieDataset<String>();

        try {
            FacturaDAO dao = sistema.getFacturaDAO();
            LocalDate hoy = LocalDate.now();
            List<String[]> filas = dao.ventasDia(hoy);

            // rellenar dataset por hora y metodos de pago y top estados
            Map<String, Double> porHora = new HashMap<>();
            Map<String, Integer> pagos = new HashMap<>();
            double total = 0;
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

            porHora.keySet().stream().sorted().forEach(h -> bar.addValue(porHora.get(h), "Ventas", h));
            pagos.forEach((metodo, cantidad) -> pie1.setValue(metodo, cantidad));

            // ejemplo secundario: porcentaje de pagos en efectivo vs tarjeta
            int efectivo = pagos.getOrDefault("EFECTIVO", 0);
            int tarjeta = pagos.values().stream().mapToInt(Integer::intValue).sum() - efectivo;
            pie2.setValue("Efectivo", efectivo);
            pie2.setValue("Tarjeta/Otros", Math.max(0, tarjeta));

            // actualizar tarjetas
            NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES"));
            ((JLabel) cardTotal.getClientProperty("valor")).setText(nf.format(total));
            ((JLabel) cardFacturas.getClientProperty("valor")).setText(String.valueOf(filas.size()));

        } catch (Exception ex) {
            // Si falla BD, dejar datasets vacíos y mostrar mensaje pequeño
            JPanel aviso = new JPanel(new BorderLayout());
            aviso.setOpaque(false);
            aviso.add(new JLabel("No se pudieron cargar datos: " + ex.getMessage()), BorderLayout.CENTER);
        }

        JFreeChart chartBar = ChartFactory.createBarChart("Ventas por Hora", "Hora", "Total",
                bar, PlotOrientation.VERTICAL, false, true, false);
        // estilo azul/white para barras
        org.jfree.chart.plot.CategoryPlot cplot = chartBar.getCategoryPlot();
        cplot.setBackgroundPaint(Color.WHITE);
        cplot.setRangeGridlinePaint(new Color(220, 220, 220));
        BarRenderer renderer = new BarRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setSeriesPaint(0, new Color(30, 120, 220));
        renderer.setShadowVisible(false);
        cplot.setRenderer(renderer);
        ChartPanel cpBar = new ChartPanel(chartBar);
        cpBar.setPreferredSize(new Dimension(900, 420));

        JFreeChart chartPie1 = ChartFactory.createPieChart("Metodos de Pago", pie1, true, true, false);
        PiePlot<?> plot1 = (PiePlot<?>) chartPie1.getPlot();
        plot1.setSimpleLabels(true);
        ChartPanel cpPie1 = new ChartPanel(chartPie1);
        cpPie1.setPreferredSize(new Dimension(300, 240));

        JFreeChart chartPie2 = ChartFactory.createPieChart("Efectivo vs Tarjeta", pie2, true, true, false);
        PiePlot<?> plot2 = (PiePlot<?>) chartPie2.getPlot();
        plot2.setSimpleLabels(true);
        ChartPanel cpPie2 = new ChartPanel(chartPie2);
        cpPie2.setPreferredSize(new Dimension(300, 240));

        // agregar grafico central
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0.75;
        c.weighty = 1.0;
        main.add(cpBar, c);

        // panel derecho con dos indicadores
        JPanel derecho = new JPanel();
        derecho.setLayout(new BoxLayout(derecho, BoxLayout.Y_AXIS));
        derecho.setOpaque(false);
        derecho.setPreferredSize(new Dimension(320, 420));
        derecho.add(cpPie1);
        derecho.add(Box.createVerticalStrut(12));
        derecho.add(cpPie2);

        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 0.25;
        c.fill = GridBagConstraints.VERTICAL;
        main.add(derecho, c);

        add(main, BorderLayout.CENTER);
    }

    private JPanel tarjetaResumen(String titulo, String valor, Color color) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        p.setPreferredSize(new Dimension(240, 80));

        JLabel lTitulo = new JLabel(titulo);
        lTitulo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lTitulo.setForeground(Color.DARK_GRAY);

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
