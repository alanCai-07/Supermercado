package supermercado.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class GestorImagenes {

    private static final String CARPETA_BASE = "imagenes/productos";

    private GestorImagenes() {}

    /** Copia la imagen seleccionada a la carpeta del proyecto con el codigo del producto como nombre. */
    public static String guardarImagenProducto(String idProducto, File origen) throws IOException {
        File dirDestino = new File(CARPETA_BASE);
        if (!dirDestino.exists())
            dirDestino.mkdirs();

        String extension = obtenerExtension(origen.getName());
        String nombreDestino = idProducto + "." + extension;
        Path destino = Path.of(CARPETA_BASE, nombreDestino);

        Files.copy(origen.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);

        // Se guarda como ruta relativa, compatible al abrir el archivo desde cualquier parte del codigo
        return CARPETA_BASE + "/" + nombreDestino;
    }

    private static String obtenerExtension(String nombreArchivo) {
        int i = nombreArchivo.lastIndexOf('.');
        return (i > 0) ? nombreArchivo.substring(i + 1).toLowerCase() : "png";
    }

    /** Devuelve el File de la imagen o null si no existe o no esta definida. */
    public static File obtenerArchivo(String rutaImagen) {
        if (rutaImagen == null || rutaImagen.isBlank())
            return null;
        File f = new File(rutaImagen);
        return f.exists() ? f : null;
    }
}