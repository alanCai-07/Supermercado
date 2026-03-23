package supermercado.modelo;

public class ItemFactura {

    private Producto producto;
    private int      cantidad;
    private double   precioUnitario; // precio fijado al momento de la venta

    public ItemFactura(Producto producto, int cantidad) {
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        this.producto       = producto;
        this.cantidad       = cantidad;
        this.precioUnitario = producto.getPrecio();
    }

    public double getSubtotal()   { return precioUnitario * cantidad; }
    public double getImpuesto()   { return getSubtotal() * producto.getImpuesto(); }
    public double getTotal()      { return getSubtotal() + getImpuesto(); }

    // ---- Getters ----
    public Producto getProducto()       { return producto; }
    public int      getCantidad()       { return cantidad; }
    public double   getPrecioUnitario() { return precioUnitario; }

    @Override
    public String toString() {
        return String.format("%-30s x%3d  $%,10.0f  IVA: $%,8.0f  Total: $%,10.0f",
                producto.getNombre(), cantidad,
                getSubtotal(), getImpuesto(), getTotal());
    }
}
