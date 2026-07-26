package supermercado.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import supermercado.dao.ClienteDAO;
import supermercado.modelo.Cliente;
import supermercado.modelo.Factura;
import supermercado.modelo.ItemFactura;
import supermercado.modelo.Producto;
import supermercado.pago.MetodoPago;
import supermercado.pago.PagoEfectivo;
import supermercado.pago.PagoTarjeta;
import supermercado.reporte.GeneradorReportePDF;
import supermercado.servicio.SistemaFacturacion;

public class NuevaVentaFrame extends JFrame {

    private final JFrame parent;
    private final SistemaFacturacion sistema = SistemaFacturacion.getInstance();
    private final ClienteDAO clienteDAO = new ClienteDAO();

    private Factura facturaActual;
    private Cliente clienteActual;

    // Lista interna del carrito (fuente de verdad en memoria)
    private final List<ItemFactura> itemsCarrito = new ArrayList<>();

    // ---- Componentes cliente ----
    private JTextField txtNitBuscar;
    private JLabel lblClienteNombre;
    private JLabel lblClienteInfo;
    private JButton btnBuscarCliente;
    private JButton btnNuevoCliente;
    private JButton btnConsumidorFinal;
    private JPanel panelClienteInfo;

    // ---- Componentes productos ----
    private JTextField txtBuscarProducto;
    private JTable tablaProductos;
    private DefaultTableModel modeloProductos;
    private JTextField txtCant;
    private JLabel lblProductoFoto;

    // ---- Componentes carrito ----
    private JTable tablaItems;
    private DefaultTableModel modeloItems;
    private JLabel lblSubtotal, lblIva, lblTotal;
    private JComboBox<String> cmbPago;
    private JTextField txtMontoPago;
    private JButton btnCobrar;
    private JPanel panelCarritoBorder;

    public NuevaVentaFrame(JFrame parent) {
        this.parent = parent;
        setTitle("Nueva Venta");
        setSize(1050, 740);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setIconImage(AppIcon.getIcon());
        setLocationRelativeTo(parent);
        construirUI();
        // Iniciar con consumidor final por defecto
        asignarConsumidorFinal();
        buscarProductos("");
    }

    // =========================================================
    // CONSTRUCCION UI
    // =========================================================
    private void construirUI() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        root.setBackground(Color.WHITE);

        // NORTH: selector de cliente (ancho completo)
        root.add(construirPanelCliente(), BorderLayout.NORTH);

        // CENTER: productos (izq) + carrito (der)
        JPanel centro = new JPanel(new BorderLayout(8, 0));
        centro.setBackground(Color.WHITE);
        centro.add(construirPanelProductos(), BorderLayout.WEST);
        centro.add(construirPanelCarrito(), BorderLayout.CENTER);
        root.add(centro, BorderLayout.CENTER);

        add(root);
    }

    // =========================================================
    // PANEL CLIENTE (NORTH — ancho completo)
    // =========================================================
    private JPanel construirPanelCliente() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(new Color(240, 246, 255));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(34, 85, 153), 1),
                        "Cliente de la factura",
                        TitledBorder.LEFT, TitledBorder.TOP,
                        new Font("Segoe UI", Font.BOLD, 11),
                        new Color(34, 85, 153)),
                BorderFactory.createEmptyBorder(4, 8, 6, 8)));

        // ---- Izquierda: campo NIT + botones ----
        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        izq.setBackground(new Color(240, 246, 255));

        JLabel lblNit = new JLabel("NIT o Nombre:");
        lblNit.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtNitBuscar = new JTextField(16);
        UIUtils.estilizarCampo(txtNitBuscar);
        txtNitBuscar.setToolTipText("Ingrese el NIT/CC (numeros) o el nombre del cliente y presione Buscar");

        btnBuscarCliente = new JButton("Buscar cliente");
        btnNuevoCliente = new JButton("+ Nuevo cliente");
        btnConsumidorFinal = new JButton("Consumidor final");

        UIUtils.estilizarBoton(btnBuscarCliente, new Color(34, 85, 153));
        UIUtils.estilizarBoton(btnNuevoCliente, new Color(30, 130, 76));
        UIUtils.estilizarBoton(btnConsumidorFinal, new Color(100, 100, 100));

        Dimension dimBtn = new Dimension(150, 30);
        btnBuscarCliente.setPreferredSize(dimBtn);
        btnNuevoCliente.setPreferredSize(dimBtn);
        btnConsumidorFinal.setPreferredSize(dimBtn);

        izq.add(lblNit);
        izq.add(txtNitBuscar);
        izq.add(btnBuscarCliente);
        izq.add(btnNuevoCliente);
        izq.add(btnConsumidorFinal);
        panel.add(izq, BorderLayout.WEST);

        // ---- Derecha: info del cliente seleccionado ----
        panelClienteInfo = new JPanel(new GridLayout(2, 1, 0, 2));
        panelClienteInfo.setBackground(new Color(240, 246, 255));
        panelClienteInfo.setBorder(BorderFactory.createEmptyBorder(2, 12, 2, 8));

        lblClienteNombre = new JLabel("Consumidor Final");
        lblClienteNombre.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblClienteNombre.setForeground(new Color(34, 85, 153));

        lblClienteInfo = new JLabel("NIT: 222222222  |  Sin datos adicionales");
        lblClienteInfo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblClienteInfo.setForeground(Color.GRAY);

        panelClienteInfo.add(lblClienteNombre);
        panelClienteInfo.add(lblClienteInfo);
        panel.add(panelClienteInfo, BorderLayout.CENTER);

        // ---- Acciones ----
        txtNitBuscar.addActionListener(e -> buscarCliente());
        btnBuscarCliente.addActionListener(e -> buscarCliente());
        btnConsumidorFinal.addActionListener(e -> asignarConsumidorFinal());
        btnNuevoCliente.addActionListener(e -> abrirDialogoNuevoCliente());

        return panel;
    }

    // =========================================================
    // PANEL PRODUCTOS (WEST)
    // =========================================================
    private JPanel construirPanelProductos() {
        JPanel panel = new JPanel(new BorderLayout(4, 6));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder("Productos"));
        panel.setPreferredSize(new Dimension(440, 0));

        // Barra busqueda
        JPanel barraBusq = new JPanel(new BorderLayout(4, 0));
        barraBusq.setBackground(Color.WHITE);
        txtBuscarProducto = new JTextField();
        UIUtils.estilizarCampo(txtBuscarProducto);
        JButton btnBuscar = new JButton("Buscar");
        UIUtils.estilizarBoton(btnBuscar, new Color(34, 85, 153));
        barraBusq.add(new JLabel("Buscar: "), BorderLayout.WEST);
        barraBusq.add(txtBuscarProducto, BorderLayout.CENTER);
        barraBusq.add(btnBuscar, BorderLayout.EAST);
        panel.add(barraBusq, BorderLayout.NORTH);

        // Tabla productos
        modeloProductos = new DefaultTableModel(
                new String[] { "Codigo", "Nombre", "Precio", "IVA%", "Stock", "Foto" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaProductos = new JTable(modeloProductos);
        tablaProductos.setRowHeight(64);
        tablaProductos.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tablaProductos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaProductos.getColumnModel().getColumn(0).setPreferredWidth(55);
        tablaProductos.getColumnModel().getColumn(1).setPreferredWidth(200);
        tablaProductos.getColumnModel().getColumn(2).setPreferredWidth(65);
        tablaProductos.getColumnModel().getColumn(3).setPreferredWidth(45);
        tablaProductos.getColumnModel().getColumn(4).setPreferredWidth(45);
        tablaProductos.getColumnModel().getColumn(5).setPreferredWidth(70);
        tablaProductos.setDefaultRenderer(Object.class, rendererImagenProducto());
        panel.add(new JScrollPane(tablaProductos), BorderLayout.CENTER);

        // Panel de previsualizacion de producto
        JPanel panelFoto = new JPanel(new BorderLayout());
        panelFoto.setBackground(Color.WHITE);
        panelFoto.setBorder(BorderFactory.createTitledBorder("Producto seleccionado"));
        lblProductoFoto = new JLabel("Sin imagen", SwingConstants.CENTER);
        lblProductoFoto.setPreferredSize(new Dimension(120, 120));
        lblProductoFoto.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblProductoFoto.setForeground(Color.GRAY);
        panelFoto.add(lblProductoFoto, BorderLayout.CENTER);

        // Panel cantidad + botones
        JPanel panelAgregar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        panelAgregar.setBackground(Color.WHITE);

        txtCant = new JTextField("1", 6);
        UIUtils.estilizarCampo(txtCant);
        txtCant.setHorizontalAlignment(JTextField.CENTER);
        txtCant.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JButton btnMenos = new JButton("-");
        JButton btnMas = new JButton("+");
        estilizarBtnCant(btnMenos, new Color(180, 60, 60));
        estilizarBtnCant(btnMas, new Color(30, 130, 76));

        JButton btnAgregar = new JButton("  Agregar al carrito  ");
        UIUtils.estilizarBoton(btnAgregar, new Color(30, 130, 76));
        btnAgregar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAgregar.setPreferredSize(new Dimension(180, 34));

        panelAgregar.add(new JLabel("Cantidad:"));
        panelAgregar.add(btnMenos);
        panelAgregar.add(txtCant);
        panelAgregar.add(btnMas);
        panelAgregar.add(Box.createHorizontalStrut(8));
        panelAgregar.add(btnAgregar);

        JPanel panelInferior = new JPanel(new BorderLayout(0, 6));
        panelInferior.setBackground(Color.WHITE);
        panelInferior.add(panelFoto, BorderLayout.CENTER);
        panelInferior.add(panelAgregar, BorderLayout.SOUTH);
        panel.add(panelInferior, BorderLayout.SOUTH);

        // Acciones
        ActionListener accionBuscar = e -> buscarProductos(txtBuscarProducto.getText());
        btnBuscar.addActionListener(accionBuscar);
        txtBuscarProducto.addActionListener(accionBuscar);
        btnMenos.addActionListener(e -> cambiarCantidad(-1));
        btnMas.addActionListener(e -> cambiarCantidad(+1));
        txtCant.addActionListener(e -> btnAgregar.doClick());
        tablaProductos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                actualizarPrevisualizacionProducto();
            }
        });

        btnAgregar.addActionListener(e -> {
            int fila = tablaProductos.getSelectedRow();
            if (fila < 0) {
                JOptionPane.showMessageDialog(this,
                        "Seleccione un producto de la lista.", "Aviso",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            int cant = leerCantidad();
            if (cant <= 0)
                return;
            agregarAlCarrito((String) modeloProductos.getValueAt(fila, 0), cant);
        });

        tablaProductos.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int fila = tablaProductos.getSelectedRow();
                    if (fila >= 0)
                        agregarAlCarrito(
                                (String) modeloProductos.getValueAt(fila, 0),
                                leerCantidad());
                }
            }
        });

        return panel;
    }

    // =========================================================
    // PANEL CARRITO (CENTER)
    // =========================================================
    private JPanel construirPanelCarrito() {
        JPanel panel = new JPanel(new BorderLayout(4, 6));
        panel.setBackground(Color.WHITE);

        // Tabla carrito
        panelCarritoBorder = new JPanel(new BorderLayout(4, 4));
        panelCarritoBorder.setBorder(BorderFactory.createTitledBorder("Carrito"));
        panelCarritoBorder.setBackground(Color.WHITE);

        modeloItems = new DefaultTableModel(
                new String[] { "Foto", "Producto", "Cant.", "Unitario", "IVA", "Total" }, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tablaItems = new JTable(modeloItems);
        tablaItems.setRowHeight(64);
        tablaItems.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tablaItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaItems.getColumnModel().getColumn(0).setPreferredWidth(70);
        tablaItems.getColumnModel().getColumn(1).setPreferredWidth(180);
        tablaItems.getColumnModel().getColumn(2).setPreferredWidth(40);
        tablaItems.getColumnModel().getColumn(3).setPreferredWidth(80);
        tablaItems.getColumnModel().getColumn(4).setPreferredWidth(80);
        tablaItems.getColumnModel().getColumn(5).setPreferredWidth(100);
        tablaItems.setDefaultRenderer(Object.class, rendererImagenProducto());
        panelCarritoBorder.add(new JScrollPane(tablaItems), BorderLayout.CENTER);

        JButton btnEliminar = new JButton("Eliminar item seleccionado");
        UIUtils.estilizarBoton(btnEliminar, new Color(170, 40, 40));
        btnEliminar.addActionListener(e -> eliminarItemSeleccionado());
        panelCarritoBorder.add(btnEliminar, BorderLayout.SOUTH);

        // Totales
        JPanel panelTotales = new JPanel(new GridLayout(3, 2, 6, 4));
        panelTotales.setBackground(new Color(235, 242, 255));
        panelTotales.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        lblSubtotal = new JLabel("$0", SwingConstants.RIGHT);
        lblIva = new JLabel("$0", SwingConstants.RIGHT);
        lblTotal = new JLabel("$0", SwingConstants.RIGHT);
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTotal.setForeground(new Color(34, 85, 153));

        JLabel lbTot = new JLabel("TOTAL:");
        lbTot.setFont(new Font("Segoe UI", Font.BOLD, 14));
        panelTotales.add(new JLabel("Subtotal:"));
        panelTotales.add(lblSubtotal);
        panelTotales.add(new JLabel("IVA:"));
        panelTotales.add(lblIva);
        panelTotales.add(lbTot);
        panelTotales.add(lblTotal);

        // Cobro
        JPanel panelCobro = new JPanel(new GridBagLayout());
        panelCobro.setBackground(Color.WHITE);
        panelCobro.setBorder(BorderFactory.createTitledBorder("Cobro"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 8, 5, 8);
        gc.fill = GridBagConstraints.HORIZONTAL;

        cmbPago = new JComboBox<>(new String[] {
                "EFECTIVO", "TARJETA_DEBITO", "TARJETA_CREDITO" });
        txtMontoPago = new JTextField("0", 12);
        UIUtils.estilizarCampo(txtMontoPago);
        btnCobrar = new JButton("COBRAR");
        UIUtils.estilizarBoton(btnCobrar, new Color(34, 85, 153));
        btnCobrar.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnCobrar.setPreferredSize(new Dimension(0, 46));

        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 0.35;
        panelCobro.add(new JLabel("Metodo pago:"), gc);
        gc.gridx = 1;
        gc.weightx = 0.65;
        panelCobro.add(cmbPago, gc);
        gc.gridx = 0;
        gc.gridy = 1;
        gc.weightx = 0.35;
        panelCobro.add(new JLabel("Monto recibido:"), gc);
        gc.gridx = 1;
        gc.weightx = 0.65;
        panelCobro.add(txtMontoPago, gc);
        gc.gridx = 0;
        gc.gridy = 2;
        gc.gridwidth = 2;
        panelCobro.add(btnCobrar, gc);

        JPanel inferior = new JPanel(new BorderLayout(4, 4));
        inferior.setBackground(Color.WHITE);
        inferior.add(panelTotales, BorderLayout.NORTH);
        inferior.add(panelCobro, BorderLayout.CENTER);

        panel.add(panelCarritoBorder, BorderLayout.CENTER);
        panel.add(inferior, BorderLayout.SOUTH);

        btnCobrar.addActionListener(e -> procesarCobro());
        return panel;
    }

    // =========================================================
    // LOGICA DE CLIENTE
    // =========================================================

    /** Busca cliente por NIT o por nombre y lo asigna a la factura. */
    private void buscarCliente() {
        String texto = txtNitBuscar.getText().trim();
        if (texto.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Ingrese el NIT, cedula o nombre del cliente.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            java.util.List<Cliente> resultados = clienteDAO.buscarInteligente(texto);

            if (resultados.isEmpty()) {
                // No se encontro nada
                String msg = texto.matches("\\d+")
                        ? "No existe cliente con NIT: " + texto
                        : "No existe cliente con nombre: \"" + texto + "\"";
                int resp = JOptionPane.showConfirmDialog(this,
                        msg + "\n\n¿Desea registrarlo como cliente nuevo?",
                        "Cliente no encontrado",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (resp == JOptionPane.YES_OPTION)
                    abrirDialogoNuevoCliente(texto.matches("\\d+") ? texto : "");

            } else if (resultados.size() == 1) {
                // Resultado unico: asignar directamente
                asignarCliente(resultados.get(0));

            } else {
                // Multiples resultados: mostrar dialogo de seleccion
                Cliente seleccionado = mostrarDialogoSeleccion(resultados);
                if (seleccionado != null)
                    asignarCliente(seleccionado);
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error al buscar cliente: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Muestra una ventana con la lista de clientes encontrados
     * para que el cajero elija el correcto.
     */
    private Cliente mostrarDialogoSeleccion(java.util.List<Cliente> clientes) {
        JDialog dlg = new JDialog(this, "Seleccionar cliente", true);
        dlg.setSize(560, 340);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        panel.setBackground(Color.WHITE);

        JLabel lbl = new JLabel("Se encontraron " + clientes.size() +
                " clientes. Seleccione el correcto y haga doble clic o presione Elegir:");
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(lbl, BorderLayout.NORTH);

        // Tabla de resultados
        String[] cols = { "NIT / CC", "Nombre", "Telefono", "Email" };
        DefaultTableModel modelo = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        for (Cliente c : clientes)
            modelo.addRow(new Object[] {
                    c.getNit(), c.getNombre(), c.getTelefono(), c.getEmail() });

        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(26);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(90);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(200);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(100);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(150);
        tabla.setRowSelectionInterval(0, 0); // seleccionar el primero por defecto
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        // Botones
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setBackground(Color.WHITE);
        JButton btnElegir = new JButton("Elegir");
        JButton btnCancelar = new JButton("Cancelar");
        UIUtils.estilizarBoton(btnElegir, new Color(34, 85, 153));
        UIUtils.estilizarBoton(btnCancelar, new Color(120, 120, 120));
        btnElegir.setPreferredSize(new Dimension(100, 34));
        btnCancelar.setPreferredSize(new Dimension(100, 34));
        btnPanel.add(btnCancelar);
        btnPanel.add(btnElegir);
        panel.add(btnPanel, BorderLayout.SOUTH);

        dlg.add(panel);

        // Resultado seleccionado
        final Cliente[] resultado = { null };

        Runnable elegir = () -> {
            int fila = tabla.getSelectedRow();
            if (fila >= 0) {
                resultado[0] = clientes.get(fila);
                dlg.dispose();
            }
        };

        btnElegir.addActionListener(e -> elegir.run());
        btnCancelar.addActionListener(e -> dlg.dispose());

        // Doble clic en la tabla tambien selecciona
        tabla.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    elegir.run();
            }
        });

        // Enter en la tabla confirma
        tabla.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    elegir.run();
            }
        });

        dlg.setVisible(true);
        return resultado[0];
    }

    /** Asigna el cliente "Consumidor Final" por defecto. */
    private void asignarConsumidorFinal() {
        try {
            Cliente c = clienteDAO.consumidorFinal();
            asignarCliente(c);
            txtNitBuscar.setText("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    /** Actualiza la factura y la UI con el cliente dado. */
    private void asignarCliente(Cliente c) {
        clienteActual = c;
        // Crear o recrear la factura con el nuevo cliente
        try {
            facturaActual = sistema.crearFactura(clienteActual);
            // Volver a agregar los items que ya estaban en el carrito
            for (ItemFactura item : itemsCarrito)
                facturaActual.agregarItem(item);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al crear factura: " + ex.getMessage());
        }

        // Actualizar UI
        boolean esConsumidorFinal = "222222222".equals(c.getNit());
        lblClienteNombre.setText(c.getNombre());
        lblClienteNombre.setForeground(
                esConsumidorFinal ? new Color(120, 120, 120) : new Color(34, 85, 153));

        String info = "NIT: " + c.getNit();
        if (!c.getTelefono().isBlank())
            info += "  |  Tel: " + c.getTelefono();
        if (!c.getEmail().isBlank())
            info += "  |  " + c.getEmail();
        if (!c.getDireccion().isBlank())
            info += "  |  " + c.getDireccion();
        lblClienteInfo.setText(info);

        // Actualizar titulo del carrito con numero de factura
        if (panelCarritoBorder != null && facturaActual != null) {
            ((TitledBorder) panelCarritoBorder.getBorder())
                    .setTitle("Carrito  —  N° " + facturaActual.getNumero() +
                            "  |  Cliente: " + c.getNombre());
            panelCarritoBorder.repaint();
        }
        txtNitBuscar.setText(esConsumidorFinal ? "" : c.getNit());
    }

    /** Dialogo para registrar un cliente nuevo rapidamente. */
    private void abrirDialogoNuevoCliente() {
        abrirDialogoNuevoCliente("");
    }

    private void abrirDialogoNuevoCliente(String nitPrellenado) {
        JDialog dlg = new JDialog(this, "Registrar nuevo cliente", true);
        dlg.setSize(420, 360);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));
        panel.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 6, 4);
        g.fill = GridBagConstraints.HORIZONTAL;

        JTextField fNit = campoDlg(nitPrellenado);
        JTextField fNom = campoDlg("");
        JTextField fTel = campoDlg("");
        JTextField fMail = campoDlg("");
        JTextField fDir = campoDlg("");

        // Validacion visual del NIT en tiempo real
        fNit.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                boolean ok = fNit.getText().trim().matches("\\d{6,15}");
                fNit.setBackground(ok ? new Color(230, 255, 230)
                        : new Color(255, 230, 230));
            }
        });

        String[] etiq = { "NIT / Cedula: *", "Nombre completo: *",
                "Telefono:", "Email:", "Direccion:" };
        JTextField[] campos = { fNit, fNom, fTel, fMail, fDir };
        for (int i = 0; i < etiq.length; i++) {
            g.gridx = 0;
            g.gridy = i;
            g.weightx = 0.35;
            JLabel lbl = new JLabel(etiq[i]);
            if (etiq[i].endsWith("*"))
                lbl.setForeground(new Color(34, 85, 153));
            panel.add(lbl, g);
            g.gridx = 1;
            g.weightx = 0.65;
            panel.add(campos[i], g);
        }

        JLabel lblErr = new JLabel("  * Campos obligatorios", SwingConstants.LEFT);
        lblErr.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        lblErr.setForeground(Color.GRAY);
        g.gridx = 0;
        g.gridy = etiq.length;
        g.gridwidth = 2;
        panel.add(lblErr, g);

        JPanel bp = new JPanel(new GridLayout(1, 2, 10, 0));
        bp.setBackground(Color.WHITE);
        JButton btnOk = new JButton("Registrar y asignar");
        JButton btnCan = new JButton("Cancelar");
        UIUtils.estilizarBoton(btnOk, new Color(30, 130, 76));
        UIUtils.estilizarBoton(btnCan, new Color(120, 120, 120));
        btnOk.setPreferredSize(new Dimension(0, 36));
        btnCan.setPreferredSize(new Dimension(0, 36));
        bp.add(btnOk);
        bp.add(btnCan);
        g.gridy = etiq.length + 1;
        panel.add(bp, g);

        dlg.add(panel);
        btnCan.addActionListener(e -> dlg.dispose());

        btnOk.addActionListener(e -> {
            String nit = fNit.getText().trim();
            String nom = fNom.getText().trim();
            if (!nit.matches("\\d{6,15}")) {
                lblErr.setText("NIT invalido: solo digitos, minimo 6.");
                lblErr.setForeground(Color.RED);
                return;
            }
            if (nom.isEmpty()) {
                lblErr.setText("El nombre es obligatorio.");
                lblErr.setForeground(Color.RED);
                return;
            }
            try {
                Cliente nuevo = new Cliente(nit, nom,
                        fTel.getText().trim(),
                        fMail.getText().trim(),
                        fDir.getText().trim());
                clienteDAO.guardar(nuevo);
                asignarCliente(nuevo);
                dlg.dispose();
                JOptionPane.showMessageDialog(this,
                        "Cliente \"" + nom + "\" registrado y asignado a la factura.",
                        "Exito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                lblErr.setText("Error: " + ex.getMessage());
                lblErr.setForeground(Color.RED);
            }
        });
        dlg.setVisible(true);
    }

    // =========================================================
    // LOGICA CARRITO
    // =========================================================
    private void agregarAlCarrito(String idProducto, int cantidad) {
        if (facturaActual == null) {
            JOptionPane.showMessageDialog(this,
                    "No hay factura activa. Seleccione un cliente primero.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Producto p = sistema.getInventario().buscarProducto(idProducto);
        if (p == null)
            return;

        int yaEnCarrito = itemsCarrito.stream()
                .filter(i -> i.getProducto().getId().equals(idProducto))
                .mapToInt(ItemFactura::getCantidad).sum();
        int stockDisponible = p.getStock() - yaEnCarrito;

        if (cantidad > stockDisponible) {
            JOptionPane.showMessageDialog(this,
                    "Stock insuficiente.\nDisponible: " + stockDisponible +
                            (yaEnCarrito > 0 ? "  (ya tiene " + yaEnCarrito + " en el carrito)" : ""),
                    "Sin stock", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ItemFactura item = new ItemFactura(p, cantidad);
        facturaActual.agregarItem(item);
        itemsCarrito.add(item);

        modeloItems.addRow(new Object[] {
                crearIconoProducto(p),
                p.getNombre(),
                cantidad,
                String.format("$%,.0f", item.getPrecioUnitario()),
                String.format("$%,.0f", item.getImpuesto()),
                String.format("$%,.0f", item.getTotal())
        });

        actualizarTotales();
        txtCant.setText("1");
        txtCant.requestFocus();
    }

    private void eliminarItemSeleccionado() {
        int fila = tablaItems.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un item del carrito para eliminar.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        itemsCarrito.remove(fila);
        modeloItems.removeRow(fila);
        reconstruirFactura();
        actualizarTotales();
    }

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
        if (facturaActual == null)
            return;
        lblSubtotal.setText(String.format("$%,.0f", facturaActual.calcularSubtotal()));
        lblIva.setText(String.format("$%,.0f", facturaActual.calcularIva()));
        lblTotal.setText(String.format("$%,.0f", facturaActual.calcularTotal()));
        txtMontoPago.setText(String.format("%.0f", facturaActual.calcularTotal()));
    }

    // =========================================================
    // BUSQUEDA DE PRODUCTOS
    // =========================================================
    private void buscarProductos(String texto) {
        modeloProductos.setRowCount(0);
        List<Producto> lista = texto.isBlank()
                ? new ArrayList<>(sistema.getInventario().getTodos().values())
                : sistema.getInventario().buscarPorNombre(texto);
        for (Producto p : lista)
            if (p.isActivo())
                modeloProductos.addRow(new Object[] {
                        p.getId(), p.getNombre(),
                        String.format("$%,.0f", p.getPrecio()),
                        String.format("%.0f%%", p.getImpuesto() * 100),
                        p.getStock(),
                        crearIconoProducto(p)
                });

        if (tablaProductos.getRowCount() > 0) {
            tablaProductos.setRowSelectionInterval(0, 0);
            actualizarPrevisualizacionProducto();
        } else {
            lblProductoFoto.setIcon(null);
            lblProductoFoto.setText("Sin imagen");
        }
    }

    private void actualizarPrevisualizacionProducto() {
        int fila = tablaProductos.getSelectedRow();
        if (fila < 0) {
            lblProductoFoto.setIcon(null);
            lblProductoFoto.setText("Sin imagen");
            return;
        }
        int filaModelo = tablaProductos.convertRowIndexToModel(fila);
        String id = (String) modeloProductos.getValueAt(filaModelo, 0);
        Producto p = sistema.getInventario().buscarProducto(id);
        ImageIcon icon = crearIconoProducto(p);
        if (icon != null) {
            lblProductoFoto.setIcon(icon);
            lblProductoFoto.setText("");
        } else {
            lblProductoFoto.setIcon(null);
            lblProductoFoto.setText("Sin imagen");
        }
    }

    private DefaultTableCellRenderer rendererImagenProducto() {
        return new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                if (value instanceof ImageIcon icon) {
                    JLabel label = new JLabel();
                    label.setOpaque(true);
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                    label.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
                    label.setBackground(isSelected
                            ? new Color(184, 207, 229)
                            : (row % 2 == 0 ? new Color(245, 250, 245) : Color.WHITE));
                    label.setIcon(icon);
                    label.setText("");
                    return label;
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        };
    }

    private ImageIcon crearIconoProducto(Producto p) {
        if (p == null || p.getRutaImagen() == null || p.getRutaImagen().isBlank()) {
            return null;
        }

        File archivo = supermercado.util.GestorImagenes.obtenerArchivo(p.getRutaImagen());
        if (archivo == null) {
            System.out.println("[NuevaVentaFrame] No se encontro imagen para producto '" + p.getId() + "' ruta='"
                    + p.getRutaImagen() + "'");
            return null;
        }

        try {
            java.awt.Image img = new ImageIcon(archivo.getAbsolutePath()).getImage();
            img = img.getScaledInstance(56, 56, java.awt.Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (Exception ex) {
            System.out.println("[NuevaVentaFrame] Error al crear icono de imagen para producto '" + p.getId() + "': "
                    + ex.getMessage());
            return null;
        }
    }

    // =========================================================
    // COBRO
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
            if (digitos == null)
                return;
            pago = new PagoTarjeta(digitos,
                    tipoPago.contains("DEBITO") ? "DEBITO" : "CREDITO");
        }

        btnCobrar.setEnabled(false);
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return sistema.procesarPago(facturaActual, pago);
            }

            @Override
            protected void done() {
                try {
                    if (!get()) {
                        JOptionPane.showMessageDialog(NuevaVentaFrame.this,
                                "Pago rechazado. Verifique el monto.",
                                "Pago fallido", JOptionPane.ERROR_MESSAGE);
                        btnCobrar.setEnabled(true);
                        return;
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(NuevaVentaFrame.this,
                            "Error al procesar el pago: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    btnCobrar.setEnabled(true);
                    return;
                }

                // ---- Pago exitoso: generar PDFs de forma independiente ----
                String rutaNormal = null;
                String rutaTermica = null;

                // PDF normal (A4 con colores)
                try {
                    rutaNormal = GeneradorReportePDF.generarFacturaTermica(facturaActual);
                    System.out.println("[PDF] Factura térmica generada: " + rutaNormal);
                } catch (Exception ex) {
                    System.err.println("[PDF] Error factura térmica: " + ex.getMessage());
                    JOptionPane.showMessageDialog(NuevaVentaFrame.this,
                            "Advertencia: No se pudo generar la factura térmica.\n" + ex.getMessage(),
                            "PDF", JOptionPane.WARNING_MESSAGE);
                }

                // PDF termico (80mm)
                try {
                    rutaTermica = GeneradorReportePDF.generarFacturaTermica(facturaActual);
                    System.out.println("[PDF] Ticket termico generado: " + rutaTermica);
                } catch (Exception ex) {
                    System.err.println("[PDF] Error ticket termico: " + ex.getMessage());
                    JOptionPane.showMessageDialog(NuevaVentaFrame.this,
                            "Advertencia: No se pudo generar el ticket termico.\n" + ex.getMessage(),
                            "PDF Termico", JOptionPane.WARNING_MESSAGE);
                }

                // ---- Dialogo de impresion ----
                String cambioTxt = (pago instanceof PagoEfectivo pe)
                        ? "\nCambio: $" + String.format("%,.0f", pe.getCambio())
                        : "";

                String msgFacturas = "";
                if (rutaNormal != null)
                    msgFacturas += "\nFactura A4:  " + rutaNormal;
                if (rutaTermica != null)
                    msgFacturas += "\nTicket 80mm: " + rutaTermica;

                // Solo mostrar opciones de apertura para los PDFs que se generaron
                boolean hayNormal = rutaNormal != null && new File(rutaNormal).exists();
                boolean hayTermica = rutaTermica != null && new File(rutaTermica).exists();

                if (hayNormal || hayTermica) {
                    // Construir opciones dinamicamente segun los PDFs disponibles
                    java.util.List<String> opsList = new java.util.ArrayList<>();
                    if (hayTermica)
                        opsList.add("Ticket termico (80mm)");
                    if (hayNormal)
                        opsList.add("Factura normal (A4)");
                    opsList.add("No imprimir");
                    String[] opciones = opsList.toArray(new String[0]);

                    int resp = JOptionPane.showOptionDialog(NuevaVentaFrame.this,
                            "Factura " + facturaActual.getNumero() + " registrada correctamente.\n" +
                                    "Cliente: " + clienteActual.getNombre() + cambioTxt + msgFacturas +
                                    "\n\n¿Que documento desea abrir para imprimir?",
                            "Venta completada",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            null, opciones, opciones[0]);

                    try {
                        if (hayTermica && hayNormal) {
                            // Ambos disponibles
                            if (resp == 0 && hayTermica)
                                Desktop.getDesktop().open(new File(rutaTermica));
                            else if (resp == 1 && hayNormal)
                                Desktop.getDesktop().open(new File(rutaNormal));
                        } else if (hayTermica) {
                            if (resp == 0)
                                Desktop.getDesktop().open(new File(rutaTermica));
                        } else if (hayNormal) {
                            if (resp == 0)
                                Desktop.getDesktop().open(new File(rutaNormal));
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(NuevaVentaFrame.this,
                                "No se pudo abrir el PDF: " + ex.getMessage(),
                                "Aviso", JOptionPane.WARNING_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(NuevaVentaFrame.this,
                            "Factura " + facturaActual.getNumero() + " registrada.\n" +
                                    "Cliente: " + clienteActual.getNombre() + cambioTxt +
                                    "\n\nNo se pudieron generar los PDFs.",
                            "Venta completada", JOptionPane.INFORMATION_MESSAGE);
                }

                int otraVenta = JOptionPane.showConfirmDialog(NuevaVentaFrame.this,
                        "¿Desea realizar otra venta?",
                        "Nueva venta", JOptionPane.YES_NO_OPTION);
                dispose();
                if (otraVenta == JOptionPane.YES_OPTION)
                    new NuevaVentaFrame(parent).setVisible(true);
            }
        };
        worker.execute();
    }

    // =========================================================
    // UTILIDADES
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
        txtCant.setText(String.valueOf(Math.max(1, leerCantidad() + delta)));
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

    private JTextField campoDlg(String valor) {
        JTextField t = new JTextField(valor, 18);
        UIUtils.estilizarCampo(t);
        return t;
    }
}
