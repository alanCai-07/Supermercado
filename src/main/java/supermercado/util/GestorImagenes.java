package supermercado.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class GestorImagenes {

    private static final String CARPETA_BASE = "imagenes/productos";

    private GestorImagenes() {
    }

    /**
     * Copia la imagen seleccionada a la carpeta del proyecto con el codigo del
     * producto como nombre.
     */
    public static String guardarImagenProducto(String idProducto, File origen) throws IOException {
        File dirDestino = new File(CARPETA_BASE);
        if (!dirDestino.exists())
            dirDestino.mkdirs();

        String extension = obtenerExtension(origen.getName());
        String nombreDestino = idProducto + "." + extension;
        Path destino = Path.of(CARPETA_BASE, nombreDestino);

        Files.copy(origen.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);

        // Se guarda como ruta relativa, compatible al abrir el archivo desde cualquier
        // parte del codigo
        return CARPETA_BASE + "/" + nombreDestino;
    }

    private static String obtenerExtension(String nombreArchivo) {
        int i = nombreArchivo.lastIndexOf('.');
        return (i > 0) ? nombreArchivo.substring(i + 1).toLowerCase() : "png";
    }

    /** Devuelve el File de la imagen o null si no existe o no esta definida. */
    public static File obtenerArchivo(String rutaImagen) {
        if (rutaImagen == null)
            return null;
        rutaImagen = rutaImagen.trim();
        if (rutaImagen.isBlank())
            return null;

        String rutaNormalizada = rutaImagen.replace("/", File.separator).replace("\\", File.separator);

        File archivo = new File(rutaImagen);
        if (archivo.exists()) {
            return archivo;
        }

        File directo = new File(System.getProperty("user.dir"), rutaNormalizada);
        if (directo.exists()) {
            return directo;
        }

        // Buscar en la ruta de trabajo desde target/classes u otros directorios de
        // ejecucion.
        java.nio.file.Path carpetaActual = java.nio.file.Path.of(System.getProperty("user.dir")).toAbsolutePath();
        java.nio.file.Path rutaBuscada = carpetaActual.resolve(rutaNormalizada).normalize();
        if (rutaBuscada.toFile().exists()) {
            return rutaBuscada.toFile();
        }

        java.nio.file.Path actual = carpetaActual;
        while (actual.getParent() != null) {
            actual = actual.getParent();
            java.nio.file.Path posible = actual.resolve(rutaNormalizada).normalize();
            if (posible.toFile().exists()) {
                return posible.toFile();
            }
        }

        // Debug: imprimir intentos de resolucion para diagnostico
        System.out.println("[GestorImagenes] No se encontro imagen. Ruta recibida='" + rutaImagen
                + "', ruta normalizada='" + rutaNormalizada + "'");
        System.out.println("[GestorImagenes] user.dir='" + System.getProperty("user.dir") + "'");
        System.out.println("[GestorImagenes] intentos: " + archivo.getAbsolutePath() + ", " + directo.getAbsolutePath()
                + ", " + rutaBuscada.toAbsolutePath());

        return null;
    }
}