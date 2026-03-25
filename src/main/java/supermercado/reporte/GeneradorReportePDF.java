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

    private static final DateTimeFormatter FMT      = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Colores corporativos
    private static final DeviceRgb COLOR_HEADER  = new DeviceRgb(34, 85, 153);   // azul oscuro
    private static final DeviceRgb COLOR_SUBHEAD = new DeviceRgb(70, 130, 180);  // azul medio
    private static final DeviceRgb COLOR_ROW_PAR = new DeviceRgb(235, 242, 250); // azul muy claro
    private static final DeviceRgb COLOR_TOTAL   = new DeviceRgb(220, 235, 255); // azul pastel
    private static final DeviceRgb COLOR_TEXT_H  = new DeviceRgb(255, 255, 255); // blanco

    private static final String EMPRESA_NOMBRE   = "SUPERMERCADO EL EXITO";
    private static final String EMPRESA_NIT      = "NIT: 900.123.456-7";
    private static final String EMPRESA_DIR      = "Calle 5 # 20-30, Cali, Valle del Cauca";
    private static final String EMPRESA_TEL      = "Tel: (602) 890-1234";

    /** Asegura que exista el directorio de destino. */
    private static void crearDirectorio(String ruta) {
        File dir = new File(ruta).getParentFile();
        if (dir != null && !dir.exists()) dir.mkdirs();
    }

    // =========================================================
    // REPORTE 1: Factura individual en PDF
    // =========================================================
    public static String generarFacturaPDF(Factura factura) throws Exception {
        String ruta = "reportes/facturas/" + factura.getNumero() + ".pdf";
        crearDirectorio(ruta);

        PdfWriter   writer = new PdfWriter(ruta);
        PdfDocument pdf    = new PdfDocument(writer);
        Document    doc    = new Document(pdf, PageSize.A4);
        doc.setMargins(36, 36, 36, 36);

        // ---- ENCABEZADO EMPRESA ----
        Table cabecera = new Table(new float[]{3, 2})
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
        Table datosGrid = new Table(new float[]{1, 1})
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
        Table tabla = new Table(new float[]{4, 1, 2, 2, 2})
                .setWidth(UnitValue.createPercentValue(100));

        // Encabezados
        String[] heads = {"Producto", "Cant.", "Precio unit.", "IVA", "Total"};
        for (String h : heads)
            tabla.addHeaderCell(celdaHeader(h));

        // Filas de items
        boolean par = false;
        for (ItemFactura item : factura.getItems()) {
            DeviceRgb bg = par ? COLOR_ROW_PAR : null;
            tabla.addCell(celdaFila(item.getProducto().getNombre(), TextAlignment.LEFT,  bg, false));
            tabla.addCell(celdaFila(String.valueOf(item.getCantidad()), TextAlignment.CENTER, bg, false));
            tabla.addCell(celdaFila(fmt(item.getPrecioUnitario()), TextAlignment.RIGHT, bg, false));
            tabla.addCell(celdaFila(fmt(item.getImpuesto()),       TextAlignment.RIGHT, bg, false));
            tabla.addCell(celdaFila(fmt(item.getTotal()),          TextAlignment.RIGHT, bg, true));
            par = !par;
        }
        doc.add(tabla);

        // ---- TOTALES ----
        Table totales = new Table(new float[]{4, 2})
                .setWidth(UnitValue.createPercentValue(60))
                .setHorizontalAlignment(HorizontalAlignment.RIGHT);

        agregarFilaTotales(totales, "Subtotal:", fmt(factura.calcularSubtotal()), null,   false);
        agregarFilaTotales(totales, "IVA:",      fmt(factura.calcularIva()),      null,   false);
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
        Document    doc = new Document(pdf, PageSize.A4.rotate()); // horizontal
        doc.setMargins(30, 30, 30, 30);

        encabezadoReporte(doc, "REPORTE DE VENTAS DIARIAS",
                "Fecha: " + fecha.format(FMT));

        List<String[]> filas = dao.ventasDia(fecha);
        double totalDia = dao.totalDia(fecha);

        String[] cols   = {"N° Factura", "Hora", "Cliente", "Cajero", "Total ($)", "Metodo pago"};
        float[]  widths = {2.5f, 1f, 3f, 2.5f, 2f, 2f};
        Table tabla = tablaConEncabezado(cols, widths);

        boolean par = false;
        for (String[] f : filas) {
            DeviceRgb bg = par ? COLOR_ROW_PAR : null;
            tabla.addCell(celdaFila(f[0], TextAlignment.LEFT,   bg, false));
            tabla.addCell(celdaFila(f[1], TextAlignment.CENTER, bg, false));
            tabla.addCell(celdaFila(f[2], TextAlignment.LEFT,   bg, false));
            tabla.addCell(celdaFila(f[3], TextAlignment.LEFT,   bg, false));
            tabla.addCell(celdaFila(f[4], TextAlignment.RIGHT,  bg, false));
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
        Document    doc = new Document(pdf, PageSize.A4);
        doc.setMargins(36, 36, 36, 36);

        encabezadoReporte(doc, "REPORTE: PRODUCTOS MAS VENDIDOS",
                "Periodo: " + desde.format(FMT) + " al " + hasta.format(FMT));

        List<String[]> filas = dao.topProductos(desde, hasta);

        String[] cols   = {"#", "Producto", "Unidades", "Ingresos netos ($)", "Total c/IVA ($)"};
        float[]  widths = {0.5f, 4f, 1.5f, 2.5f, 2.5f};
        Table tabla = tablaConEncabezado(cols, widths);

        boolean par = false;
        for (String[] f : filas) {
            DeviceRgb bg = par ? COLOR_ROW_PAR : null;
            tabla.addCell(celdaFila(f[0], TextAlignment.CENTER, bg, false));
            tabla.addCell(celdaFila(f[1], TextAlignment.LEFT,   bg, false));
            tabla.addCell(celdaFila(f[2], TextAlignment.CENTER, bg, false));
            tabla.addCell(celdaFila(f[3], TextAlignment.RIGHT,  bg, false));
            tabla.addCell(celdaFila(f[4], TextAlignment.RIGHT,  bg, true));
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
        Document    doc = new Document(pdf, PageSize.A4);
        doc.setMargins(36, 36, 36, 36);

        encabezadoReporte(doc, "REPORTE: VENTAS POR CAJERO",
                "Periodo: " + desde.format(FMT) + " al " + hasta.format(FMT));

        List<String[]> filas = dao.ventasPorCajero(desde, hasta);

        String[] cols   = {"Cajero", "N° Facturas", "Total vendido ($)"};
        float[]  widths = {4f, 2f, 3f};
        Table tabla = tablaConEncabezado(cols, widths);

        double gran_total = 0;
        boolean par = false;
        for (String[] f : filas) {
            DeviceRgb bg = par ? COLOR_ROW_PAR : null;
            tabla.addCell(celdaFila(f[0], TextAlignment.LEFT,   bg, false));
            tabla.addCell(celdaFila(f[1], TextAlignment.CENTER, bg, false));
            tabla.addCell(celdaFila(f[2], TextAlignment.RIGHT,  bg, true));
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

    private static String fmt(double v) { return String.format("$%,.0f", v); }

    private static Table lineaSeparadora() {
        // Reemplaza SolidLine (no disponible en iText 7.2.5) por una tabla de 1 celda
        // con borde superior coloreado — mismo efecto visual, compatible con todas las versiones
        Table linea = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(4).setMarginBottom(6);
        Cell celda = new Cell()
                .setHeight(2)
                .setBackgroundColor(COLOR_HEADER)
                .setBorder(Border.NO_BORDER)
                .setPadding(0);
        linea.addCell(celda);
        return linea;
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
        if (bg != null) c.setBackgroundColor(bg);
        return c;
    }

    private static void agregarFilaTotales(Table t, String etiqueta,
                                            String valor, DeviceRgb bg, boolean grande) {
        Paragraph pEtiq = new Paragraph(etiqueta)
                .setFontSize(grande ? 10 : 9)
                .setTextAlignment(TextAlignment.RIGHT);
        if (grande) pEtiq.setBold();

        Paragraph pVal = new Paragraph(valor)
                .setFontSize(grande ? 11 : 9)
                .setTextAlignment(TextAlignment.RIGHT);
        if (grande) pVal.setBold();

        Cell cEtiq = new Cell().add(pEtiq).setBorder(Border.NO_BORDER);
        Cell cVal  = new Cell().add(pVal) .setBorder(Border.NO_BORDER);
        if (bg != null) { cEtiq.setBackgroundColor(bg); cVal.setBackgroundColor(bg); }
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
        for (String c : cols) t.addHeaderCell(celdaHeader(c));
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

    // =========================================================
    // FACTURA TERMICA 80mm — para impresoras POS
    // =========================================================

    // Ancho papel 80mm en puntos PDF (1mm = 2.8346 pt)
    private static final float MM80   = 80  * 2.8346f;  // 226.77 pt
    // Margenes minimos para termica
    private static final float MAR_H  = 8f;  // margen horizontal
    private static final float MAR_V  = 6f;  // margen vertical

    /**
     * Genera un PDF de factura en formato termico 80mm.
     * La altura de la pagina se calcula dinamicamente segun el numero de items.
     * Ruta: reportes/facturas/TERMICA_FAC-XXXXX.pdf
     */
    public static String generarFacturaTermica(Factura factura) throws Exception {
        String ruta = "reportes/facturas/TERMICA_" + factura.getNumero() + ".pdf";

        // Crear directorio explicitamente y verificar
        File dir = new File("reportes/facturas");
        if (!dir.exists()) {
            boolean creado = dir.mkdirs();
            System.out.println("[TERMICA] Directorio creado: " + creado + " -> " + dir.getAbsolutePath());
        }

        System.out.println("[TERMICA] Generando ticket en: " + new File(ruta).getAbsolutePath());

        int numItems = factura.getItems().size();
        if (numItems == 0) throw new Exception("La factura no tiene items.");

        // Calcular altura dinamica del ticket en puntos
        float alturaTicket = 155 + (numItems * 28f) + 100 + 70;
        PageSize tamanoTermica = new PageSize(MM80, alturaTicket);

        PdfWriter   writer = new PdfWriter(ruta);
        PdfDocument pdf    = new PdfDocument(writer);
        Document    doc    = new Document(pdf, tamanoTermica);
        doc.setMargins(MAR_V, MAR_H, MAR_V, MAR_H);

        float anchoUtil = MM80 - (MAR_H * 2);

        // ---- FUENTE MONOESPACIADA para alineacion perfecta ----
        // iText incluye Courier por defecto (ideal para termicas)
        PdfFont fuenteMono   = PdfFontFactory.createFont(
                com.itextpdf.io.font.constants.StandardFonts.COURIER);
        PdfFont fuenteMonoBold = PdfFontFactory.createFont(
                com.itextpdf.io.font.constants.StandardFonts.COURIER_BOLD);

        // ============ ENCABEZADO ============
        // Nombre empresa centrado y grande
        doc.add(new Paragraph(EMPRESA_NOMBRE)
                .setFont(fuenteMonoBold).setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(1));

        doc.add(new Paragraph(EMPRESA_NIT)
                .setFont(fuenteMono).setFontSize(7)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(1));

        doc.add(new Paragraph(EMPRESA_DIR)
                .setFont(fuenteMono).setFontSize(7)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(1));

        doc.add(new Paragraph(EMPRESA_TEL)
                .setFont(fuenteMono).setFontSize(7)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(3));

        // Linea separadora de asteriscos
        doc.add(lineaTermica(anchoUtil, fuenteMono, '*'));

        // ---- Datos de la factura ----
        doc.add(new Paragraph("FACTURA ELECTRONICA")
                .setFont(fuenteMonoBold).setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(2).setMarginBottom(2));

        doc.add(lineaTermica(anchoUtil, fuenteMono, '-'));

        doc.add(filaDatos(fuenteMono, fuenteMonoBold, "N°:", factura.getNumero(), anchoUtil));
        doc.add(filaDatos(fuenteMono, fuenteMonoBold, "Fecha:",
                factura.getFecha().format(FMT_HORA), anchoUtil));
        doc.add(filaDatos(fuenteMono, fuenteMonoBold, "Cliente:",
                factura.getCliente().getNombre(), anchoUtil));
        doc.add(filaDatos(fuenteMono, fuenteMonoBold, "NIT:",
                factura.getCliente().getNit(), anchoUtil));
        doc.add(filaDatos(fuenteMono, fuenteMonoBold, "Cajero:",
                factura.getCajero().getNombre(), anchoUtil));
        doc.add(filaDatos(fuenteMono, fuenteMonoBold, "Pago:",
                factura.getMetodoPago(), anchoUtil));

        doc.add(lineaTermica(anchoUtil, fuenteMono, '-'));

        // ============ ENCABEZADO DE ITEMS ============
        // Columnas: Nombre (wrap) | Cant | Precio | Total
        // En 80mm tenemos ~32 caracteres con Courier 8pt
        doc.add(new Paragraph(
                padDer("PRODUCTO", 18) + pad("CANT", 4) + padIzq("TOTAL", 8))
                .setFont(fuenteMonoBold).setFontSize(7.5f)
                .setMarginBottom(1));

        doc.add(lineaTermica(anchoUtil, fuenteMono, '-'));

        // ============ ITEMS ============
        for (ItemFactura item : factura.getItems()) {
            String nombre  = item.getProducto().getNombre();
            String cant    = String.valueOf(item.getCantidad());
            String total   = String.format("%,.0f", item.getTotal());
            String unitario = String.format("$%,.0f c/u", item.getPrecioUnitario());

            // Nombre puede ser largo — se trunca a 30 chars
            if (nombre.length() > 28) nombre = nombre.substring(0, 27) + ".";

            // Fila principal: nombre | cant | total
            doc.add(new Paragraph(
                    padDer(nombre, 18) + pad(cant, 4) + padIzq("$" + total, 8))
                    .setFont(fuenteMono).setFontSize(7.5f)
                    .setMarginBottom(0));

            // Precio unitario en linea secundaria con sangria
            doc.add(new Paragraph("  " + unitario +
                    (item.getImpuesto() > 0
                            ? "  IVA:$" + String.format("%,.0f", item.getImpuesto())
                            : ""))
                    .setFont(fuenteMono).setFontSize(6.5f)
                    .setFontColor(ColorConstants.GRAY)
                    .setMarginBottom(2));
        }

        doc.add(lineaTermica(anchoUtil, fuenteMono, '-'));

        // ============ TOTALES ============
        doc.add(filaTotalTermica(fuenteMono, fuenteMonoBold,
                "Subtotal:", String.format("$%,.0f", factura.calcularSubtotal()),
                anchoUtil, false));

        if (factura.calcularIva() > 0)
            doc.add(filaTotalTermica(fuenteMono, fuenteMonoBold,
                    "IVA (19%):", String.format("$%,.0f", factura.calcularIva()),
                    anchoUtil, false));

        doc.add(lineaTermica(anchoUtil, fuenteMono, '='));

        doc.add(filaTotalTermica(fuenteMono, fuenteMonoBold,
                "TOTAL:", String.format("$%,.0f", factura.calcularTotal()),
                anchoUtil, true));

        doc.add(lineaTermica(anchoUtil, fuenteMono, '='));

        // ============ PIE DE PAGINA ============
        doc.add(new Paragraph(" ").setFontSize(4));
        doc.add(new Paragraph("Gracias por su compra!")
                .setFont(fuenteMonoBold).setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(1));
        doc.add(new Paragraph("Conserve este comprobante")
                .setFont(fuenteMono).setFontSize(7)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(1));
        doc.add(new Paragraph("como soporte de su transaccion.")
                .setFont(fuenteMono).setFontSize(7)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(3));

        // QR / codigo de factura (representado en texto)
        doc.add(new Paragraph("[ " + factura.getNumero() + " ]")
                .setFont(fuenteMonoBold).setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(1));

        doc.add(new Paragraph(factura.getFecha().format(FMT_HORA))
                .setFont(fuenteMono).setFontSize(7)
                .setTextAlignment(TextAlignment.CENTER));

        doc.add(lineaTermica(anchoUtil, fuenteMono, '-'));
        doc.add(new Paragraph("Regimen Simplificado - Art. 616-1 E.T.")
                .setFont(fuenteMono).setFontSize(6)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.GRAY));

        doc.close();
        System.out.println("[TERMICA] Ticket generado: " + ruta);
        return ruta;
    }

    // ---- Helpers exclusivos para termica ----

    /** Linea de caracteres repetidos al ancho del ticket. */
    private static Paragraph lineaTermica(float anchoUtil, PdfFont font, char caracter) {
        // ~32 caracteres caben en 80mm con Courier 8pt (aprox 6.5pt por char)
        int chars = 32;
        String linea = String.valueOf(caracter).repeat(chars);
        return new Paragraph(linea)
                .setFont(font).setFontSize(7.5f)
                .setMarginTop(1).setMarginBottom(1);
    }

    /** Fila etiqueta—valor alineados a los extremos. */
    private static Paragraph filaDatos(PdfFont normal, PdfFont bold,
                                        String etiqueta, String valor, float anchoUtil) {
        // Total ~32 chars: etiqueta a la izq (max 10), valor a la der
        int totalChars = 32;
        String eti = etiqueta.length() > 9 ? etiqueta.substring(0, 9) : etiqueta;
        // Valor truncado si es muy largo
        String val = valor != null ? valor : "";
        if (val.length() > (totalChars - eti.length() - 1))
            val = val.substring(0, totalChars - eti.length() - 2) + ".";

        int espacios = totalChars - eti.length() - val.length();
        String linea = eti + " ".repeat(Math.max(1, espacios)) + val;

        return new Paragraph(linea)
                .setFont(normal).setFontSize(7.5f)
                .setMarginBottom(1);
    }

    /** Fila de totales: etiqueta izq, valor der, opcionalmente en negrita. */
    private static Paragraph filaTotalTermica(PdfFont normal, PdfFont bold,
                                               String etiqueta, String valor,
                                               float anchoUtil, boolean grande) {
        int totalChars = 32;
        int espacios   = totalChars - etiqueta.length() - valor.length();
        String linea   = etiqueta + " ".repeat(Math.max(1, espacios)) + valor;

        return new Paragraph(linea)
                .setFont(grande ? bold : normal)
                .setFontSize(grande ? 9f : 7.5f)
                .setMarginBottom(grande ? 2 : 1);
    }

    /** Pad a la derecha (rellenar con espacios). */
    private static String padDer(String s, int ancho) {
        if (s == null) s = "";
        if (s.length() >= ancho) return s.substring(0, ancho);
        return s + " ".repeat(ancho - s.length());
    }

    /** Pad centrado. */
    private static String pad(String s, int ancho) {
        if (s == null) s = "";
        if (s.length() >= ancho) return s.substring(0, ancho);
        int total = ancho - s.length();
        int izq   = total / 2;
        return " ".repeat(izq) + s + " ".repeat(total - izq);
    }

    /** Pad a la izquierda (alinear a la derecha). */
    private static String padIzq(String s, int ancho) {
        if (s == null) s = "";
        if (s.length() >= ancho) return s.substring(0, ancho);
        return " ".repeat(ancho - s.length()) + s;
    }
}
