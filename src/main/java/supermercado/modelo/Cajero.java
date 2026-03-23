package supermercado.modelo;

public class Cajero {

    private String id;
    private String nombre;
    private String turno;
    private String contrasenaHash;
    private boolean activo;
    private String  rol;   // "ADMIN" o "CAJERO"

    public Cajero(String id, String nombre, String turno, String contrasenaHash) {
        this.id             = id;
        this.nombre         = nombre;
        this.turno          = turno;
        this.contrasenaHash = contrasenaHash;
        this.activo         = true;
        this.rol            = "CAJERO";
    }

    public Cajero(String id, String nombre, String turno,
                  String contrasenaHash, String rol) {
        this(id, nombre, turno, contrasenaHash);
        this.rol = (rol != null ? rol.toUpperCase() : "CAJERO");
    }

    /** Retorna true solo si este cajero tiene rol ADMIN. */
    public boolean esAdmin() {
        return "ADMIN".equalsIgnoreCase(rol);
    }

    /** Compara la contrasena en texto plano con el hash almacenado (SHA-256). */
    public boolean autenticar(String contrasenaIngresada) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(contrasenaIngresada.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString().equalsIgnoreCase(contrasenaHash);
        } catch (Exception e) {
            return false;
        }
    }

    // ---- Getters ----
    public String  getId()             { return id; }
    public String  getNombre()         { return nombre; }
    public String  getTurno()          { return turno; }
    public String  getContrasenaHash() { return contrasenaHash; }
    public boolean isActivo()          { return activo; }
    public String  getRol()            { return rol; }

    // ---- Setters ----
    public void setActivo(boolean a) { this.activo = a; }
    public void setTurno(String t)   { this.turno  = t; }
    public void setRol(String r)     { this.rol    = r; }

    @Override
    public String toString() {
        return String.format("Cajero[%s] %s | Turno: %s | Rol: %s", id, nombre, turno, rol);
    }
}
