package supermercado.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import supermercado.dao.FacturaDAO;
import supermercado.modelo.EstadoFactura;
import supermercado.servicio.SistemaFacturacion;

public class BuscarFacturaFrame extends JFrame {

    private final SistemaFacturacion sistema = SistemaFacturacion.getInstance();
    private final FacturaDAO dao = sistema.getFacturaDAO();
    private final boolean esAdmin = sistema.getCajeroActivo().esAdmin();

    // Tabla principal
    private DefaultTableModel modeloFacturas;
    private JTable tablaFacturas;
    private TableRowSorter<DefaultTableModel> sorter;

    // Tabla items
    private DefaultTableModel modeloItems;
    private JTable tablaItems;

    // Filtros
    private JTextField txtFiltro;
    private JComboBox<String> cmbEstado;

    private JPanel rootPanel;

    // Botones de accion (siempre visibles)
    private JButton btnVerPDF;
    private JButton btnVerTermica;
    private JButton btnAnular;
    private JButton btnCambiarEstado;

    // Labels de detalle
    private JLabel lblNum, lblFecha, lblHora, lblCliente, lblCajero, lblTotal, lblEstadoDet;

    // Fila actualmente seleccionada
    private String[] filaActual = null;

    public BuscarFacturaFrame() {
        setTitle("Historial de Facturas");
        setSize(1600, 760);
        setMinimumSize(new Dimension(1100, 760));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setIconImage(AppIcon.getIcon());
        construirUI();
        setLocationRelativeTo(null);
        cargarFacturas();
    }

    // =========================================================
    // UI PRINCIPAL
    // =========================================================
    private void construirUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(Color.WHITE);
        this.rootPanel = root;

        root.add(construirBarra(), BorderLayout.NORTH);
        root.add(construirCentro(), BorderLayout.CENTER);
        root.add(construirDetalle(), BorderLayout.SOUTH);

        add(root);

    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    // =========================================================
    // BARRA SUPERIOR: filtros + botones de accion
    // =========================================================

    private JPanel construirBarra() {
        JPanel barra = new JPanel();
        barra.setLayout(new BoxLayout(barra, BoxLayout.Y_AXIS));
        barra.setBackground(new Color(34, 85, 153));
        barra.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        // ---- FILA 1: Titulo + filtros ----
        JPanel fila1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        fila1.setOpaque(false);

        JLabel titulo = new JLabel("Historial de Facturas");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titulo.setForeground(Color.WHITE);

        JLabel lbF = new JLabel("Buscar:");
        lbF.setForeground(Color.WHITE);
        lbF.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        txtFiltro = new JTextField(14);
        UIUtils.estilizarCampo(txtFiltro);
        txtFiltro.setToolTipText("Filtrar por N° factura, cliente o NIT...");
        txtFiltro.setPreferredSize(new Dimension(180, 28));

        JLabel lbE = new JLabel("Estado:");
        lbE.setForeground(Color.WHITE);
        lbE.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        cmbEstado = new JComboBox<>(new String[] { "TODOS", "PAGADA", "PENDIENTE", "ANULADA" });
        cmbEstado.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbEstado.setPreferredSize(new Dimension(120, 28));

        JButton btnRecargar = new JButton("Recargar");
        UIUtils.estilizarBoton(btnRecargar, new Color(70, 70, 130));
        btnRecargar.setPreferredSize(new Dimension(120, 30));
        btnRecargar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRecargar.setToolTipText("Recargar lista de facturas");

        fila1.add(titulo);
        fila1.add(Box.createHorizontalStrut(14));
        fila1.add(lbF);
        fila1.add(txtFiltro);
        fila1.add(lbE);
        fila1.add(cmbEstado);
        fila1.add(btnRecargar);

        // ---- FILA 2: Botones de accion ----
        JPanel fila2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        fila2.setOpaque(false);

        btnVerPDF = new JButton("Ver Factura PDF");
        btnVerTermica = new JButton("Ver Ticket Termico");
        btnAnular = new JButton("Anular Factura");
        btnCambiarEstado = new JButton("Cambiar Estado");

        UIUtils.estilizarBoton(btnVerPDF, new Color(30, 100, 180));
        UIUtils.estilizarBoton(btnVerTermica, new Color(80, 130, 80));
        UIUtils.estilizarBoton(btnAnular, new Color(180, 40, 40));
        UIUtils.estilizarBoton(btnCambiarEstado, new Color(160, 100, 20));

        Dimension dimBtn = new Dimension(170, 32);
        btnVerPDF.setPreferredSize(dimBtn);
        btnVerTermica.setPreferredSize(dimBtn);
        btnAnular.setPreferredSize(dimBtn);
        btnCambiarEstado.setPreferredSize(dimBtn);

        btnVerPDF.setEnabled(false);
        btnVerTermica.setEnabled(false);
        btnAnular.setEnabled(false);
        btnCambiarEstado.setEnabled(false);

        fila2.add(btnVerPDF);
        fila2.add(btnVerTermica);
        if (esAdmin) {
            fila2.add(btnCambiarEstado);
            fila2.add(btnAnular);
        }

        barra.add(fila1);
        barra.add(fila2);

        // Acciones filtros
        txtFiltro.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                aplicarFiltros();
            }
        });
        cmbEstado.addActionListener(e -> aplicarFiltros());
        btnRecargar.addActionListener(e -> cargarFacturas());

        // Acciones botones
        btnVerPDF.addActionListener(e -> abrirPDF(false));
        btnVerTermica.addActionListener(e -> abrirPDF(true));
        btnAnular.addActionListener(e -> anularSeleccionada());
        btnCambiarEstado.addActionListener(e -> cambiarEstado());

        return barra;
    }

    // =========================================================
    // CENTRO: tabla facturas + tabla items (SplitPane)
    // =========================================================
    private JSplitPane construirCentro() {
        // --- Tabla facturas ---
        modeloFacturas = new DefaultTableModel(
                new String[] { "N° Factura", "Fecha", "Hora", "Cliente",
                        "NIT", "Cajero", "Subtotal", "IVA", "Total",
                        "Metodo pago", "Estado" },
                0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaFacturas = new JTable(modeloFacturas);
        tablaFacturas.setRowHeight(26);
        tablaFacturas.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tablaFacturas.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tablaFacturas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        int[] anchos = { 95, 85, 55, 160, 95, 120, 85, 70, 95, 130, 85 };
        for (int i = 0; i < anchos.length; i++)
            tablaFacturas.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);

        // Colores por estado
        tablaFacturas.setDefaultRenderer(Object.class, (t, val, sel, foc, row, col) -> {
            JLabel lbl = new JLabel(val == null ? "" : val.toString());
            lbl.setOpaque(true);
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            String estado = modeloFacturas.getValueAt(row, 10).toString();
            if (sel) {
                lbl.setBackground(new Color(184, 207, 229));
                lbl.setForeground(Color.BLACK);
            } else if ("ANULADA".equals(estado)) {
                lbl.setBackground(new Color(255, 215, 215));
                lbl.setForeground(new Color(140, 30, 30));
            } else if ("PENDIENTE".equals(estado)) {
                lbl.setBackground(new Color(255, 248, 210));
                lbl.setForeground(new Color(120, 80, 0));
            } else {
                lbl.setBackground(row % 2 == 0 ? new Color(242, 250, 242) : Color.WHITE);
                lbl.setForeground(Color.BLACK);
            }
            if (col == 8)
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
            if (col == 10) {
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));
            }
            return lbl;
        });

        sorter = new TableRowSorter<>(modeloFacturas);
        tablaFacturas.setRowSorter(sorter);

        JScrollPane scrollFact = new JScrollPane(tablaFacturas);
        scrollFact.setBorder(BorderFactory.createTitledBorder("  Todas las facturas  "));

        // --- Tabla items ---
        modeloItems = new DefaultTableModel(
                new String[] { "Producto", "Cant.", "Precio unit.", "Subtotal", "IVA", "Total" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaItems = new JTable(modeloItems);
        tablaItems.setRowHeight(24);
        tablaItems.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tablaItems.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        tablaItems.getColumnModel().getColumn(0).setPreferredWidth(280);

        JScrollPane scrollItems = new JScrollPane(tablaItems);
        scrollItems.setBorder(BorderFactory.createTitledBorder("  Productos de la factura seleccionada  "));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollFact, scrollItems);
        split.setResizeWeight(0.62);
        split.setDividerSize(5);

        // Listener de seleccion
        tablaFacturas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                onSeleccionFila();
        });

        return split;
    }

    // =========================================================
    // PANEL DETALLE INFERIOR (siempre visible, altura fija)
    // =========================================================
    private JPanel construirDetalle() {
        JPanel panel = new JPanel(new GridLayout(1, 7, 8, 0));
        panel.setBackground(new Color(235, 242, 255));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(160, 190, 230)),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        panel.setPreferredSize(new Dimension(0, 64));

        lblNum = infoLabel("—");
        lblFecha = infoLabel("—");
        lblHora = infoLabel("—");
        lblCliente = infoLabel("—");
        lblCajero = infoLabel("—");
        lblTotal = infoLabel("—");
        lblEstadoDet = infoLabel("—");

        panel.add(bloqueDet("N° Factura", lblNum));
        panel.add(bloqueDet("Fecha", lblFecha));
        panel.add(bloqueDet("Hora", lblHora));
        panel.add(bloqueDet("Cliente", lblCliente));
        panel.add(bloqueDet("Cajero", lblCajero));
        panel.add(bloqueDet("Total", lblTotal));
        panel.add(bloqueDet("Estado", lblEstadoDet));

        return panel;
    }

    // =========================================================
    // DATOS
    // =========================================================
    private void cargarFacturas() {
        modeloFacturas.setRowCount(0);
        modeloItems.setRowCount(0);
        limpiarDetalle();
        try {
            for (String[] f : dao.listarTodas())
                modeloFacturas.addRow(f);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al cargar facturas: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aplicarFiltros() {
        String txt = txtFiltro.getText().trim();
        String estado = (String) cmbEstado.getSelectedItem();

        RowFilter<DefaultTableModel, Object> fTxt = txt.isEmpty() ? null : RowFilter.regexFilter("(?i)" + txt, 0, 3, 4);
        RowFilter<DefaultTableModel, Object> fEst = "TODOS".equals(estado) ? null
                : RowFilter.regexFilter("^" + estado + "$", 10);

        if (fTxt == null && fEst == null)
            sorter.setRowFilter(null);
        else if (fTxt == null)
            sorter.setRowFilter(fEst);
        else if (fEst == null)
            sorter.setRowFilter(fTxt);
        else
            sorter.setRowFilter(RowFilter.andFilter(Arrays.asList(fTxt, fEst)));
    }

    private void onSeleccionFila() {
        int vista = tablaFacturas.getSelectedRow();
        if (vista < 0) {
            limpiarDetalle();
            return;
        }

        int modelo = tablaFacturas.convertRowIndexToModel(vista);
        filaActual = new String[modeloFacturas.getColumnCount()];
        for (int c = 0; c < filaActual.length; c++)
            filaActual[c] = String.valueOf(modeloFacturas.getValueAt(modelo, c));

        // Actualizar labels de detalle
        lblNum.setText(filaActual[0]);
        lblFecha.setText(filaActual[1]);
        lblHora.setText(filaActual[2]);
        lblCliente.setText(filaActual[3]);
        lblCajero.setText(filaActual[5]);
        lblTotal.setText(filaActual[8]);

        String estado = filaActual[10];
        lblEstadoDet.setText(estado);
        lblEstadoDet.setForeground(colorEstado(estado));

        // Habilitar botones
        boolean esAnulada = "ANULADA".equals(estado);

        btnVerPDF.setEnabled(true);
        btnVerTermica.setEnabled(true);
        // Anular: cualquier cajero puede anular, pero solo facturas PAGADAS o
        // PENDIENTES
        btnAnular.setEnabled(esAdmin && !esAnulada);
        // Cambiar estado: solo ADMIN
        btnCambiarEstado.setEnabled(esAdmin && !esAnulada);

        // Cargar items
        modeloItems.setRowCount(0);
        try {
            for (String[] it : dao.itemsDe(filaActual[0]))
                modeloItems.addRow(it);
        } catch (Exception ex) {
            modeloItems.addRow(new String[] { "Error: " + ex.getMessage(), "", "", "", "", "" });
        }
    }

    // =========================================================
    // ACCIONES BOTONES
    // =========================================================
    private void abrirPDF(boolean termica) {
        if (filaActual == null)
            return;
        String numero = filaActual[0];
        String prefijo = termica ? "TERMICA_" : "";
        String ruta = "reportes/facturas/" + prefijo + numero + ".pdf";
        File f = new File(ruta);

        if (f.exists()) {
            try {
                Desktop.getDesktop().open(f);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "No se pudo abrir el PDF: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "El archivo PDF no existe en disco.\n\n" +
                            "Ruta buscada:\n" + f.getAbsolutePath() + "\n\n" +
                            "Los PDFs se generan al momento de realizar la venta.\n" +
                            "Facturas antiguas pueden no tener PDF guardado.",
                    "PDF no encontrado", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void anularSeleccionada() {
        if (filaActual == null)
            return;
        String numero = filaActual[0];
        String estado = filaActual[10];

        if ("ANULADA".equals(estado)) {
            JOptionPane.showMessageDialog(this,
                    "Esta factura ya fue anulada.", "Aviso",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int r = JOptionPane.showConfirmDialog(this,
                "¿Confirmar anulacion de la factura " + numero + "?\n\n" +
                        "Cliente : " + filaActual[3] + "\n" +
                        "Total   : " + filaActual[8] + "\n" +
                        "Estado  : " + estado + "\n\n" +
                        "Esta accion cambiara el estado a ANULADA.",
                "Anular factura",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (r != JOptionPane.YES_OPTION)
            return;

        try {
            sistema.anularFactura(numero);
            JOptionPane.showMessageDialog(this,
                    "Factura " + numero + " anulada correctamente.",
                    "Anulada", JOptionPane.INFORMATION_MESSAGE);
            cargarFacturas();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al anular: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cambiarEstado() {
        if (filaActual == null)
            return;
        String numero = filaActual[0];
        String estadoActual = filaActual[10];

        // Opciones disponibles segun el estado actual
        String[] opciones;
        if ("PAGADA".equals(estadoActual)) {
            opciones = new String[] { "PENDIENTE", "ANULADA" };
        } else if ("PENDIENTE".equals(estadoActual)) {
            opciones = new String[] { "PAGADA", "ANULADA" };
        } else {
            JOptionPane.showMessageDialog(this,
                    "No se puede cambiar el estado de una factura ANULADA.",
                    "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String nuevoEstado = (String) JOptionPane.showInputDialog(
                this,
                "Factura: " + numero + "\n" +
                        "Estado actual: " + estadoActual + "\n\n" +
                        "Seleccione el nuevo estado:",
                "Cambiar estado",
                JOptionPane.QUESTION_MESSAGE,
                null, opciones, opciones[0]);

        if (nuevoEstado == null)
            return; // cancelado

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Cambiar estado de " + estadoActual + " a " + nuevoEstado + "?\n\n" +
                        "Factura : " + numero + "\n" +
                        "Cliente : " + filaActual[3],
                "Confirmar cambio",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION)
            return;

        try {
            EstadoFactura ef = EstadoFactura.valueOf(nuevoEstado);
            dao.actualizarEstado(numero, ef);
            JOptionPane.showMessageDialog(this,
                    "Estado de " + numero + " cambiado a " + nuevoEstado + ".",
                    "Exito", JOptionPane.INFORMATION_MESSAGE);
            cargarFacturas();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al cambiar estado: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================
    // UTILIDADES
    // =========================================================
    private void limpiarDetalle() {
        filaActual = null;
        lblNum.setText("—");
        lblFecha.setText("—");
        lblHora.setText("—");
        lblCliente.setText("—");
        lblCajero.setText("—");
        lblTotal.setText("—");
        lblEstadoDet.setText("—");
        lblEstadoDet.setForeground(Color.GRAY);
        btnVerPDF.setEnabled(false);
        btnVerTermica.setEnabled(false);
        btnAnular.setEnabled(false);
        btnCambiarEstado.setEnabled(false);
    }

    private Color colorEstado(String estado) {
        return switch (estado) {
            case "PAGADA" -> new Color(30, 120, 50);
            case "ANULADA" -> new Color(180, 30, 30);
            case "PENDIENTE" -> new Color(160, 100, 0);
            default -> Color.GRAY;
        };
    }

    private JLabel infoLabel(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(34, 85, 153));
        return l;
    }

    private JPanel bloqueDet(String titulo, JLabel valor) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        JLabel t = new JLabel(titulo);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        t.setForeground(Color.GRAY);
        p.add(t, BorderLayout.NORTH);
        p.add(valor, BorderLayout.CENTER);
        return p;
    }
}
