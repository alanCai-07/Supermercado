package supermercado.dao;

import supermercado.db.ConexionDB;
import supermercado.modelo.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FacturaDAO {

    // ---- Guardar factura completa (cabecera + items + descontar stock) ----
    public void guardar(Factura factura) throws SQLException {
        Connection con = ConexionDB.getConexion();
        con.setAutoCommit(false); // transaccion atomica

        try {
            // 1. Insertar cabecera
            String sqlF = """
                    INSERT INTO supermercado.facturas
                      (numero_factura, fecha, nit_cliente, id_cajero,
                       estado, subtotal, iva, total, metodo_pago)
                    VALUES (?, NOW(), ?, ?,
                            ?::supermercado.estado_factura, ?, ?, ?,
                            ?::supermercado.metodo_pago)
                    """;
            try (PreparedStatement ps = con.prepareStatement(sqlF)) {
                ps.setString(1, factura.getNumero());
                ps.setString(2, factura.getCliente().getNit());
                ps.setString(3, factura.getCajero().getId());
                ps.setString(4, factura.getEstado().name());
                ps.setDouble(5, factura.calcularSubtotal());
                ps.setDouble(6, factura.calcularIva());
                ps.setDouble(7, factura.calcularTotal());
                ps.setString(8, factura.getMetodoPago());
                ps.executeUpdate();
            }

            // 2. Insertar items en batch
            String sqlI = """
                    INSERT INTO supermercado.items_factura
                      (numero_factura, id_producto, cantidad,
                       precio_unitario, subtotal_item, iva_item)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """;
            try (PreparedStatement ps = con.prepareStatement(sqlI)) {
                for (ItemFactura item : factura.getItems()) {
                    ps.setString(1, factura.getNumero());
                    ps.setString(2, item.getProducto().getId());
                    ps.setInt(3, item.getCantidad());
                    ps.setDouble(4, item.getPrecioUnitario());
                    ps.setDouble(5, item.getSubtotal());
                    ps.setDouble(6, item.getImpuesto());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // 3. Descontar stock de cada producto
            String sqlS = "UPDATE supermercado.productos SET stock = stock - ? WHERE id_producto = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlS)) {
                for (ItemFactura item : factura.getItems()) {
                    ps.setInt(1, item.getCantidad());
                    ps.setString(2, item.getProducto().getId());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            con.commit();
            System.out.println("[DAO] Factura " + factura.getNumero() + " guardada en BD.");

        } catch (SQLException e) {
            con.rollback();
            throw new SQLException("Error al guardar factura: " + e.getMessage(), e);
        } finally {
            con.setAutoCommit(true);
        }
    }

    // ---- Actualizar estado de una factura ----
    public void actualizarEstado(String numero, EstadoFactura estado) throws SQLException {
        String sql = "UPDATE supermercado.facturas SET estado = ?::supermercado.estado_factura WHERE numero_factura = ?";
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setString(1, estado.name());
            ps.setString(2, numero);
            ps.executeUpdate();
        }
    }

    // ---- Obtener siguiente numero de factura ----
    public String siguienteNumero() throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM supermercado.facturas";
        try (Statement st = ConexionDB.getConexion().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            if (rs.next())
                return String.format("FAC-%05d", rs.getInt("total") + 1);
        }
        return "FAC-00001";
    }

    // ---- Buscar por numero ----
    public Optional<Factura> buscarPorNumero(String numero,
            ClienteDAO clienteDAO, CajeroDAO cajeroDAO) throws SQLException {
        String sql = "SELECT * FROM supermercado.facturas WHERE numero_factura = ?";
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setString(1, numero);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Cliente c = clienteDAO.buscar(rs.getString("nit_cliente"));
                Cajero j = cajeroDAO.buscar(rs.getString("id_cajero"));
                Factura f = new Factura(numero, c, j);
                f.setMetodoPago(rs.getString("metodo_pago"));
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }

    // ---- Listar facturas del dia para reporte ----
    public List<String[]> ventasDia(java.time.LocalDate fecha) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        String sql = """
                SELECT f.numero_factura, TO_CHAR(f.fecha, 'HH24:MI') AS hora,
                       c.nombre AS cliente, ca.nombre AS cajero,
                       f.total, f.metodo_pago, f.estado
                FROM supermercado.facturas f
                JOIN supermercado.clientes c  ON f.nit_cliente = c.nit
                JOIN supermercado.cajeros  ca ON f.id_cajero   = ca.id_cajero
                WHERE DATE(f.fecha) = ? AND f.estado = 'PAGADA'
                ORDER BY f.fecha
                """;
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fecha));
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                filas.add(new String[] {
                        rs.getString("numero_factura"),
                        rs.getString("hora").substring(0, 5),
                        rs.getString("cliente"),
                        rs.getString("cajero"),
                        String.format("%,.0f", rs.getDouble("total")),
                        rs.getString("metodo_pago")
                });
        }
        return filas;
    }

    // ---- Totales del dia ----
    public double totalDia(java.time.LocalDate fecha) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total),0) FROM supermercado.facturas WHERE DATE(fecha)=? AND estado='PAGADA'";
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fecha));
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    // ---- Ventas por cajero en un rango de fechas ----
    public List<String[]> ventasPorCajero(java.time.LocalDate desde,
            java.time.LocalDate hasta) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        String sql = """
                SELECT ca.nombre, COUNT(*) AS facturas,
                       SUM(f.total) AS total_vendido
                FROM supermercado.facturas f
                JOIN supermercado.cajeros ca ON f.id_cajero = ca.id_cajero
                WHERE DATE(f.fecha) BETWEEN ? AND ? AND f.estado='PAGADA'
                GROUP BY ca.id_cajero, ca.nombre
                ORDER BY total_vendido DESC
                """;
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                filas.add(new String[] {
                        rs.getString("nombre"),
                        String.valueOf(rs.getInt("facturas")),
                        String.format("%,.0f", rs.getDouble("total_vendido"))
                });
        }
        return filas;
    }

    // ---- Top productos vendidos ----
    public List<String[]> topProductos(java.time.LocalDate desde,
            java.time.LocalDate hasta) throws SQLException {
        List<String[]> filas = new ArrayList<>();
        String sql = """
                SELECT p.id_producto, p.nombre,
                       SUM(i.cantidad)      AS unidades,
                       SUM(i.subtotal_item) AS ingresos,
                       SUM(i.iva_item)      AS iva,
                       SUM(i.subtotal_item + i.iva_item) AS total_con_iva
                FROM supermercado.items_factura i
                JOIN supermercado.productos p ON i.id_producto  = p.id_producto
                JOIN supermercado.facturas  f ON i.numero_factura = f.numero_factura
                WHERE DATE(f.fecha) BETWEEN ? AND ? AND f.estado='PAGADA'
                GROUP BY p.id_producto, p.nombre
                ORDER BY unidades DESC
                LIMIT 20
                """;
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            ResultSet rs = ps.executeQuery();
            int rank = 1;
            while (rs.next())
                filas.add(new String[] {
                        String.valueOf(rank++),
                        rs.getString("nombre"),
                        String.valueOf(rs.getInt("unidades")),
                        String.format("%,.0f", rs.getDouble("ingresos")),
                        String.format("%,.0f", rs.getDouble("total_con_iva"))
                });
        }
        return filas;
    }

    // ---- Listar TODAS las facturas (para pantalla de consulta) ----
    public java.util.List<String[]> listarTodas() throws SQLException {
        java.util.List<String[]> filas = new ArrayList<>();
        String sql = "SELECT f.numero_factura, " +
                "       TO_CHAR(f.fecha,'DD/MM/YYYY') AS fecha, " +
                "       TO_CHAR(f.fecha,'HH24:MI') AS hora, " +
                "       c.nombre AS cliente, c.nit, " +
                "       ca.nombre AS cajero, " +
                "       f.subtotal, f.iva, f.total, " +
                "       f.metodo_pago, f.estado " +
                " FROM supermercado.facturas f " +
                "JOIN supermercado.clientes c  ON f.nit_cliente = c.nit " +
                "JOIN supermercado.cajeros  ca ON f.id_cajero   = ca.id_cajero " +
                "ORDER BY f.fecha DESC";
        try (Statement st = ConexionDB.getConexion().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                filas.add(new String[] {
                        rs.getString("numero_factura"),
                        rs.getString("fecha"),
                        rs.getString("hora"),
                        rs.getString("cliente"),
                        rs.getString("nit"),
                        rs.getString("cajero"),
                        String.format("$%,.0f", rs.getDouble("subtotal")),
                        String.format("$%,.0f", rs.getDouble("iva")),
                        String.format("$%,.0f", rs.getDouble("total")),
                        rs.getString("metodo_pago"),
                        rs.getString("estado")
                });
        }
        return filas;
    }

    // ---- Items de una factura especifica ----
    public java.util.List<String[]> itemsDe(String numero) throws SQLException {
        java.util.List<String[]> filas = new ArrayList<>();
        String sql = "SELECT p.nombre, i.cantidad, i.precio_unitario, " +
                "       i.subtotal_item, i.iva_item, " +
                "       (i.subtotal_item + i.iva_item) AS total_item " +
                "FROM supermercado.items_factura i " +
                "JOIN supermercado.productos p ON i.id_producto = p.id_producto " +
                "WHERE i.numero_factura = ? ORDER BY p.nombre";
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setString(1, numero);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                filas.add(new String[] {
                        rs.getString("nombre"),
                        String.valueOf(rs.getInt("cantidad")),
                        String.format("$%,.0f", rs.getDouble("precio_unitario")),
                        String.format("$%,.0f", rs.getDouble("subtotal_item")),
                        String.format("$%,.0f", rs.getDouble("iva_item")),
                        String.format("$%,.0f", rs.getDouble("total_item"))
                });
        }
        return filas;
    }
}
