package supermercado.modelo;

public class Cliente {

    private String nit;
    private String nombre;
    private String telefono;
    private String email;
    private String direccion;

    public Cliente(String nit, String nombre, String telefono,
                   String email, String direccion) {
        this.nit       = nit;
        this.nombre    = nombre;
        this.telefono  = telefono;
        this.email     = email;
        this.direccion = direccion;
    }

    public boolean validarNit() {
        return nit != null && nit.matches("\\d{6,15}");
    }

    public String getDatos() {
        return nombre + "  |  NIT: " + nit + "  |  " + telefono;
    }

    // ---- Getters ----
    public String getNit()       { return nit; }
    public String getNombre()    { return nombre; }
    public String getTelefono()  { return telefono; }
    public String getEmail()     { return email; }
    public String getDireccion() { return direccion; }

    // ---- Setters ----
    public void setNombre(String n)    { this.nombre    = n; }
    public void setTelefono(String t)  { this.telefono  = t; }
    public void setEmail(String e)     { this.email     = e; }
    public void setDireccion(String d) { this.direccion = d; }

    @Override
    public String toString() { return getDatos(); }
}
