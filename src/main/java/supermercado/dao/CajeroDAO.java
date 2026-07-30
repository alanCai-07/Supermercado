package supermercado.dao;

import supermercado.db.ConexionDB;
import supermercado.modelo.Cajero;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CajeroDAO {

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el hash de la contrasena.", e);
        }
    }

    // ---- Buscar por ID (uso interno) ----
    public Cajero buscar(String id) throws SQLException {
        String sql = "SELECT * FROM supermercado.cajeros WHERE id_cajero = ? AND activo = TRUE";
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapear(rs);
        }
        return null;
    }

    // ---- Buscar por nombre (para el login) ----
    public Cajero buscarPorNombre(String nombre) throws SQLException {
        String sql = "SELECT * FROM supermercado.cajeros WHERE LOWER(nombre) = LOWER(?) AND activo = TRUE";
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapear(rs);
        }
        return null;
    }

    // ---- Autenticar por NOMBRE + contrasena ----
    public Cajero autenticar(String nombre, String contrasena) throws SQLException {
        Cajero cajero = buscarPorNombre(nombre);
        if (cajero != null && cajero.autenticar(contrasena))
            return cajero;
        return null;
    }

    // ---- Listar todos los cajeros activos (para el combo del login) ----
    public List<String> listarNombres() throws SQLException {
        List<String> nombres = new ArrayList<>();
        String sql = "SELECT nombre FROM supermercado.cajeros WHERE activo = TRUE ORDER BY nombre";
        try (Statement st = ConexionDB.getConexion().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                nombres.add(rs.getString("nombre"));
        }
        return nombres;
    }

    public void guardar(Cajero c) throws SQLException {
        String sql = """
                INSERT INTO supermercado.cajeros (id_cajero, nombre, turno, contrasena_hash, rol, activo)
                VALUES (?, ?, ?, ?, ?, TRUE)
                ON CONFLICT (id_cajero) DO UPDATE
                  SET nombre = EXCLUDED.nombre,
                      turno  = EXCLUDED.turno,
                      contrasena_hash = EXCLUDED.contrasena_hash,
                      rol    = EXCLUDED.rol,
                      activo = TRUE
                """;
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setString(1, c.getId());
            ps.setString(2, c.getNombre());
            ps.setString(3, c.getTurno());
            ps.setString(4, c.getContrasenaHash());
            ps.setString(5, c.getRol());
            ps.executeUpdate();
        }
    }

    public boolean existePorNombre(String nombre) throws SQLException {
        String sql = "SELECT 1 FROM supermercado.cajeros WHERE LOWER(nombre) = LOWER(?) AND activo = TRUE LIMIT 1";
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existePorId(String id) throws SQLException {
        String sql = "SELECT 1 FROM supermercado.cajeros WHERE LOWER(id_cajero) = LOWER(?) AND activo = TRUE LIMIT 1";
        try (PreparedStatement ps = ConexionDB.getConexion().prepareStatement(sql)) {
            ps.setString(1, id.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ---- Mapear ResultSet a Cajero ----
    private Cajero mapear(ResultSet rs) throws SQLException {
        return new Cajero(
                rs.getString("id_cajero"),
                rs.getString("nombre"),
                rs.getString("turno"),
                rs.getString("contrasena_hash"),
                rs.getString("rol"));
    }
}
