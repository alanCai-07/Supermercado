package supermercado.pago;

public class PagoTarjeta implements MetodoPago {

    private String numeroTarjeta; // solo los ultimos 4 digitos para seguridad
    private String tipoTarjeta;   // DEBITO | CREDITO
    private double monto;
    private boolean aprobado;

    public PagoTarjeta(String ultimosCuatroDigitos, String tipoTarjeta) {
        this.numeroTarjeta = ultimosCuatroDigitos;
        this.tipoTarjeta   = tipoTarjeta.toUpperCase();
    }

    @Override
    public boolean pagar(double monto) {
        this.monto = monto;
        // Simulacion de aprobacion (en produccion conectar con datafono)
        aprobado = numeroTarjeta != null && numeroTarjeta.matches("\\d{4}");
        if (aprobado)
            System.out.printf("[TARJETA] Pago aprobado por $%,.0f con tarjeta ***%s%n",
                    monto, numeroTarjeta);
        else
            System.out.println("[TARJETA] Pago rechazado.");
        return aprobado;
    }

    public boolean validarTarjeta() {
        return numeroTarjeta != null && numeroTarjeta.matches("\\d{4}");
    }

    @Override public String getTipo()  { return "TARJETA_" + tipoTarjeta; }
    @Override public double getMonto() { return monto; }

    @Override
    public void generarRecibo() {
        System.out.println("┌─ RECIBO TARJETA ──────────────────┐");
        System.out.printf("│  Tipo:    %-26s│%n", tipoTarjeta);
        System.out.printf("│  Tarjeta: ***%-22s│%n", numeroTarjeta);
        System.out.printf("│  Monto:   $%,20.0f      │%n", monto);
        System.out.printf("│  Estado:  %-26s│%n", aprobado ? "APROBADO" : "RECHAZADO");
        System.out.println("└────────────────────────────────────┘");
    }
}
