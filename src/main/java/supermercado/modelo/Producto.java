package supermercado.modelo;

public class Producto {

    private String  id;
    private String  nombre;
    private double  precio;
    private String  categoria;
    private double  impuesto;
    private int     stock;
    private boolean activo;
    private String  rutaImagen;   // NUEVO: ruta relativa del archivo de imagen

    public Producto(String id, String nombre, double precio,
                    String categoria, double impuesto, int stock) {
        this.id        = id;
        this.nombre    = nombre;
        this.precio    = precio;
        this.categoria = categoria;
        this.impuesto  = impuesto;
        this.stock     = stock;
        this.activo    = true;
        this.rutaImagen = null;
    }

    public double getPrecioConIva() {
        return precio * (1 + impuesto);
    }

    // ---- Getters ----
    public String  getId()          { return id; }
    public String  getNombre()      { return nombre; }
    public double  getPrecio()      { return precio; }
    public String  getCategoria()   { return categoria; }
    public double  getImpuesto()    { return impuesto; }
    public int     getStock()       { return stock; }
    public boolean isActivo()       { return activo; }
    public String  getRutaImagen()  { return rutaImagen; }

    // ---- Setters ----
    public void setStock(int stock)          { this.stock      = stock; }
    public void setActivo(boolean a)         { this.activo     = a; }
    public void setPrecio(double p)          { this.precio     = p; }
    public void setRutaImagen(String ruta)   { this.rutaImagen = ruta; }

    @Override
    public String toString() {
        return String.format("[%s] %-30s $%,.0f  (IVA %.0f%%)  Stock: %d",
                id, nombre, precio, impuesto * 100, stock);
    }
}