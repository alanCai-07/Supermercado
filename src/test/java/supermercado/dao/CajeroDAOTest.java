package supermercado.dao;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import supermercado.modelo.Cajero;

class CajeroDAOTest {

    @Test
    void deberiaGenerarHashValidoParaCredencialesDeCajero() {
        String password = "Caja123";
        String hash = CajeroDAO.hashPassword(password);

        Cajero cajero = new Cajero("C999", "Caja Nuevo", "Mañana", hash);
        assertTrue(cajero.autenticar(password));
    }
}
