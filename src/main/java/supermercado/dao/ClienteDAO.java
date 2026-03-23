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
