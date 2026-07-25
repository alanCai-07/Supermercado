package supermercado.servicio;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import supermercado.db.ConexionDB;

public class ImportadorProductosExcel {

    public record ProductoImportado(
            String codigo,
            String nombre,
            double precio,
            int stock,
            String categoria,
            double impuesto) {
    }

    public List<ProductoImportado> leerProductos(File archivo) throws Exception {
        if (archivo == null || !archivo.exists()) {
            throw new IllegalArgumentException("No se encontró el archivo seleccionado.");
        }

        List<ProductoImportado> productos = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (FileInputStream input = new FileInputStream(archivo);
                Workbook workbook = WorkbookFactory.create(input)) {

            Sheet hoja = workbook.getSheetAt(0);
            if (hoja == null) {
                return productos;
            }

            for (int filaIndex = 1; filaIndex <= hoja.getLastRowNum(); filaIndex++) {
                Row fila = hoja.getRow(filaIndex);
                if (fila == null) {
                    continue;
                }

                String codigo = formatter.formatCellValue(fila.getCell(0)).trim();
                String nombre = formatter.formatCellValue(fila.getCell(1)).trim();
                if (codigo.isBlank() && nombre.isBlank()) {
                    continue;
                }

                productos.add(new ProductoImportado(
                        codigo.toUpperCase(),
                        nombre,
                        parseDouble(formatter.formatCellValue(fila.getCell(2))),
                        parseInt(formatter.formatCellValue(fila.getCell(3))),
                        formatter.formatCellValue(fila.getCell(4)).trim().isBlank()
                                ? "General"
                                : formatter.formatCellValue(fila.getCell(4)).trim(),
                        parseImpuesto(formatter.formatCellValue(fila.getCell(5)))));
            }
        }

        return productos;
    }

    public int importar(File archivo) throws Exception {
        List<ProductoImportado> productos = leerProductos(archivo);
        if (productos.isEmpty()) {
            return 0;
        }

        try (Connection conn = ConexionDB.getConexion()) {
            conn.setAutoCommit(false);
            int importados = 0;
            try {
                for (ProductoImportado producto : productos) {
                    if (producto.codigo().isBlank() || producto.nombre().isBlank()) {
                        continue;
                    }
                    int idCategoria = buscarOCrearCategoria(conn, producto.categoria(), producto.impuesto());
                    insertarOActualizar(conn, producto, idCategoria);
                    importados++;
                }
                conn.commit();
                return importados;
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        }
    }

    private void insertarOActualizar(Connection conn, ProductoImportado producto, int idCategoria) throws SQLException {
        String codigo = producto.codigo().trim();
        String nombre = producto.nombre().trim();

        try (PreparedStatement existe = conn.prepareStatement(
                "SELECT id_producto FROM supermercado.productos WHERE id_producto = ?")) {
            existe.setString(1, codigo);
            try (ResultSet rs = existe.executeQuery()) {
                if (rs.next()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE supermercado.productos SET nombre = ?, precio = ?, stock = ?, id_categoria = ?, activo = TRUE WHERE id_producto = ?")) {
                        ps.setString(1, nombre);
                        ps.setDouble(2, producto.precio());
                        ps.setInt(3, producto.stock());
                        ps.setInt(4, idCategoria);
                        ps.setString(5, codigo);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO supermercado.productos (id_producto, nombre, precio, stock, id_categoria, activo) VALUES (?, ?, ?, ?, ?, TRUE)")) {
                        ps.setString(1, codigo);
                        ps.setString(2, nombre);
                        ps.setDouble(3, producto.precio());
                        ps.setInt(4, producto.stock());
                        ps.setInt(5, idCategoria);
                        ps.executeUpdate();
                    }
                }
            }
        }
    }

    private int buscarOCrearCategoria(Connection conn, String categoria, double impuesto) throws SQLException {
        String nombreCategoria = categoria == null || categoria.isBlank() ? "General" : categoria.trim();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id_categoria FROM supermercado.categorias WHERE LOWER(nombre) = LOWER(?)")) {
            ps.setString(1, nombreCategoria);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_categoria");
                }
            }
        }

        String sql = "INSERT INTO supermercado.categorias (nombre, impuesto) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombreCategoria);
            ps.setDouble(2, impuesto);
            ps.executeUpdate();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        }

        throw new SQLException("No se pudo crear la categoría " + nombreCategoria);
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.replace("$", "").replace(",", ""));
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private int parseInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.replace(".", ""));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private double parseImpuesto(String value) {
        double parsed = parseDouble(value);
        return parsed > 1 ? parsed / 100 : parsed;
    }
}
