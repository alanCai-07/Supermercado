package supermercado.ui;

import supermercado.modelo.EstadoFactura;
import supermercado.reporte.GeneradorReportePDF;
import supermercado.servicio.SistemaFacturacion;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;

public class BuscarFacturaFrame extends JFrame {

    private final SistemaFacturacion sistema = SistemaFacturacion.getInstance();

    // Tabla principal de facturas
    private DefaultTableModel      modeloFacturas;
    private JTable                 tablaFacturas;
    private TableRowSorter<DefaultTableModel> sorter;

    // Tabla de detalle (items de la factura seleccionada)
    private DefaultTableModel      modeloItems;
    private JTable                 tablaItems;

    // Filtros
    private JTextField   txtFiltroNum;
    private JComboBox<String> cmbFiltroEstado;

    // Panel detalle
    private JLabel  lblDetNumero, lblDetFecha, lblDetHora,
                    lblDetCliente, lblDetCajero, lblDetTotal, lblDetEstado;
    private JButton btnAnular, btnPDF;

    // Fila actualmente seleccionada (datos completos)
    private String[] filaSeleccionada = null;

    public BuscarFacturaFrame() {
        setTitle("Historial de Facturas");
        setSize(1100, 680);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        construirUI();
        cargarFacturas();
    }

    // =========================================================
    //  CONSTRUCCION UI
    // =========================================================
    private void construirUI() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        root.setBackground(Color.WHITE);

        root.add(construirPanelFiltros(),  BorderLayout.NORTH);
        root.add(construirPanelCentro(),   BorderLayout.CENTER);
        root.add(construirPanelDetalle(),  BorderLayout.SOUTH);
        add(root);
    }

    // ---- Barra de filtros ----
    private JPanel construirPanelFiltros() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        panel.setBackground(new Color(240, 245, 255));
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                new Color(180, 200, 230)));

        JLabel titulo = new JLabel("Historial de Facturas");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titulo.setForeground(new Color(34, 85, 153));
        panel.add(titulo);

        panel.add(Box.createHorizontalStrut(20));
        panel.add(new JLabel("Buscar N°:"));
        txtFiltroNum = new JTextField(12);
        LoginFrame.estilizarCampo(txtFiltroNum);
        txtFiltroNum.setToolTipText("Filtrar por numero de factura o cliente...");
        panel.add(txtFiltroNum);

        panel.add(new JLabel("Estado:"));
        cmbFiltroEstado = new JComboBox<>(
                new String[]{"TODOS", "PAGADA", "PENDIENTE", "ANULADA"});
        cmbFiltroEstado.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(cmbFiltroEstado);

        JButton btnRecargar = new JButton("Recargar");
        LoginFrame.estilizarBoton(btnRecargar, new Color(80, 80, 80));
        btnRecargar.setPreferredSize(new Dimension(100, 32));
        panel.add(btnRecargar);

        // Acciones filtros
        KeyAdapter filtroKey = new KeyAdapter() {
            public void keyReleased(KeyEvent e) { aplicarFiltros(); }
        };
        txtFiltroNum.addKeyListener(filtroKey);
        cmbFiltroEstado.addActionListener(e -> aplicarFiltros());
        btnRecargar.addActionListener(e -> cargarFacturas());

        return panel;
    }

    // ---- Tabla principal + tabla de items ----
    private JPanel construirPanelCentro() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(Color.WHITE);

        // --- Tabla de facturas ---
        modeloFacturas = new DefaultTableModel(
            new String[]{"N° Factura", "Fecha", "Hora", "Cliente",
                         "NIT", "Cajero", "Subtotal", "IVA", "Total",
                         "Metodo pago", "Estado"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaFacturas = new JTable(modeloFacturas);
        tablaFacturas.setRowHeight(26);
        tablaFacturas.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tablaFacturas.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tablaFacturas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Anchos de columna
        int[] anchos = {90, 85, 55, 160, 90, 120, 85, 70, 90, 110, 80};
        for (int i = 0; i < anchos.length; i++)
            tablaFacturas.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);

        // Renderer con colores por estado
        tablaFacturas.setDefaultRenderer(Object.class, (t, val, sel, foc, row, col) -> {
            JLabel lbl = new JLabel(val == null ? "" : val.toString());
            lbl.setOpaque(true);
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            String estado = modeloFacturas.getValueAt(row, 10).toString();
            if (sel) {
                lbl.setBackground(new Color(184, 207, 229));
            } else if ("ANULADA".equals(estado)) {
                lbl.setBackground(new Color(255, 220, 220));
                lbl.setForeground(new Color(140, 30, 30));
            } else if ("PAGADA".equals(estado)) {
                lbl.setBackground(row % 2 == 0 ? new Color(240, 255, 240) : Color.WHITE);
            } else { // PENDIENTE
                lbl.setBackground(new Color(255, 250, 220));
            }
            // Columna total en negrita
            if (col == 8) lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
            return lbl;
        });

        sorter = new TableRowSorter<>(modeloFacturas);
        tablaFacturas.setRowSorter(sorter);

        JScrollPane scrollFacturas = new JScrollPane(tablaFacturas);
        scrollFacturas.setBorder(BorderFactory.createTitledBorder("Todas las facturas"));
        scrollFacturas.setPreferredSize(new Dimension(0, 260));

        // --- Tabla de items del detalle ---
        modeloItems = new DefaultTableModel(
            new String[]{"Producto", "Cant.", "Precio unit.", "Subtotal", "IVA", "Total"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaItems = new JTable(modeloItems);
        tablaItems.setRowHeight(24);
        tablaItems.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tablaItems.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        tablaItems.getColumnModel().getColumn(0).setPreferredWidth(260);

        JScrollPane scrollItems = new JScrollPane(tablaItems);
        scrollItems.setBorder(BorderFactory.createTitledBorder(
                "Productos de la factura seleccionada"));
        scrollItems.setPreferredSize(new Dimension(0, 150));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                scrollFacturas, scrollItems);
        split.setResizeWeight(0.6);
        split.setDividerSize(6);
        split.setBorder(null);
        panel.add(split, BorderLayout.CENTER);

        // Al seleccionar una fila cargar sus items y actualizar el detalle
        tablaFacturas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onSeleccionFila();
        });

        return panel;
    }

    // ---- Panel inferior: info detalle + botones ----
    private JPanel construirPanelDetalle() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(new Color(245, 248, 255));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(180, 200, 230)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        // Info en grid
        JPanel info = new JPanel(new GridLayout(2, 6, 8, 4));
        info.setBackground(new Color(245, 248, 255));

        lblDetNumero  = etiquetaInfo("—");
        lblDetFecha   = etiquetaInfo("—");
        lblDetHora    = etiquetaInfo("—");
        lblDetCliente = etiquetaInfo("—");
        lblDetCajero  = etiquetaInfo("—");
        lblDetTotal   = etiquetaInfo("—");
        lblDetEstado  = etiquetaInfo("—");

        info.add(labelTitulo("N° Factura:"));   info.add(lblDetNumero);
        info.add(labelTitulo("Fecha:"));        info.add(lblDetFecha);
        info.add(labelTitulo("Hora:"));         info.add(lblDetHora);
        info.add(labelTitulo("Cliente:"));      info.add(lblDetCliente);
        info.add(labelTitulo("Cajero:"));       info.add(lblDetCajero);
        info.add(labelTitulo("Total:"));        info.add(lblDetTotal);

        panel.add(info, BorderLayout.CENTER);

        // Botones de accion
        JPanel botones = new JPanel(new GridLayout(3, 1, 0, 6));
        botones.setBackground(new Color(245, 248, 255));
        botones.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        btnPDF    = new JButton("Ver / PDF");
        btnAnular = new JButton("Anular factura");
        JButton btnEstado = new JButton("Estado: " +
                sistema.getCajeroActivo().getRol());

        LoginFrame.estilizarBoton(btnPDF,    new Color(34, 85, 153));
        LoginFrame.estilizarBoton(btnAnular, new Color(160, 40, 40));
        LoginFrame.estilizarBoton(btnEstado, new Color(80, 80, 80));

        btnPDF   .setEnabled(false);
        btnAnular.setEnabled(false);

        botones.add(btnPDF);
        botones.add(btnAnular);
        botones.add(btnEstado);
        panel.add(botones, BorderLayout.EAST);

        // Acciones
        btnPDF.addActionListener(e -> verPDF());
        btnAnular.addActionListener(e -> anularSeleccionada());
        btnEstado.setEnabled(false);

        return panel;
    }

    // =========================================================
    //  LOGICA DE DATOS
    // =========================================================
    private void cargarFacturas() {
        modeloFacturas.setRowCount(0);
        modeloItems.setRowCount(0);
        limpiarDetalle();
        try {
            List<String[]> filas = sistema.getFacturaDAO().listarTodas();
            for (String[] f : filas)
                modeloFacturas.addRow(f);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al cargar facturas: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aplicarFiltros() {
        String texto = txtFiltroNum.getText().trim();
        String estado = (String) cmbFiltroEstado.getSelectedItem();

        RowFilter<DefaultTableModel, Object> filtroTexto = texto.isEmpty() ? null :
                RowFilter.regexFilter("(?i)" + texto, 0, 3, 4); // col N°, cliente, NIT

        RowFilter<DefaultTableModel, Object> filtroEstado =
                "TODOS".equals(estado) ? null :
                RowFilter.regexFilter(estado, 10); // col estado

        if (filtroTexto == null && filtroEstado == null) {
            sorter.setRowFilter(null);
        } else if (filtroTexto == null) {
            sorter.setRowFilter(filtroEstado);
        } else if (filtroEstado == null) {
            sorter.setRowFilter(filtroTexto);
        } else {
            sorter.setRowFilter(RowFilter.andFilter(
                    java.util.Arrays.asList(filtroTexto, filtroEstado)));
        }
    }

    private void onSeleccionFila() {
        int filaVista = tablaFacturas.getSelectedRow();
        if (filaVista < 0) {
            limpiarDetalle();
            return;
        }
        int fila = tablaFacturas.convertRowIndexToModel(filaVista);
        filaSeleccionada = new String[modeloFacturas.getColumnCount()];
        for (int c = 0; c < filaSeleccionada.length; c++)
            filaSeleccionada[c] = modeloFacturas.getValueAt(fila, c).toString();

        // Llenar panel detalle
        lblDetNumero .setText(filaSeleccionada[0]);
        lblDetFecha  .setText(filaSeleccionada[1]);
        lblDetHora   .setText(filaSeleccionada[2]);
        lblDetCliente.setText(filaSeleccionada[3] + "  (NIT: " + filaSeleccionada[4] + ")");
        lblDetCajero .setText(filaSeleccionada[5]);
        lblDetTotal  .setText(filaSeleccionada[8]);

        boolean esPagada  = "PAGADA" .equals(filaSeleccionada[10]);
        boolean esAnulada = "ANULADA".equals(filaSeleccionada[10]);

        btnPDF   .setEnabled(true);
        // Solo ADMIN puede anular; solo facturas PAGADAS se pueden anular
        btnAnular.setEnabled(esPagada && sistema.getCajeroActivo().esAdmin());

        // Cargar items de esta factura
        cargarItems(filaSeleccionada[0]);
    }

    private void cargarItems(String numeroFactura) {
        modeloItems.setRowCount(0);
        try {
            List<String[]> items = sistema.getFacturaDAO().itemsDe(numeroFactura);
            for (String[] it : items)
                modeloItems.addRow(it);
        } catch (Exception ex) {
            modeloItems.addRow(new String[]{"Error al cargar items: " + ex.getMessage(),
                    "", "", "", "", ""});
        }
    }

    private void verPDF() {
        if (filaSeleccionada == null) return;
        try {
            String ruta = "reportes/facturas/" + filaSeleccionada[0] + ".pdf";
            File f = new File(ruta);
            if (f.exists()) {
                Desktop.getDesktop().open(f);
            } else {
                JOptionPane.showMessageDialog(this,
                        "El PDF no existe en disco.\nRuta buscada: " + ruta +
                        "\n\nSolo se pueden regenerar facturas que aun esten\n" +
                        "cargadas en la sesion actual.",
                        "PDF no encontrado", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al abrir PDF: " + ex.getMessage());
        }
    }

    private void anularSeleccionada() {
        if (filaSeleccionada == null) return;
        String numero = filaSeleccionada[0];

        int r = JOptionPane.showConfirmDialog(this,
                "¿Confirma anular la factura " + numero + "?\n\n" +
                "Cliente : " + filaSeleccionada[3] + "\n" +
                "Total   : " + filaSeleccionada[8] + "\n\n" +
                "Esta accion no se puede deshacer.",
                "Anular factura", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (r != JOptionPane.YES_OPTION) return;

        try {
            sistema.anularFactura(numero);
            JOptionPane.showMessageDialog(this,
                    "Factura " + numero + " anulada correctamente.",
                    "Anulada", JOptionPane.INFORMATION_MESSAGE);
            cargarFacturas(); // recargar la lista completa
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al anular: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================
    //  UTILIDADES UI
    // =========================================================
    private void limpiarDetalle() {
        filaSeleccionada = null;
        lblDetNumero .setText("—");
        lblDetFecha  .setText("—");
        lblDetHora   .setText("—");
        lblDetCliente.setText("—");
        lblDetCajero .setText("—");
        lblDetTotal  .setText("—");
        if (lblDetEstado != null) lblDetEstado.setText("—");
        btnPDF   .setEnabled(false);
        btnAnular.setEnabled(false);
    }

    private JLabel etiquetaInfo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(34, 85, 153));
        return l;
    }

    private JLabel labelTitulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(Color.GRAY);
        return l;
    }
}
