package supermercado.servicio;

import supermercado.dao.*;
import supermercado.modelo.*;
import supermercado.pago.MetodoPago;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SistemaFacturacion {

    private static SistemaFacturacion instancia;

    private Inventario inventario;
    private Cajero cajeroActivo;
    private FacturaDAO facturaDAO;
    private ClienteDAO clienteDAO;
    private CajeroDAO cajeroDAO;

    // Facturas en memoria de la sesion actual
    private List<Factura> facturasSesion = new ArrayList<>();

    private SistemaFacturacion() {
        inventario = new Inventario();
        facturaDAO = new FacturaDAO();
        clienteDAO = new ClienteDAO();
        cajeroDAO = new CajeroDAO();
    }

    public static SistemaFacturacion getInstance() {
        if (instancia == null)
            instancia = new SistemaFacturacion();
        return instancia;
    }

    // ---- Login del cajero ----
    public boolean login(String idCajero, String contrasena) throws SQLException {
        cajeroActivo = cajeroDAO.autenticar(idCajero, contrasena);
        if (cajeroActivo != null) {
            System.out.println("[SISTEMA] Sesion iniciada: " + cajeroActivo.getNombre());
            inventario.cargarDesdeDB();
            return true;
        }
        System.out.println("[SISTEMA] Credenciales incorrectas.");
        return false;
    }

    public void logout() {
        System.out.println("[SISTEMA] Sesion cerrada: " +
                (cajeroActivo != null ? cajeroActivo.getNombre() : ""));
        cajeroActivo = null;
    }

    // ---- Crear nueva factura ----
    public Factura crearFactura(Cliente cliente) throws SQLException {
        if (cajeroActivo == null)
            throw new IllegalStateException("No hay cajero activo.");
        String numero = facturaDAO.siguienteNumero();
        Factura f = new Factura(numero, cliente, cajeroActivo);
        facturasSesion.add(f);
        return f;
    }

    // ---- Completar pago y guardar en BD ----
    public boolean procesarPago(Factura factura, MetodoPago metodo) throws SQLException {
        if (metodo.pagar(factura.calcularTotal())) {
            factura.marcarPagada();
            factura.setMetodoPago(metodo.getTipo());
            // FIX: facturaDAO.guardar ya descuenta el stock en BD con UPDATE stock = stock
            // - cantidad
            // NO llamar a inventario.descontarStock() aqui para evitar doble descuento
            facturaDAO.guardar(factura);
            // Sincronizar inventario en memoria con los valores reales de BD
            inventario.cargarDesdeDB();
            return true;
        }
        return false;
    }

    // ---- Anular factura ----
    public void anularFactura(String numero) throws SQLException {
        facturaDAO.actualizarEstado(numero, EstadoFactura.ANULADA);
        facturasSesion.stream()
                .filter(f -> f.getNumero().equals(numero))
                .findFirst()
                .ifPresent(Factura::anular);
        System.out.println("[SISTEMA] Factura " + numero + " anulada.");
    }

    // ---- Buscar factura ----
    public Optional<Factura> buscarFactura(String numero) throws SQLException {
        return facturaDAO.buscarPorNumero(numero, clienteDAO, cajeroDAO);
    }

    public boolean registrarCajeroCaja(String id, String nombre, String turno, String password) throws SQLException {
        if (cajeroActivo == null || !cajeroActivo.esAdmin()) {
            throw new IllegalStateException("Solo un administrador puede registrar nuevos cajeros.");
        }
        if (id == null || id.isBlank() || nombre == null || nombre.isBlank() || turno == null || turno.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Todos los campos son obligatorios.");
        }
        if (cajeroDAO.existePorId(id) || cajeroDAO.existePorNombre(nombre)) {
            throw new IllegalArgumentException("Ya existe un cajero con ese identificador o nombre.");
        }

        String passwordHash = CajeroDAO.hashPassword(password);
        Cajero nuevo = new Cajero(id.trim(), nombre.trim(), turno.trim(), passwordHash, "CAJERO");
        cajeroDAO.guardar(nuevo);
        return true;
    }

    // ---- Getters ----
    public Inventario getInventario() {
        return inventario;
    }

    public Cajero getCajeroActivo() {
        return cajeroActivo;
    }

    public FacturaDAO getFacturaDAO() {
        return facturaDAO;
    }

    public ClienteDAO getClienteDAO() {
        return clienteDAO;
    }

    public CajeroDAO getCajeroDAO() {
        return cajeroDAO;
    }
}
