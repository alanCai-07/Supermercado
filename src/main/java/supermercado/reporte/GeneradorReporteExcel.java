package supermercado.reporte;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import supermercado.dao.FacturaDAO;

public class GeneradorReporteExcel {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Asegura que exista el directorio de destino. */
    private static void crearDirectorio(String ruta) {
        File dir = new File(ruta).getParentFile();
        if (dir != null && !dir.exists())
            dir.mkdirs();
    }

    // =========================================================
    // REPORTE 1: Ventas diarias
    // =========================================================
    public static String reporteVentasDiariasExcel(LocalDate fecha, FacturaDAO dao) throws Exception {
        String ruta = "reportes/excel/ventas_" + fecha + ".xlsx";
        crearDirectorio(ruta);

        List<String[]> filas = dao.ventasDia(fecha);
        double totalDia = dao.totalDia(fecha);

        String[] cols = { "N° Factura", "Hora", "Cliente", "Cajero", "Total", "Metodo pago" };

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Ventas del dia");

            CellStyle estiloTitulo = estiloTitulo(wb);
            CellStyle estiloHeader = estiloHeader(wb);
            CellStyle estiloTotal = estiloTotal(wb);

            int fila = 0;
            Row rTitulo = sheet.createRow(fila++);
            crearCelda(rTitulo, 0, "REPORTE DE VENTAS DIARIAS - " + fecha.format(FMT), estiloTitulo);
            fila++;

            Row rHead = sheet.createRow(fila++);
            for (int i = 0; i < cols.length; i++)
                crearCelda(rHead, i, cols[i], estiloHeader);

            for (String[] f : filas) {
                Row r = sheet.createRow(fila++);
                for (int i = 0; i < f.length; i++)
                    crearCelda(r, i, f[i], null);
            }

            fila++;
            Row rTotal = sheet.createRow(fila);
            crearCelda(rTotal, 0, "Total transacciones: " + filas.size(), estiloTotal);
            crearCelda(rTotal, 3, "Total recaudado:", estiloTotal);
            crearCelda(rTotal, 4, String.format("$%,.0f", totalDia), estiloTotal);

            for (int i = 0; i < cols.length; i++)
                sheet.autoSizeColumn(i);

            try (FileOutputStream fos = new FileOutputStream(ruta)) {
                wb.write(fos);
            }
        }
        System.out.println("[EXCEL] Reporte diario generado: " + ruta);
        return ruta;
    }

    // =========================================================
    // REPORTE 2: Top productos vendidos
    // =========================================================
    public static String reporteTopProductosExcel(LocalDate desde, LocalDate hasta, FacturaDAO dao) throws Exception {
        String ruta = "reportes/excel/top_productos_" + desde + "_" + hasta + ".xlsx";
        crearDirectorio(ruta);

        List<String[]> filas = dao.topProductos(desde, hasta);
        String[] cols = { "#", "Producto", "Unidades", "Ingresos netos", "Total c/IVA" };

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Top productos");

            CellStyle estiloTitulo = estiloTitulo(wb);
            CellStyle estiloHeader = estiloHeader(wb);

            int fila = 0;
            Row rTitulo = sheet.createRow(fila++);
            crearCelda(rTitulo, 0, "TOP PRODUCTOS MAS VENDIDOS  (" + desde.format(FMT) + " - " + hasta.format(FMT) + ")",
                    estiloTitulo);
            fila++;

            Row rHead = sheet.createRow(fila++);
            for (int i = 0; i < cols.length; i++)
                crearCelda(rHead, i, cols[i], estiloHeader);

            for (String[] f : filas) {
                Row r = sheet.createRow(fila++);
                for (int i = 0; i < f.length; i++)
                    crearCelda(r, i, f[i], null);
            }

            for (int i = 0; i < cols.length; i++)
                sheet.autoSizeColumn(i);

            try (FileOutputStream fos = new FileOutputStream(ruta)) {
                wb.write(fos);
            }
        }
        System.out.println("[EXCEL] Reporte top productos generado: " + ruta);
        return ruta;
    }

    // =========================================================
    // REPORTE 3: Ventas por cajero
    // =========================================================
    public static String reporteVentasPorCajeroExcel(LocalDate desde, LocalDate hasta, FacturaDAO dao) throws Exception {
        String ruta = "reportes/excel/ventas_cajero_" + desde + "_" + hasta + ".xlsx";
        crearDirectorio(ruta);

        List<String[]> filas = dao.ventasPorCajero(desde, hasta);
        String[] cols = { "Cajero", "N° Facturas", "Total vendido" };

        double granTotal = 0;
        for (String[] f : filas)
            granTotal += Double.parseDouble(f[2].replace(",", ""));

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Ventas por cajero");

            CellStyle estiloTitulo = estiloTitulo(wb);
            CellStyle estiloHeader = estiloHeader(wb);
            CellStyle estiloTotal = estiloTotal(wb);

            int fila = 0;
            Row rTitulo = sheet.createRow(fila++);
            crearCelda(rTitulo, 0, "VENTAS POR CAJERO  (" + desde.format(FMT) + " - " + hasta.format(FMT) + ")",
                    estiloTitulo);
            fila++;

            Row rHead = sheet.createRow(fila++);
            for (int i = 0; i < cols.length; i++)
                crearCelda(rHead, i, cols[i], estiloHeader);

            for (String[] f : filas) {
                Row r = sheet.createRow(fila++);
                for (int i = 0; i < f.length; i++)
                    crearCelda(r, i, f[i], null);
            }

            fila++;
            Row rTotal = sheet.createRow(fila);
            crearCelda(rTotal, 0, "Cajeros activos: " + filas.size(), estiloTotal);
            crearCelda(rTotal, 1, "Gran total:", estiloTotal);
            crearCelda(rTotal, 2, String.format("$%,.0f", granTotal), estiloTotal);

            for (int i = 0; i < cols.length; i++)
                sheet.autoSizeColumn(i);

            try (FileOutputStream fos = new FileOutputStream(ruta)) {
                wb.write(fos);
            }
        }
        System.out.println("[EXCEL] Reporte por cajero generado: " + ruta);
        return ruta;
    }

    // =========================================================
    // UTILIDADES DE ESTILO
    // =========================================================
    private static void crearCelda(Row row, int col, String valor, CellStyle estilo) {
        Cell c = row.createCell(col);
        c.setCellValue(valor == null ? "" : valor);
        if (estilo != null)
            c.setCellStyle(estilo);
    }

    private static CellStyle estiloTitulo(Workbook wb) {
        Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 13);
        CellStyle s = wb.createCellStyle();
        s.setFont(f);
        return s;
    }

    private static CellStyle estiloHeader(Workbook wb) {
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        CellStyle s = wb.createCellStyle();
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private static CellStyle estiloTotal(Workbook wb) {
        Font f = wb.createFont();
        f.setBold(true);
        CellStyle s = wb.createCellStyle();
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }
}
