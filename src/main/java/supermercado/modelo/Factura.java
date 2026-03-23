package supermercado.modelo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Factura {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String          numero;
    private LocalDateTime   fecha;
    private Cliente         cliente;
    private Cajero          cajero;
    private List<ItemFactura> items;
    private EstadoFactura   estado;
    private String          metodoPago;

    public Factura(String numero, Cliente cliente, Cajero cajero) {
        this.numero     = numero;
        this.fecha      = LocalDateTime.now();
        this.cliente    = cliente;
        this.cajero     = cajero;
        this.items      = new ArrayList<>();
        this.estado     = EstadoFactura.PENDIENTE;
        this.metodoPago = "EFECTIVO";
    }

    public void agregarItem(ItemFactura item) {
        if (estado != EstadoFactura.PENDIENTE)
            throw new IllegalStateException("No se puede modificar una factura " + estado);
        items.add(item);
    }

    public double calcularSubtotal() {
        return items.stream().mapToDouble(ItemFactura::getSubtotal).sum();
    }

    public double calcularIva() {
        return items.stream().mapToDouble(ItemFactura::getImpuesto).sum();
    }

    public double calcularTotal() {
        return items.stream().mapToDouble(ItemFactura::getTotal).sum();
    }

    public void marcarPagada()  { this.estado = EstadoFactura.PAGADA;   }
    public void anular()        { this.estado = EstadoFactura.ANULADA;  }

    // ---- Getters ----
    public String            getNumero()     { return numero; }
    public LocalDateTime     getFecha()      { return fecha; }
    public Cliente           getCliente()    { return cliente; }
    public Cajero            getCajero()     { return cajero; }
    public List<ItemFactura> getItems()      { return Collections.unmodifiableList(items); }
    public EstadoFactura     getEstado()     { return estado; }
    public String            getMetodoPago() { return metodoPago; }

    // ---- Setters ----
    public void setMetodoPago(String m) { this.metodoPago = m; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String sep = "═".repeat(62);
        sb.append(sep).append("\n");
        sb.append(String.format("  FACTURA ELECTRONICA  N°: %-20s  %s%n",
                numero, fecha.format(FMT)));
        sb.append(sep).append("\n");
        sb.append("  Cliente : ").append(cliente.getDatos()).append("\n");
        sb.append("  Cajero  : ").append(cajero.getNombre())
          .append("  |  Turno: ").append(cajero.getTurno()).append("\n");
        sb.append("─".repeat(62)).append("\n");
        sb.append(String.format("  %-30s %5s %12s %10s %12s%n",
                "PRODUCTO","CANT","SUBTOTAL","IVA","TOTAL"));
        sb.append("─".repeat(62)).append("\n");
        for (ItemFactura it : items) sb.append("  ").append(it).append("\n");
        sb.append("─".repeat(62)).append("\n");
        sb.append(String.format("  %-44s $%,12.0f%n", "Subtotal:", calcularSubtotal()));
        sb.append(String.format("  %-44s $%,12.0f%n", "IVA:",      calcularIva()));
        sb.append(String.format("  %-44s $%,12.0f%n", "TOTAL:",    calcularTotal()));
        sb.append(sep).append("\n");
        sb.append(String.format("  Estado: %-20s  Pago: %s%n", estado, metodoPago));
        sb.append(sep).append("\n");
        return sb.toString();
    }
}
