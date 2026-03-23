package supermercado.dao;

import supermercado.db.ConexionDB;
import supermercado.modelo.Cajero;

import java.sql.*;

public class CajeroDAO {

    public Cajero buscar(String id) throws SQLException {
        String sql = "SELECT * FROM cajeros WHERE id_cajero = ? AND activo = 1";
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return new Cajero(
                    rs.getString("id_cajero"),
                    rs.getString("nombre"),
                    rs.getString("turno"),
                    rs.getString("contrasena_hash"),
                    rs.getString("rol"));
        }
        return null;
    }

    /**
     * Autentica por id y contrasena en texto plano.
     * Compara SHA-256 del texto ingresado contra el hash en BD.
     */
    public Cajero autenticar(String id, String contrasena) throws SQLException {
        Cajero cajero = buscar(id);
        if (cajero != null && cajero.autenticar(contrasena)) return cajero;
        return null;
    }

    public void guardar(Cajero c) throws SQLException {
        String sql = """
            INSERT INTO cajeros (id_cajero, nombre, turno, contrasena_hash)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE nombre=VALUES(nombre), turno=VALUES(turno)
            """;
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setString(1, c.getId());
            ps.setString(2, c.getNombre());
            ps.setString(3, c.getTurno());
            ps.setString(4, c.getContrasenaHash());
            ps.executeUpdate();
        }
    }
}
