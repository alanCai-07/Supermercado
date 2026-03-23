package supermercado.servicio;

import supermercado.db.ConexionDB;
import supermercado.modelo.Producto;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Inventario {

    private Map<String, Producto> productos = new HashMap<>();

    // ---- Cargar todos los productos desde MySQL ----
    public void cargarDesdeDB() throws SQLException {
        productos.clear();
        String sql = """
            SELECT p.id_producto, p.nombre, p.precio, p.stock,
                   c.nombre AS categoria, c.impuesto
            FROM productos p
            JOIN categorias c ON p.id_categoria = c.id_categoria
            WHERE p.activo = 1
            """;
        try (Statement st = ConexionDB.getConexion().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Producto p = new Producto(
                        rs.getString("id_producto"),
                        rs.getString("nombre"),
                        rs.getDouble("precio"),
                        rs.getString("categoria"),
                        rs.getDouble("impuesto"),
                        rs.getInt("stock"));
                productos.put(p.getId(), p);
            }
        }
        System.out.println("[Inventario] " + productos.size() + " productos cargados.");
    }

    // ---- Buscar por ID ----
    public Producto buscarProducto(String id) {
        return productos.get(id);
    }

    // ---- Buscar por nombre (contiene) ----
    public List<Producto> buscarPorNombre(String texto) {
        List<Producto> resultado = new ArrayList<>();
        String lower = texto.toLowerCase();
        for (Producto p : productos.values())
            if (p.getNombre().toLowerCase().contains(lower))
                resultado.add(p);
        return resultado;
    }

    public boolean hayDisponible(String idProducto, int cantidad) {
        Producto p = productos.get(idProducto);
        return p != null && p.getStock() >= cantidad;
    }

    public int getStock(String idProducto) {
        Producto p = productos.get(idProducto);
        return p == null ? 0 : p.getStock();
    }

    public void agregarProducto(Producto p) {
        productos.put(p.getId(), p);
    }

    public Map<String, Producto> getTodos() {
        return new HashMap<>(productos);
    }

    // ---- Actualizar stock en memoria (la DB lo actualiza el DAO) ----
    public void descontarStock(String idProducto, int cantidad) {
        Producto p = productos.get(idProducto);
        if (p != null) p.setStock(p.getStock() - cantidad);
    }
}
