package supermercado.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ImportadorProductosExcelTest {

    @Test
    void deberiaLeerProductosDesdeHojaExcel() throws Exception {
        File archivo = File.createTempFile("productos-importar", ".xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Productos");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("codigo");
            header.createCell(1).setCellValue("nombre");
            header.createCell(2).setCellValue("precio");
            header.createCell(3).setCellValue("stock");
            header.createCell(4).setCellValue("categoria");
            header.createCell(5).setCellValue("iva");

            Row fila = sheet.createRow(1);
            fila.createCell(0).setCellValue("P001");
            fila.createCell(1).setCellValue("Arroz");
            fila.createCell(2).setCellValue(3500);
            fila.createCell(3).setCellValue(10);
            fila.createCell(4).setCellValue("Abarrotes");
            fila.createCell(5).setCellValue(0.19);

            try (OutputStream out = new FileOutputStream(archivo)) {
                workbook.write(out);
            }
        }

        try {
            ImportadorProductosExcel importador = new ImportadorProductosExcel();
            List<ImportadorProductosExcel.ProductoImportado> productos = importador.leerProductos(archivo);

            assertEquals(1, productos.size());
            ImportadorProductosExcel.ProductoImportado producto = productos.get(0);
            assertEquals("P001", producto.codigo());
            assertEquals("Arroz", producto.nombre());
            assertEquals(3500.0, producto.precio(), 0.001);
            assertEquals(10, producto.stock());
            assertEquals("Abarrotes", producto.categoria());
            assertEquals(0.19, producto.impuesto(), 0.001);
        } finally {
            archivo.delete();
        }
    }
}
