package supermercado.ui;

import supermercado.dao.ClienteDAO;
import supermercado.modelo.*;
import supermercado.pago.*;
import supermercado.reporte.GeneradorReportePDF;
import supermercado.servicio.SistemaFacturacion;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class NuevaVentaFrame extends JFrame {

    private final JFrame              parent;
    private final SistemaFacturacion  sistema = SistemaFacturacion.getInstance();

    private Factura  facturaActual;
    private Cliente  clienteActual;

    // Lista interna de items (fuente de verdad para el carrito en pantalla)
    // Solo se confirma en BD cuando se presiona COBRAR
    private final List<ItemFactura> itemsCarrito = new ArrayList<>();

    // Componentes
    private JTextField             txtBuscarProducto;
    private JTable                 tablaProductos;
    private DefaultTableModel      modeloProductos;
    private JTable                 tablaItems;
    private DefaultTableModel      modeloItems;
    private JLabel                 lblSubtotal, lblIva, lblTotal;
    private JComboBox<String>      cmbPago;
    private JTextField             txtMontoPago;
    private JTextField             txtCant;   // campo cantidad — referencia de clase
    private JButton                btnCobrar;

    public NuevaVentaFrame(JFrame parent) {
        this.parent = parent;
        setTitle("Nueva Venta");
        setSize(1000, 680);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(parent);
        iniciarFactura();
        construirUI();
    }

    private void iniciarFactura() {
        try {
            clienteActual = new ClienteDAO().consumidorFinal();
            facturaActual = sistema.crearFactura(clienteActual);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al crear factura: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================
    //  CONSTRUCCION UI
    // =========================================================
    private void construirUI() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.setBackground(Color.WHITE);

        root.add(construirPanelProductos(), BorderLayout.WEST);
        root.add(construirPanelCarrito(),   BorderLayout.CENTER);
        add(root);

        buscarProductos("");
    }

    // ---- Panel izquierdo: catalogo de productos ----
    private JPanel construirPanelProductos() {
        JPanel panel = new JPanel(new BorderLayout(4, 6));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Productos"));
        panel.setPreferredSize(new Dimension(440, 0));

        // Barra de busqueda
        JPanel barraBusq = new JPanel(new BorderLayout(4, 0));
        barraBusq.setBackground(Color.WHITE);
        txtBuscarProducto = new JTextField();
        LoginFrame.estilizarCampo(txtBuscarProducto);
        JButton btnBuscar = new JButton("Buscar");
        LoginFrame.estilizarBoton(btnBuscar, new Color(34, 85, 153));
        barraBusq.add(new JLabel("Buscar: "), BorderLayout.WEST);
        barraBusq.add(txtBuscarProducto,      BorderLayout.CENTER);
        barraBusq.add(btnBuscar,              BorderLayout.EAST);
        panel.add(barraBusq, BorderLayout.NORTH);

        // Tabla de productos (NO editable)
        modeloProductos = new DefaultTableModel(
                new String[]{"Codigo", "Nombre", "Precio", "IVA%", "Stock"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaProductos = new JTable(modeloProductos);
        tablaProductos.setRowHeight(24);
        tablaProductos.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tablaProductos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaProductos.getColumnModel().getColumn(0).setPreferredWidth(55);
        tablaProductos.getColumnModel().getColumn(1).setPreferredWidth(200);
        tablaProductos.getColumnModel().getColumn(2).setPreferredWidth(65);
        tablaProductos.getColumnModel().getColumn(3).setPreferredWidth(45);
        tablaProductos.getColumnModel().getColumn(4).setPreferredWidth(45);

        // FIX #1: al seleccionar fila NO se limpia txtCant — no hay listener en la seleccion
        panel.add(new JScrollPane(tablaProductos), BorderLayout.CENTER);

        // Panel cantidad + boton agregar
        JPanel panelAgregar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        panelAgregar.setBackground(Color.WHITE);

        // FIX #1: txtCant es campo de clase para evitar que se pierda la referencia
        txtCant = new JTextField("1", 6);
        LoginFrame.estilizarCampo(txtCant);
        txtCant.setHorizontalAlignment(JTextField.CENTER);
        txtCant.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JButton btnMenos = new JButton("-");
        JButton btnMas   = new JButton("+");
        estilizarBtnCant(btnMenos, new Color(180, 60, 60));
        estilizarBtnCant(btnMas,   new Color(30, 130, 76));

        JButton btnAgregar = new JButton("  Agregar al carrito  ");
        LoginFrame.estilizarBoton(btnAgregar, new Color(30, 130, 76));
        btnAgregar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAgregar.setPreferredSize(new Dimension(180, 34));

        panelAgregar.add(new JLabel("Cantidad:"));
        panelAgregar.add(btnMenos);
        panelAgregar.add(txtCant);
        panelAgregar.add(btnMas);
        panelAgregar.add(Box.createHorizontalStrut(8));
        panelAgregar.add(btnAgregar);
        panel.add(panelAgregar, BorderLayout.SOUTH);

        // ---- ACCIONES panel izquierdo ----
        ActionListener accionBuscar = e -> buscarProductos(txtBuscarProducto.getText());
        btnBuscar.addActionListener(accionBuscar);
        txtBuscarProducto.addActionListener(accionBuscar);

        // Botones +/-
        btnMenos.addActionListener(e -> cambiarCantidad(-1));
        btnMas  .addActionListener(e -> cambiarCantidad(+1));

        // FIX #1: txtCant no pierde valor al hacer Enter — solo agrega si hay producto
        txtCant.addActionListener(e -> btnAgregar.doClick());

        // FIX #2: boton agregar — UN SOLO ActionListener, sin duplicados
        btnAgregar.addActionListener(e -> {
            int fila = tablaProductos.getSelectedRow();
            if (fila < 0) {
                JOptionPane.showMessageDialog(this,
                        "Seleccione un producto de la lista.", "Aviso",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            int cant = leerCantidad();
            if (cant <= 0) {
                JOptionPane.showMessageDialog(this,
                        "La cantidad debe ser mayor a 0.", "Aviso",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            String id = (String) modeloProductos.getValueAt(fila, 0);
            agregarAlCarrito(id, cant);
        });

        // FIX #2: doble clic agrega con la cantidad del campo (no hardcoded 1)
        // y NO dispara el ActionListener del boton — usa agregarAlCarrito directamente
        tablaProductos.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int fila = tablaProductos.getSelectedRow();
                    if (fila >= 0) {
                        String id = (String) modeloProductos.getValueAt(fila, 0);
                        agregarAlCarrito(id, leerCantidad());
                    }
                }
            }
        });

        return panel;
    }

    // ---- Panel derecho: carrito + cobro ----
    private JPanel construirPanelCarrito() {
        JPanel panel = new JPanel(new BorderLayout(4, 6));
        panel.setBackground(Color.WHITE);

        // Carrito
        JPanel panelCarrito = new JPanel(new BorderLayout(4, 4));
        String numFact = facturaActual != null ? facturaActual.getNumero() : "---";
        panelCarrito.setBorder(BorderFactory.createTitledBorder(
                "Carrito  —  N° " + numFact));
        panelCarrito.setBackground(Color.WHITE);

        modeloItems = new DefaultTableModel(
                new String[]{"Producto", "Cant.", "Unitario", "IVA", "Total"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaItems = new JTable(modeloItems);
        tablaItems.setRowHeight(26);
        tablaItems.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tablaItems.getColumnModel().getColumn(0).setPreferredWidth(220);
        panelCarrito.add(new JScrollPane(tablaItems), BorderLayout.CENTER);

        // FIX: Eliminar item realmente lo quita de la lista interna y recalcula
        JButton btnEliminar = new JButton("Eliminar item seleccionado");
        LoginFrame.estilizarBoton(btnEliminar, new Color(170, 40, 40));
        btnEliminar.addActionListener(e -> eliminarItemSeleccionado());
        panelCarrito.add(btnEliminar, BorderLayout.SOUTH);

        // Totales
        JPanel panelTotales = new JPanel(new GridLayout(3, 2, 6, 4));
        panelTotales.setBackground(new Color(235, 242, 255));
        panelTotales.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        lblSubtotal = new JLabel("$0", SwingConstants.RIGHT);
        lblIva      = new JLabel("$0", SwingConstants.RIGHT);
        lblTotal    = new JLabel("$0", SwingConstants.RIGHT);
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTotal.setForeground(new Color(34, 85, 153));

        JLabel lbSub  = new JLabel("Subtotal:");
        JLabel lbIva  = new JLabel("IVA:");
        JLabel lbTot  = new JLabel("TOTAL:");
        lbTot.setFont(new Font("Segoe UI", Font.BOLD, 14));

        panelTotales.add(lbSub);  panelTotales.add(lblSubtotal);
        panelTotales.add(lbIva);  panelTotales.add(lblIva);
        panelTotales.add(lbTot);  panelTotales.add(lblTotal);

        // Cobro
        JPanel panelCobro = new JPanel(new GridBagLayout());
        panelCobro.setBackground(Color.WHITE);
        panelCobro.setBorder(BorderFactory.createTitledBorder("Cobro"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 8, 5, 8);
        gc.fill = GridBagConstraints.HORIZONTAL;

        cmbPago = new JComboBox<>(new String[]{
            "EFECTIVO", "TARJETA_DEBITO", "TARJETA_CREDITO"});
        txtMontoPago = new JTextField("0", 12);
        LoginFrame.estilizarCampo(txtMontoPago);
        btnCobrar = new JButton("COBRAR");
        LoginFrame.estilizarBoton(btnCobrar, new Color(34, 85, 153));
        btnCobrar.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnCobrar.setPreferredSize(new Dimension(0, 46));

        gc.gridx=0; gc.gridy=0; gc.weightx=0.35;
        panelCobro.add(new JLabel("Metodo pago:"), gc);
        gc.gridx=1; gc.weightx=0.65;
        panelCobro.add(cmbPago, gc);
        gc.gridx=0; gc.gridy=1; gc.weightx=0.35;
        panelCobro.add(new JLabel("Monto recibido:"), gc);
        gc.gridx=1; gc.weightx=0.65;
        panelCobro.add(txtMontoPago, gc);
        gc.gridx=0; gc.gridy=2; gc.gridwidth=2;
        panelCobro.add(btnCobrar, gc);

        JPanel inferior = new JPanel(new BorderLayout(4, 4));
        inferior.setBackground(Color.WHITE);
        inferior.add(panelTotales, BorderLayout.NORTH);
        inferior.add(panelCobro,   BorderLayout.CENTER);

        panel.add(panelCarrito, BorderLayout.CENTER);
        panel.add(inferior,     BorderLayout.SOUTH);

        btnCobrar.addActionListener(e -> procesarCobro());
        return panel;
    }

    // =========================================================
    //  LOGICA DEL CARRITO
    // =========================================================

    /** Agrega un producto al carrito en memoria. NO toca la BD ni el stock todavia. */
    private void agregarAlCarrito(String idProducto, int cantidad) {
        if (facturaActual == null) return;

        Producto p = sistema.getInventario().buscarProducto(idProducto);
        if (p == null) return;

        // Calcular cuanto ya hay de este producto en el carrito
        int yaEnCarrito = itemsCarrito.stream()
                .filter(i -> i.getProducto().getId().equals(idProducto))
                .mapToInt(ItemFactura::getCantidad).sum();

        // Verificar stock disponible restando lo que ya esta en el carrito
        int stockReal = p.getStock() - yaEnCarrito;
        if (cantidad > stockReal) {
            JOptionPane.showMessageDialog(this,
                    "Stock insuficiente.\nDisponible: " + stockReal +
                    (yaEnCarrito > 0 ? " (ya tiene " + yaEnCarrito + " en el carrito)" : ""),
                    "Sin stock", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ItemFactura item = new ItemFactura(p, cantidad);

        // Agregar al modelo de la factura y a la lista interna
        facturaActual.agregarItem(item);
        itemsCarrito.add(item);

        // Actualizar tabla del carrito
        modeloItems.addRow(new Object[]{
                p.getNombre(),
                cantidad,
                String.format("$%,.0f", item.getPrecioUnitario()),
                String.format("$%,.0f", item.getImpuesto()),
                String.format("$%,.0f", item.getTotal())
        });

        actualizarTotales();

        // Resetear cantidad a 1 despues de agregar
        txtCant.setText("1");
        txtCant.requestFocus();
    }

    /** Elimina el item seleccionado del carrito en memoria (sin tocar BD). */
    private void eliminarItemSeleccionado() {
        int fila = tablaItems.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un item del carrito para eliminar.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Quitar de la lista interna y de la factura
        itemsCarrito.remove(fila);
        modeloItems.removeRow(fila);

        // Reconstruir la factura sin el item eliminado
        reconstruirFactura();
        actualizarTotales();
    }

    /** Recrea la factura en memoria con los items actuales del carrito. */
    private void reconstruirFactura() {
        try {
            facturaActual = sistema.crearFactura(clienteActual);
            for (ItemFactura item : itemsCarrito)
                facturaActual.agregarItem(item);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void actualizarTotales() {
        lblSubtotal .setText(String.format("$%,.0f", facturaActual.calcularSubtotal()));
        lblIva      .setText(String.format("$%,.0f", facturaActual.calcularIva()));
        lblTotal    .setText(String.format("$%,.0f", facturaActual.calcularTotal()));
        txtMontoPago.setText(String.format("%.0f",   facturaActual.calcularTotal()));
    }

    // =========================================================
    //  BUSQUEDA
    // =========================================================
    private void buscarProductos(String texto) {
        modeloProductos.setRowCount(0);
        List<Producto> lista = texto.isBlank()
                ? new ArrayList<>(sistema.getInventario().getTodos().values())
                : sistema.getInventario().buscarPorNombre(texto);
        // Solo mostrar productos activos
        for (Producto p : lista)
            if (p.isActivo())
                modeloProductos.addRow(new Object[]{
                        p.getId(), p.getNombre(),
                        String.format("$%,.0f", p.getPrecio()),
                        String.format("%.0f%%", p.getImpuesto() * 100),
                        p.getStock()
                });
    }

    // =========================================================
    //  COBRO — aqui es donde la BD se actualiza (1 sola vez)
    // =========================================================
    private void procesarCobro() {
        if (facturaActual == null || facturaActual.getItems().isEmpty()) {
            JOptionPane.showMessageDialog(this, "El carrito esta vacio.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double montoIngresado;
        try {
            montoIngresado = Double.parseDouble(
                    txtMontoPago.getText().replace(",", "").replace("$", "").trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Monto de pago invalido.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String tipoPago = (String) cmbPago.getSelectedItem();
        MetodoPago pago;

        if ("EFECTIVO".equals(tipoPago)) {
            pago = new PagoEfectivo(montoIngresado);
        } else {
            String digitos = JOptionPane.showInputDialog(this,
                    "Ingrese los ultimos 4 digitos de la tarjeta:");
            if (digitos == null) return;
            pago = new PagoTarjeta(digitos,
                    tipoPago.contains("DEBITO") ? "DEBITO" : "CREDITO");
        }

        btnCobrar.setEnabled(false);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                // FIX #2: procesarPago guarda en BD Y descuenta stock UNA SOLA VEZ
                return sistema.procesarPago(facturaActual, pago);
            }
            @Override
            protected void done() {
                try {
                    if (get()) {
                        pago.generarRecibo();
                        String ruta = GeneradorReportePDF.generarFacturaPDF(facturaActual);
                        String cambioTxt = (pago instanceof PagoEfectivo pe)
                                ? "\nCambio: $" + String.format("%,.0f", pe.getCambio())
                                : "";
                        int resp = JOptionPane.showConfirmDialog(
                                NuevaVentaFrame.this,
                                "Factura " + facturaActual.getNumero() +
                                " registrada exitosamente.\n" +
                                "PDF guardado en: " + ruta + cambioTxt +
                                "\n\n¿Desea realizar otra venta?",
                                "Venta completada",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.INFORMATION_MESSAGE);

                        dispose();
                        if (resp == JOptionPane.YES_OPTION)
                            new NuevaVentaFrame(parent).setVisible(true);

                    } else {
                        JOptionPane.showMessageDialog(NuevaVentaFrame.this,
                                "Pago rechazado. Verifique el monto ingresado.",
                                "Pago fallido", JOptionPane.ERROR_MESSAGE);
                        btnCobrar.setEnabled(true);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(NuevaVentaFrame.this,
                            "Error al procesar: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    btnCobrar.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    // =========================================================
    //  UTILIDADES
    // =========================================================
    private int leerCantidad() {
        try {
            int v = Integer.parseInt(txtCant.getText().trim());
            return Math.max(1, v);
        } catch (NumberFormatException ex) {
            txtCant.setText("1");
            return 1;
        }
    }

    private void cambiarCantidad(int delta) {
        int actual = leerCantidad();
        int nuevo  = Math.max(1, actual + delta);
        txtCant.setText(String.valueOf(nuevo));
    }

    private void estilizarBtnCant(JButton btn, Color color) {
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(32, 32));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
