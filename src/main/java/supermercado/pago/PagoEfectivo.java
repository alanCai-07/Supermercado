package supermercado.pago;

public class PagoEfectivo implements MetodoPago {

    private double montoRecibido;
    private double cambio;

    public PagoEfectivo(double montoRecibido) {
        this.montoRecibido = montoRecibido;
    }

    @Override
    public boolean pagar(double monto) {
        if (montoRecibido >= monto) {
            cambio = montoRecibido - monto;
            return true;
        }
        System.out.printf("[PAGO] Monto insuficiente. Falta: $%,.0f%n",
                monto - montoRecibido);
        return false;
    }

    @Override public String getTipo()   { return "EFECTIVO"; }
    @Override public double getMonto()  { return montoRecibido; }
    public    double getCambio()        { return cambio; }

    @Override
    public void generarRecibo() {
        System.out.println("┌─ RECIBO EFECTIVO ─────────────────┐");
        System.out.printf("│  Recibido: $%,20.0f      │%n", montoRecibido);
        System.out.printf("│  Cambio:   $%,20.0f      │%n", cambio);
        System.out.println("└────────────────────────────────────┘");
    }
}
