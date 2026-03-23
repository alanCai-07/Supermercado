package supermercado.reporte;

import com.itextpdf.kernel.colors.*;
import com.itextpdf.kernel.font.*;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.*;
import com.itextpdf.layout.borders.*;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;

import supermercado.dao.FacturaDAO;
import supermercado.modelo.Factura;
import supermercado.modelo.ItemFactura;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class GeneradorReportePDF {

        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // Colores corporativos
        private static final DeviceRgb COLOR_HEADER = new DeviceRgb(34, 85, 153); // azul oscuro
        private static final DeviceRgb COLOR_SUBHEAD = new DeviceRgb(70, 130, 180); // azul medio
        private static final DeviceRgb COLOR_ROW_PAR = new DeviceRgb(235, 242, 250); // azul muy claro
        private static final DeviceRgb COLOR_TOTAL = new DeviceRgb(220, 235, 255); // azul pastel
        private static final DeviceRgb COLOR_TEXT_H = new DeviceRgb(255, 255, 255); // texto blanco para encabezados

        private static final String EMPRESA_NOMBRE = "SUPERMERCADO EL EXITO";
        private static final String EMPRESA_NIT = "NIT: 900.123.456-7";
        private static final String EMPRESA_DIR = "Calle 5 # 20-30, Cali, Valle del Cauca";
        private static final String EMPRESA_TEL = "Tel: (602) 890-1234";

        /** Asegura que exista el directorio de destino. */
        private static void crearDirectorio(String ruta) {
                File dir = new File(ruta).getParentFile();
                if (dir != null && !dir.exists())
                        dir.mkdirs();
        }

        // =========================================================
        // REPORTE 1: Factura individual en PDF
        // =========================================================
        public static String generarFacturaPDF(Factura factura) throws Exception {
                String ruta = "reportes/facturas/" + factura.getNumero() + ".pdf";
                crearDirectorio(ruta);

                PdfWriter writer = new PdfWriter(ruta);
                PdfDocument pdf = new PdfDocument(writer);
                Document doc = new Document(pdf, PageSize.A4);
                doc.setMargins(36, 36, 36, 36);

                // ---- ENCABEZADO EMPRESA ----
                Table cabecera = new Table(new float[] { 3, 2 })
                                .setWidth(UnitValue.createPercentValue(100));

                Cell celdaEmpresa = new Cell()
                                .add(new Paragraph(EMPRESA_NOMBRE).setFontSize(16).setBold()
                                                .setFontColor(COLOR_HEADER))
                                .add(new Paragraph(EMPRESA_NIT).setFontSize(9))
                                .add(new Paragraph(EMPRESA_DIR).setFontSize(9))
                                .add(new Paragraph(EMPRESA_TEL).setFontSize(9))
                                .setBorder(Border.NO_BORDER);
                cabecera.addCell(celdaEmpresa);

                Cell celdaFact = new Cell()
                                .add(new Paragraph("FACTURA ELECTRONICA").setFontSize(13).setBold()
                                                .setFontColor(COLOR_HEADER)
                                                .setTextAlignment(TextAlignment.RIGHT))
                                .add(new Paragraph("N°: " + factura.getNumero()).setFontSize(10)
                                                .setTextAlignment(TextAlignment.RIGHT).setBold())
                                .add(new Paragraph("Fecha: " + factura.getFecha().format(FMT_HORA))
                                                .setFontSize(9).setTextAlignment(TextAlignment.RIGHT))
                                .add(new Paragraph("Estado: " + factura.getEstado())
                                                .setFontSize(9).setTextAlignment(TextAlignment.RIGHT))
                                .setBorder(Border.NO_BORDER);
                cabecera.addCell(celdaFact);
                doc.add(cabecera);

                // ---- SEPARADOR ----
                doc.add(lineaSeparadora());

                // ---- DATOS CLIENTE / CAJERO ----
                Table datosGrid = new Table(new float[] { 1, 1 })
                                .setWidth(UnitValue.createPercentValue(100));
                datosGrid.addCell(bloqueInfo("DATOS DEL CLIENTE",
                                "Nombre  : " + factura.getCliente().getNombre(),
                                "NIT/CC  : " + factura.getCliente().getNit(),
                                "Tel     : " + factura.getCliente().getTelefono()));
                datosGrid.addCell(bloqueInfo("DATOS DEL CAJERO",
                                "Nombre  : " + factura.getCajero().getNombre(),
                                "Turno   : " + factura.getCajero().getTurno(),
                                "Pago    : " + factura.getMetodoPago()));
                doc.add(datosGrid);
                doc.add(new Paragraph(" ").setFontSize(4));

                // ---- TABLA DE PRODUCTOS ----
                Table tabla = new Table(new float[] { 4, 1, 2, 2, 2 })
                                .setWidth(UnitValue.createPercentValue(100));

                // Encabezados
                String[] heads = { "Producto", "Cant.", "Precio unit.", "IVA", "Total" };
                for (String h : heads)
                        tabla.addHeaderCell(celdaHeader(h));

                // Filas de items
                boolean par = false;
                for (ItemFactura item : factura.getItems()) {
                        DeviceRgb bg = par ? COLOR_ROW_PAR : null;
                        tabla.addCell(celdaFila(item.getProducto().getNombre(), TextAlignment.LEFT, bg, false));
                        tabla.addCell(celdaFila(String.valueOf(item.getCantidad()), TextAlignment.CENTER, bg, false));
                        tabla.addCell(celdaFila(fmt(item.getPrecioUnitario()), TextAlignment.RIGHT, bg, false));
                        tabla.addCell(celdaFila(fmt(item.getImpuesto()), TextAlignment.RIGHT, bg, false));
                        tabla.addCell(celdaFila(fmt(item.getTotal()), TextAlignment.RIGHT, bg, true));
                        par = !par;
                }
                doc.add(tabla);

                // ---- TOTALES ----
                Table totales = new Table(new float[] { 4, 2 })
                                .setWidth(UnitValue.createPercentValue(60))
                                .setHorizontalAlignment(HorizontalAlignment.RIGHT);

                agregarFilaTotales(totales, "Subtotal:", fmt(factura.calcularSubtotal()), null, false);
                agregarFilaTotales(totales, "IVA:", fmt(factura.calcularIva()), null, false);
                agregarFilaTotales(totales, "TOTAL A PAGAR:",
                                fmt(factura.calcularTotal()), COLOR_TOTAL, true);
                doc.add(totales);

                // ---- PIE DE PAGINA ----
                doc.add(new Paragraph(" "));
                doc.add(new Paragraph("Gracias por su compra. Conserve esta factura como soporte de su transaccion.")
                                .setFontSize(8).setItalic().setTextAlignment(TextAlignment.CENTER)
                                .setFontColor(ColorConstants.GRAY));
                doc.add(new Paragraph("Documento equivalente - Regimen simplificado")
                                .setFontSize(7).setTextAlignment(TextAlignment.CENTER)
                                .setFontColor(ColorConstants.GRAY));

                doc.close();
                System.out.println("[PDF] Factura generada: " + ruta);
                return ruta;
        }

        // =========================================================
        // REPORTE 2: Ventas diarias
        // =========================================================
        public static String reporteVentasDiarias(LocalDate fecha,
                        FacturaDAO dao) throws Exception {
                String ruta = "reportes/ventas_" + fecha.toString() + ".pdf";
                crearDirectorio(ruta);

                PdfDocument pdf = new PdfDocument(new PdfWriter(ruta));
                Document doc = new Document(pdf, PageSize.A4.rotate()); // horizontal
                doc.setMargins(30, 30, 30, 30);

                encabezadoReporte(doc, "REPORTE DE VENTAS DIARIAS",
                                "Fecha: " + fecha.format(FMT));

                List<String[]> filas = dao.ventasDia(fecha);
                double totalDia = dao.totalDia(fecha);

                String[] cols = { "N° Factura", "Hora", "Cliente", "Cajero", "Total ($)", "Metodo pago" };
                float[] widths = { 2.5f, 1f, 3f, 2.5f, 2f, 2f };
                Table tabla = tablaConEncabezado(cols, widths);

                boolean par = false;
                for (String[] f : filas) {
                        DeviceRgb bg = par ? COLOR_ROW_PAR : null;
                        tabla.addCell(celdaFila(f[0], TextAlignment.LEFT, bg, false));
                        tabla.addCell(celdaFila(f[1], TextAlignment.CENTER, bg, false));
                        tabla.addCell(celdaFila(f[2], TextAlignment.LEFT, bg, false));
                        tabla.addCell(celdaFila(f[3], TextAlignment.LEFT, bg, false));
                        tabla.addCell(celdaFila(f[4], TextAlignment.RIGHT, bg, false));
                        tabla.addCell(celdaFila(f[5], TextAlignment.CENTER, bg, true));
                        par = !par;
                }
                doc.add(tabla);

                doc.add(new Paragraph(" "));
                resumenBloque(doc,
                                "Total de transacciones: " + filas.size(),
                                "Total recaudado del dia: $" + String.format("%,.0f", totalDia));

                doc.close();
                System.out.println("[PDF] Reporte diario generado: " + ruta);
                return ruta;
        }

        // =========================================================
        // REPORTE 3: Top productos vendidos
        // =========================================================
        public static String reporteTopProductos(LocalDate desde, LocalDate hasta,
                        FacturaDAO dao) throws Exception {
                String ruta = "reportes/top_productos_" + desde + "_" + hasta + ".pdf";
                crearDirectorio(ruta);

                PdfDocument pdf = new PdfDocument(new PdfWriter(ruta));
                Document doc = new Document(pdf, PageSize.A4);
                doc.setMargins(36, 36, 36, 36);

                encabezadoReporte(doc, "REPORTE: PRODUCTOS MAS VENDIDOS",
                                "Periodo: " + desde.format(FMT) + " al " + hasta.format(FMT));

                List<String[]> filas = dao.topProductos(desde, hasta);

                String[] cols = { "#", "Producto", "Unidades", "Ingresos netos ($)", "Total c/IVA ($)" };
                float[] widths = { 0.5f, 4f, 1.5f, 2.5f, 2.5f };
                Table tabla = tablaConEncabezado(cols, widths);

                boolean par = false;
                for (String[] f : filas) {
                        DeviceRgb bg = par ? COLOR_ROW_PAR : null;
                        tabla.addCell(celdaFila(f[0], TextAlignment.CENTER, bg, false));
                        tabla.addCell(celdaFila(f[1], TextAlignment.LEFT, bg, false));
                        tabla.addCell(celdaFila(f[2], TextAlignment.CENTER, bg, false));
                        tabla.addCell(celdaFila(f[3], TextAlignment.RIGHT, bg, false));
                        tabla.addCell(celdaFila(f[4], TextAlignment.RIGHT, bg, true));
                        par = !par;
                }
                doc.add(tabla);

                doc.close();
                System.out.println("[PDF] Reporte top productos generado: " + ruta);
                return ruta;
        }

        // =========================================================
        // REPORTE 4: Ventas por cajero
        // =========================================================
        public static String reporteVentasPorCajero(LocalDate desde, LocalDate hasta,
                        FacturaDAO dao) throws Exception {
                String ruta = "reportes/ventas_cajero_" + desde + "_" + hasta + ".pdf";
                crearDirectorio(ruta);

                PdfDocument pdf = new PdfDocument(new PdfWriter(ruta));
                Document doc = new Document(pdf, PageSize.A4);
                doc.setMargins(36, 36, 36, 36);

                encabezadoReporte(doc, "REPORTE: VENTAS POR CAJERO",
                                "Periodo: " + desde.format(FMT) + " al " + hasta.format(FMT));

                List<String[]> filas = dao.ventasPorCajero(desde, hasta);

                String[] cols = { "Cajero", "N° Facturas", "Total vendido ($)" };
                float[] widths = { 4f, 2f, 3f };
                Table tabla = tablaConEncabezado(cols, widths);

                double gran_total = 0;
                boolean par = false;
                for (String[] f : filas) {
                        DeviceRgb bg = par ? COLOR_ROW_PAR : null;
                        tabla.addCell(celdaFila(f[0], TextAlignment.LEFT, bg, false));
                        tabla.addCell(celdaFila(f[1], TextAlignment.CENTER, bg, false));
                        tabla.addCell(celdaFila(f[2], TextAlignment.RIGHT, bg, true));
                        gran_total += Double.parseDouble(f[2].replace(",", ""));
                        par = !par;
                }
                doc.add(tabla);
                doc.add(new Paragraph(" "));
                resumenBloque(doc, "Total cajeros activos: " + filas.size(),
                                "Gran total del periodo: $" + String.format("%,.0f", gran_total));

                doc.close();
                System.out.println("[PDF] Reporte por cajero generado: " + ruta);
                return ruta;
        }

        // =========================================================
        // UTILIDADES PRIVADAS
        // =========================================================

        private static String fmt(double v) {
                return String.format("$%,.0f", v);
        }

        private static Paragraph lineaSeparadora() {
                Paragraph p = new Paragraph()
                                .setBorderBottom(new SolidBorder(COLOR_HEADER, 1f))
                                .setMarginTop(4)
                                .setMarginBottom(6);
                return p;
        }

        private static Cell bloqueInfo(String titulo, String... lineas) {
                Cell c = new Cell().setBorder(Border.NO_BORDER)
                                .setBorderLeft(new SolidBorder(COLOR_HEADER, 2));
                c.add(new Paragraph(titulo).setFontSize(8).setBold().setFontColor(COLOR_HEADER));
                for (String l : lineas)
                        c.add(new Paragraph(l).setFontSize(8).setFontColor(ColorConstants.DARK_GRAY));
                return c;
        }

        private static Cell celdaHeader(String texto) {
                return new Cell()
                                .add(new Paragraph(texto).setFontSize(9).setBold().setFontColor(COLOR_TEXT_H))
                                .setBackgroundColor(COLOR_HEADER)
                                .setTextAlignment(TextAlignment.CENTER)
                                .setPadding(5);
        }

        private static Cell celdaFila(String texto, TextAlignment alin,
                        DeviceRgb bg, boolean esFinal) {
                Cell c = new Cell()
                                .add(new Paragraph(texto == null ? "" : texto).setFontSize(9))
                                .setTextAlignment(alin)
                                .setPaddingTop(4).setPaddingBottom(4)
                                .setPaddingLeft(5).setPaddingRight(5)
                                .setBorderBottom(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.3f))
                                .setBorderTop(Border.NO_BORDER)
                                .setBorderLeft(Border.NO_BORDER)
                                .setBorderRight(esFinal ? Border.NO_BORDER : Border.NO_BORDER);
                if (bg != null)
                        c.setBackgroundColor(bg);
                return c;
        }

        private static void agregarFilaTotales(Table t, String etiqueta,
                        String valor, DeviceRgb bg, boolean grande) {
                Cell cEtiq = new Cell()
                                .add(new Paragraph(etiqueta).setFontSize(grande ? 10 : 9)
                                                .setBold().setTextAlignment(TextAlignment.RIGHT))
                                .setBorder(Border.NO_BORDER);
                Cell cVal = new Cell()
                                .add(new Paragraph(valor).setFontSize(grande ? 11 : 9)
                                                .setBold().setTextAlignment(TextAlignment.RIGHT))
                                .setBorder(Border.NO_BORDER);
                if (bg != null) {
                        cEtiq.setBackgroundColor(bg);
                        cVal.setBackgroundColor(bg);
                }
                t.addCell(cEtiq);
                t.addCell(cVal);
        }

        private static void encabezadoReporte(Document doc, String titulo, String subtitulo)
                        throws Exception {
                doc.add(new Paragraph(EMPRESA_NOMBRE).setFontSize(14).setBold()
                                .setFontColor(COLOR_HEADER).setTextAlignment(TextAlignment.CENTER));
                doc.add(new Paragraph(titulo).setFontSize(13).setBold()
                                .setTextAlignment(TextAlignment.CENTER));
                doc.add(new Paragraph(subtitulo).setFontSize(10)
                                .setTextAlignment(TextAlignment.CENTER)
                                .setFontColor(ColorConstants.DARK_GRAY));
                doc.add(lineaSeparadora());
        }

        private static Table tablaConEncabezado(String[] cols, float[] widths) {
                Table t = new Table(widths).setWidth(UnitValue.createPercentValue(100));
                for (String c : cols)
                        t.addHeaderCell(celdaHeader(c));
                return t;
        }

        private static void resumenBloque(Document doc, String... lineas) {
                Cell c = new Cell().setBorder(Border.NO_BORDER)
                                .setBorderLeft(new SolidBorder(COLOR_HEADER, 3))
                                .setBackgroundColor(COLOR_TOTAL)
                                .setPadding(8);
                for (String l : lineas)
                        c.add(new Paragraph(l).setFontSize(11).setBold().setFontColor(COLOR_HEADER));
                Table t = new Table(1).setWidth(UnitValue.createPercentValue(50))
                                .setHorizontalAlignment(HorizontalAlignment.RIGHT);
                t.addCell(c);
                doc.add(t);
        }
}
