package supermercado.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InventarioFrameTest {

    @Test
    void debeCalcularElNuevoStockParaAgregarYRestarUnidades() {
        assertEquals(15, InventarioFrame.calcularNuevoStock(10, 5, "AGREGAR"));
        assertEquals(5, InventarioFrame.calcularNuevoStock(10, 5, "RESTAR"));
        assertEquals(8, InventarioFrame.calcularNuevoStock(10, 8, "EXACTA"));
    }
}
