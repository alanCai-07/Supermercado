package supermercado.dao;

import supermercado.db.ConexionDB;
import supermercado.modelo.Cliente;

import java.sql.*;

public class ClienteDAO {

    public void guardar(Cliente c) throws SQLException {
        String sql = """
            INSERT INTO clientes (nit, nombre, telefono, email, direccion)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
              nombre=VALUES(nombre), telefono=VALUES(telefono),
              email=VALUES(email),   direccion=VALUES(direccion)
            """;
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setString(1, c.getNit());
            ps.setString(2, c.getNombre());
            ps.setString(3, c.getTelefono());
            ps.setString(4, c.getEmail());
            ps.setString(5, c.getDireccion());
            ps.executeUpdate();
        }
    }

    public Cliente buscar(String nit) throws SQLException {
        String sql = "SELECT * FROM clientes WHERE nit = ?";
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setString(1, nit);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return new Cliente(
                    rs.getString("nit"),
                    rs.getString("nombre"),
                    rs.getString("telefono"),
                    rs.getString("email"),
                    rs.getString("direccion"));
        }
        return null;
    }

    /** Busca clientes cuyo nombre contenga el texto (insensible a mayusculas). */
    public java.util.List<Cliente> buscarPorNombre(String texto) throws SQLException {
        java.util.List<Cliente> lista = new java.util.ArrayList<>();
        String sql = "SELECT * FROM clientes WHERE LOWER(nombre) LIKE LOWER(?) " +
                     "AND nit != '222222222' ORDER BY nombre LIMIT 20";
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setString(1, "%" + texto + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                lista.add(new Cliente(
                    rs.getString("nit"),
                    rs.getString("nombre"),
                    rs.getString("telefono"),
                    rs.getString("email"),
                    rs.getString("direccion")));
        }
        return lista;
    }

    /**
     * Busqueda inteligente: si el texto es solo digitos busca por NIT exacto,
     * si contiene letras busca por nombre. Retorna lista de coincidencias.
     */
    public java.util.List<Cliente> buscarInteligente(String texto) throws SQLException {
        java.util.List<Cliente> lista = new java.util.ArrayList<>();
        if (texto == null || texto.isBlank()) return lista;

        if (texto.matches("\\d+")) {
            // Es numerico: buscar por NIT exacto primero
            Cliente c = buscar(texto);
            if (c != null) lista.add(c);
        } else {
            // Contiene letras: buscar por nombre
            lista = buscarPorNombre(texto);
        }
        return lista;
    }

    /** Devuelve el cliente "Consumidor Final" para ventas sin datos. */
    public Cliente consumidorFinal() throws SQLException {
        Cliente c = buscar("222222222");
        if (c == null) {
            c = new Cliente("222222222", "Consumidor Final", "", "", "");
            guardar(c);
        }
        return c;
    }
}
