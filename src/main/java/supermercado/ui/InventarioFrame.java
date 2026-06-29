package supermercado.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import supermercado.db.ConexionDB;
import supermercado.modelo.Producto;
import supermercado.servicio.SistemaFacturacion;

public class InventarioFrame extends JFrame {

    private final SistemaFacturacion sistema = SistemaFacturacion.getInstance();
    private final boolean esAdmin = sistema.getCajeroActivo().esAdmin();

    private DefaultTableModel modelo;
    private JTable tabla;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField txtFiltro;
    private JButton btnAgregar, btnEditar, btnEliminar, btnAjustarStock;

    public InventarioFrame() {
        setTitle("Inventario de Productos" + (esAdmin ? "  [Modo Administrador]" : ""));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setIconImage(AppIcon.getIcon());
        construirUI();
        // Ajustar tamanio DESPUES de construir la UI para que pack() calcule bien
        if (esAdmin)
            setSize(920, 660);
        else
            setSize(880, 580);
        // Centrar EN LA PANTALLA despues de establecer el tamaño
        setLocationRelativeTo(null);
    }

    private void construirUI() {
        // ================================================================
        // ESTRUCTURA PRINCIPAL: BorderLayout con 3 zonas bien definidas
        // NORTH = titulo + filtro
        // CENTER = tabla
        // SOUTH = leyenda + botones (apilados con BoxLayout)
        // ================================================================
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.setBackground(Color.WHITE);

        // ---- NORTH: titulo + filtro ----
        JPanel panelNorth = new JPanel();
        panelNorth.setLayout(new BoxLayout(panelNorth, BoxLayout.Y_AXIS));
        panelNorth.setBackground(Color.WHITE);

        // Fila titulo + badge
        JPanel filaTitulo = new JPanel(new BorderLayout());
        filaTitulo.setBackground(Color.WHITE);
        filaTitulo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JLabel titulo = new JLabel("Inventario de Productos");
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titulo.setForeground(new Color(30, 130, 76));
        JLabel badge = new JLabel(esAdmin ? "  ADMINISTRADOR  " : "  SOLO LECTURA  ");
        badge.setOpaque(true);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        badge.setBackground(esAdmin ? new Color(34, 85, 153) : new Color(120, 120, 120));
        badge.setForeground(Color.WHITE);
        filaTitulo.add(titulo, BorderLayout.WEST);
        filaTitulo.add(badge, BorderLayout.EAST);
        panelNorth.add(filaTitulo);
        panelNorth.add(Box.createVerticalStrut(8));

        // Fila filtro
        JPanel filaFiltro = new JPanel(new BorderLayout(6, 0));
        filaFiltro.setBackground(Color.WHITE);
        filaFiltro.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        txtFiltro = new JTextField();
        UIUtils.estilizarCampo(txtFiltro);
        txtFiltro.setToolTipText("Filtrar por nombre o codigo...");
        JLabel lbF = new JLabel("Buscar: ");
        lbF.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filaFiltro.add(lbF, BorderLayout.WEST);
        filaFiltro.add(txtFiltro, BorderLayout.CENTER);
        panelNorth.add(filaFiltro);
        panelNorth.add(Box.createVerticalStrut(4));

        root.add(panelNorth, BorderLayout.NORTH);

        // ---- CENTER: tabla ----
        modelo = new DefaultTableModel(
                new String[] { "Codigo", "Nombre", "Precio", "IVA%",
                        "Categoria", "Stock", "Precio c/IVA", "Estado" },
                0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tabla = new JTable(modelo);
        tabla.setRowHeight(26);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabla.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(70);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(200);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(120);
        tabla.getColumnModel().getColumn(7).setPreferredWidth(75);

        // Renderer con colores por estado/stock
        tabla.setDefaultRenderer(Object.class, (t, val, sel, foc, row, col) -> {
            JLabel lbl = new JLabel(val == null ? "" : val.toString());
            lbl.setOpaque(true);
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            int stockVal = 0;
            try {
                stockVal = Integer.parseInt(modelo.getValueAt(row, 5).toString());
            } catch (Exception ignored) {
            }
            String estado = modelo.getValueAt(row, 7).toString();
            if (sel) {
                lbl.setBackground(new Color(184, 207, 229));
            } else if ("INACTIVO".equals(estado)) {
                lbl.setBackground(new Color(255, 218, 218));
                lbl.setForeground(new Color(140, 40, 40));
            } else if (stockVal <= 5) {
                lbl.setBackground(new Color(255, 245, 190));
                if (col == 5) {
                    lbl.setForeground(new Color(160, 80, 0));
                    lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
                }
            } else {
                lbl.setBackground(row % 2 == 0 ? new Color(245, 250, 245) : Color.WHITE);
            }
            return lbl;
        });

        sorter = new TableRowSorter<>(modelo);
        tabla.setRowSorter(sorter);

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 200)));
        root.add(scroll, BorderLayout.CENTER);

        // ---- SOUTH: leyenda + botones (BoxLayout vertical) ----
        JPanel panelSouth = new JPanel();
        panelSouth.setLayout(new BoxLayout(panelSouth, BoxLayout.Y_AXIS));
        panelSouth.setBackground(Color.WHITE);
        panelSouth.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        // Leyenda
        JPanel leyenda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        leyenda.setBackground(Color.WHITE);
        leyenda.setAlignmentX(Component.LEFT_ALIGNMENT);
        leyenda.add(pastilla(new Color(255, 245, 190), "Stock <= 5 unidades"));
        leyenda.add(pastilla(new Color(255, 218, 218), "Producto inactivo"));
        panelSouth.add(leyenda);

        // Separador
        panelSouth.add(Box.createVerticalStrut(4));
        JSeparator sep = new JSeparator();
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panelSouth.add(sep);
        panelSouth.add(Box.createVerticalStrut(6));

        // Fila de botones
        JPanel filaBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filaBotones.setBackground(Color.WHITE);
        filaBotones.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btnRecargar = new JButton("Recargar");
        UIUtils.estilizarBoton(btnRecargar, new Color(80, 80, 80));
        btnRecargar.setPreferredSize(new Dimension(110, 36));
        filaBotones.add(btnRecargar);

        if (esAdmin) {
            btnAgregar = new JButton("+ Agregar");
            btnEditar = new JButton("Editar");
            btnAjustarStock = new JButton("Ajustar stock");
            btnEliminar = new JButton("Activar / Desactivar");

            Dimension dimBtn = new Dimension(150, 36);
            UIUtils.estilizarBoton(btnAgregar, new Color(30, 130, 76));
            UIUtils.estilizarBoton(btnEditar, new Color(34, 85, 153));
            UIUtils.estilizarBoton(btnAjustarStock, new Color(140, 80, 10));
            UIUtils.estilizarBoton(btnEliminar, new Color(160, 40, 40));

            btnAgregar.setPreferredSize(dimBtn);
            btnEditar.setPreferredSize(dimBtn);
            btnAjustarStock.setPreferredSize(dimBtn);
            btnEliminar.setPreferredSize(new Dimension(170, 36));

            btnEditar.setEnabled(false);
            btnAjustarStock.setEnabled(false);
            btnEliminar.setEnabled(false);

            // Habilitar al seleccionar fila
            tabla.getSelectionModel().addListSelectionListener(e -> {
                boolean sel = tabla.getSelectedRow() >= 0;
                btnEditar.setEnabled(sel);
                btnAjustarStock.setEnabled(sel);
                btnEliminar.setEnabled(sel);
            });

            // Doble clic = editar
            tabla.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && tabla.getSelectedRow() >= 0)
                        editarSeleccionado();
                }
            });

            btnAgregar.addActionListener(e -> abrirDialogoProducto(null));
            btnEditar.addActionListener(e -> editarSeleccionado());
            btnAjustarStock.addActionListener(e -> ajustarStock());
            btnEliminar.addActionListener(e -> toggleActivoSeleccionado());

            filaBotones.add(btnAgregar);
            filaBotones.add(btnEditar);
            filaBotones.add(btnAjustarStock);
            filaBotones.add(btnEliminar);
        }

        panelSouth.add(filaBotones);
        root.add(panelSouth, BorderLayout.SOUTH);

        add(root);

        // Filtro en tiempo real
        txtFiltro.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                String txt = txtFiltro.getText().trim();
                sorter.setRowFilter(txt.isEmpty() ? null : RowFilter.regexFilter("(?i)" + txt, 0, 1));
            }
        });

        btnRecargar.addActionListener(e -> recargar());
        cargarDatos();
    }

    // =====================================================================
    // DATOS
    // =====================================================================
    private void recargar() {
        try {
            sistema.getInventario().cargarDesdeDB();
            cargarDatos();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al recargar: " + ex.getMessage());
        }
    }

    private void cargarDatos() {
        modelo.setRowCount(0);
        List<Producto> lista = new ArrayList<>(sistema.getInventario().getTodos().values());
        lista.sort(Comparator.comparing(Producto::getNombre));
        for (Producto p : lista)
            modelo.addRow(new Object[] {
                    p.getId(), p.getNombre(),
                    String.format("$%,.0f", p.getPrecio()),
                    String.format("%.0f%%", p.getImpuesto() * 100),
                    p.getCategoria(), p.getStock(),
                    String.format("$%,.0f", p.getPrecioConIva()),
                    p.isActivo() ? "ACTIVO" : "INACTIVO"
            });
    }

    // =====================================================================
    // DIALOGO AGREGAR / EDITAR
    // =====================================================================
    private void abrirDialogoProducto(Producto prod) {
        boolean nuevo = (prod == null);
        JDialog dlg = new JDialog(this, nuevo ? "Agregar producto" : "Editar producto", true);
        dlg.setSize(430, 370);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));
        panel.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 6, 4);
        g.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtCod = campo(nuevo ? "" : prod.getId());
        JTextField txtNom = campo(nuevo ? "" : prod.getNombre());
        JTextField txtPrecio = campo(nuevo ? "" : String.format("%.0f", prod.getPrecio()));
        JTextField txtStock = campo(nuevo ? "0" : String.valueOf(prod.getStock()));

        txtCod.setEditable(nuevo);
        if (!nuevo)
            txtCod.setBackground(new Color(235, 235, 235));

        Map<Integer, String> cats = cargarCategorias();
        JComboBox<String> cmbCat = new JComboBox<>(cats.values().toArray(new String[0]));
        if (!nuevo) {
            int i = 0;
            for (String v : cats.values()) {
                if (v.equals(prod.getCategoria())) {
                    cmbCat.setSelectedIndex(i);
                    break;
                }
                i++;
            }
        }

        JComboBox<String> cmbIva = new JComboBox<>(new String[] { "0%  - Exento", "5%", "19%" });
        if (!nuevo) {
            if (prod.getImpuesto() >= 0.19)
                cmbIva.setSelectedIndex(2);
            else if (prod.getImpuesto() >= 0.05)
                cmbIva.setSelectedIndex(1);
            else
                cmbIva.setSelectedIndex(0);
        }

        String[] etiq = { "Codigo:", "Nombre:", "Precio ($):", "Stock:", "Categoria:", "IVA:" };
        Component[] ctrl = { txtCod, txtNom, txtPrecio, txtStock, cmbCat, cmbIva };
        for (int i = 0; i < etiq.length; i++) {
            g.gridx = 0;
            g.gridy = i;
            g.weightx = 0.35;
            panel.add(new JLabel(etiq[i]), g);
            g.gridx = 1;
            g.weightx = 0.65;
            panel.add(ctrl[i], g);
        }

        JLabel lblErr = new JLabel(" ", SwingConstants.CENTER);
        lblErr.setForeground(Color.RED);
        lblErr.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        g.gridx = 0;
        g.gridy = etiq.length;
        g.gridwidth = 2;
        panel.add(lblErr, g);

        JPanel bp = new JPanel(new GridLayout(1, 2, 10, 0));
        bp.setBackground(Color.WHITE);
        JButton btnOk = new JButton(nuevo ? "Agregar" : "Guardar cambios");
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
            String cod = txtCod.getText().trim().toUpperCase();
            String nom = txtNom.getText().trim();
            String prS = txtPrecio.getText().trim().replace(",", "").replace("$", "");
            String stS = txtStock.getText().trim();

            if (cod.isEmpty() || nom.isEmpty() || prS.isEmpty()) {
                lblErr.setText("Codigo, nombre y precio son obligatorios.");
                return;
            }
            double pr;
            int st;
            try {
                pr = Double.parseDouble(prS);
            } catch (NumberFormatException ex) {
                lblErr.setText("Precio invalido.");
                return;
            }
            try {
                st = Integer.parseInt(stS);
            } catch (NumberFormatException ex) {
                lblErr.setText("Stock invalido.");
                return;
            }
            if (pr <= 0) {
                lblErr.setText("El precio debe ser mayor a 0.");
                return;
            }
            if (st < 0) {
                lblErr.setText("El stock no puede ser negativo.");
                return;
            }

            String catNom = (String) cmbCat.getSelectedItem();
            int idCat = cats.entrySet().stream()
                    .filter(en -> en.getValue().equals(catNom))
                    .mapToInt(Map.Entry::getKey).findFirst().orElse(1);

            try {
                if (nuevo)
                    insertarProducto(cod, nom, pr, st, idCat);
                else
                    actualizarProducto(prod.getId(), nom, pr, st, idCat);
                recargar();
                dlg.dispose();
                JOptionPane.showMessageDialog(this,
                        nuevo ? "Producto \"" + nom + "\" agregado correctamente."
                                : "Producto \"" + nom + "\" actualizado correctamente.",
                        "Exito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                lblErr.setText("Error: " + ex.getMessage());
            }
        });

        dlg.setVisible(true);
    }

    // =====================================================================
    // DIALOGO AJUSTAR STOCK
    // =====================================================================
    private void ajustarStock() {
        int fila = tabla.convertRowIndexToModel(tabla.getSelectedRow());
        if (fila < 0)
            return;
        String id = (String) modelo.getValueAt(fila, 0);
        String nombre = (String) modelo.getValueAt(fila, 1);
        int actual = (int) modelo.getValueAt(fila, 5);

        JDialog dlg = new JDialog(this, "Ajustar stock — " + nombre, true);
        dlg.setSize(340, 240);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        panel.setBackground(Color.WHITE);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 6, 4);
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblAct = new JLabel("Stock actual: " + actual + " unidades");
        lblAct.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblAct.setForeground(new Color(34, 85, 153));

        JComboBox<String> cmbOp = new JComboBox<>(new String[] {
                "Establecer cantidad exacta", "Agregar unidades", "Restar unidades" });
        JTextField txtCant = campo("0");
        JLabel lblErr = new JLabel(" ", SwingConstants.CENTER);
        lblErr.setForeground(Color.RED);

        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        panel.add(lblAct, g);
        g.gridy = 1;
        panel.add(cmbOp, g);
        g.gridwidth = 1;
        g.gridy = 2;
        g.gridx = 0;
        g.weightx = 0.4;
        panel.add(new JLabel("Cantidad:"), g);
        g.gridx = 1;
        g.weightx = 0.6;
        panel.add(txtCant, g);
        g.gridx = 0;
        g.gridy = 3;
        g.gridwidth = 2;
        panel.add(lblErr, g);

        JPanel bp = new JPanel(new GridLayout(1, 2, 8, 0));
        bp.setBackground(Color.WHITE);
        JButton btnOk = new JButton("Aplicar");
        JButton btnCan = new JButton("Cancelar");
        UIUtils.estilizarBoton(btnOk, new Color(140, 80, 10));
        UIUtils.estilizarBoton(btnCan, new Color(120, 120, 120));
        btnOk.setPreferredSize(new Dimension(0, 36));
        btnCan.setPreferredSize(new Dimension(0, 36));
        bp.add(btnOk);
        bp.add(btnCan);
        g.gridy = 4;
        panel.add(bp, g);

        dlg.add(panel);
        btnCan.addActionListener(e -> dlg.dispose());

        btnOk.addActionListener(e -> {
            int cant;
            try {
                cant = Integer.parseInt(txtCant.getText().trim());
            } catch (NumberFormatException ex) {
                lblErr.setText("Cantidad invalida.");
                return;
            }
            if (cant < 0) {
                lblErr.setText("Ingrese un numero positivo.");
                return;
            }

            int nuevo = switch (cmbOp.getSelectedIndex()) {
                case 0 -> cant;
                case 1 -> actual + cant;
                case 2 -> Math.max(0, actual - cant);
                default -> actual;
            };
            try {
                PreparedStatement ps = ConexionDB.getConexion()
                        .prepareStatement("UPDATE productos SET stock=? WHERE id_producto=?");
                ps.setInt(1, nuevo);
                ps.setString(2, id);
                ps.executeUpdate();
                recargar();
                dlg.dispose();
                JOptionPane.showMessageDialog(this,
                        "Stock de \"" + nombre + "\" actualizado: " + actual + " → " + nuevo,
                        "Exito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                lblErr.setText("Error: " + ex.getMessage());
            }
        });

        dlg.setVisible(true);
    }

    // =====================================================================
    // ACTIVAR / DESACTIVAR
    // =====================================================================
    private void toggleActivoSeleccionado() {
        int fila = tabla.convertRowIndexToModel(tabla.getSelectedRow());
        if (fila < 0)
            return;
        String id = (String) modelo.getValueAt(fila, 0);
        String nombre = (String) modelo.getValueAt(fila, 1);
        boolean activo = "ACTIVO".equals(modelo.getValueAt(fila, 7));
        String accion = activo ? "desactivar" : "reactivar";

        int r = JOptionPane.showConfirmDialog(this,
                "¿Desea " + accion + " el producto:\n\"" + nombre + "\"?\n\n" +
                        (activo ? "No aparecera en nuevas ventas."
                                : "Volvera a estar disponible para ventas."),
                "Confirmar " + accion,
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION)
            return;

        try {
            PreparedStatement ps = ConexionDB.getConexion()
                    .prepareStatement("UPDATE productos SET activo=? WHERE id_producto=?");
            ps.setInt(1, activo ? 0 : 1);
            ps.setString(2, id);
            ps.executeUpdate();
            recargar();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editarSeleccionado() {
        int fila = tabla.convertRowIndexToModel(tabla.getSelectedRow());
        if (fila < 0)
            return;
        Producto p = sistema.getInventario().buscarProducto(
                (String) modelo.getValueAt(fila, 0));
        if (p != null)
            abrirDialogoProducto(p);
    }

    // =====================================================================
    // OPERACIONES BD
    // =====================================================================
    private void insertarProducto(String id, String nombre, double precio,
            int stock, int idCat) throws Exception {
        ResultSet rs = ConexionDB.getConexion().createStatement()
                .executeQuery("SELECT id_producto FROM supermercado.productos WHERE id_producto='" + id + "'");
        if (rs.next())
            throw new Exception("Ya existe el codigo " + id);

        PreparedStatement ps = ConexionDB.getConexion().prepareStatement(
                "INSERT INTO supermercado.productos (id_producto,nombre,precio,stock,id_categoria,activo) " +
                        "VALUES (?,?,?,?,?,true)");
        ps.setString(1, id);
        ps.setString(2, nombre);
        ps.setDouble(3, precio);
        ps.setInt(4, stock);
        ps.setInt(5, idCat);
        ps.executeUpdate();
    }

    private void actualizarProducto(String id, String nombre, double precio,
            int stock, int idCat) throws Exception {
        PreparedStatement ps = ConexionDB.getConexion().prepareStatement(
                "UPDATE supermercado.productos SET nombre=?,precio=?,stock=?,id_categoria=? WHERE id_producto=?");
        ps.setString(1, nombre);
        ps.setDouble(2, precio);
        ps.setInt(3, stock);
        ps.setInt(4, idCat);
        ps.setString(5, id);
        ps.executeUpdate();
    }

    private Map<Integer, String> cargarCategorias() {
        Map<Integer, String> cats = new LinkedHashMap<>();
        try {
            ResultSet rs = ConexionDB.getConexion().createStatement()
                    .executeQuery("SELECT id_categoria, nombre FROM supermercado.categorias ORDER BY nombre");
            while (rs.next())
                cats.put(rs.getInt(1), rs.getString(2));
        } catch (Exception ex) {
            cats.put(1, "General");
        }
        return cats;
    }

    // =====================================================================
    // UTILIDADES
    // =====================================================================
    private JTextField campo(String v) {
        JTextField t = new JTextField(v, 16);
        UIUtils.estilizarCampo(t);
        return t;
    }

    private JLabel pastilla(Color bg, String txt) {
        JLabel l = new JLabel("  " + txt + "  ");
        l.setOpaque(true);
        l.setBackground(bg);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        l.setBorder(BorderFactory.createLineBorder(bg.darker(), 1));
        return l;
    }
}
