package com.panaderia.erp.produccion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panaderia.erp.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cubre de punta a punta lo más sensible de la Fase 2: crear un insumo, un producto elaborado
 * y su receta, confirmar una orden de producción (consumo de insumo + alta de producto) y
 * confirmar una compra (alta de insumo + actualización de costo). Usa Postgres real vía
 * Testcontainers, igual que {@link com.panaderia.erp.auth.AuthControllerIT}.
 */
class ProduccionCompraFlowIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@panaderia.local";
    private static final String ADMIN_PASSWORD = "changeme123";

    @Autowired
    private MockMvc mockMvc;

    // Instancia propia y no la del contexto de Spring: desde Spring Boot 4 / Jackson 3, el
    // ObjectMapper que autoconfigura el framework es tools.jackson.databind.ObjectMapper, no
    // com.fasterxml.jackson.databind.ObjectMapper. Para parsear JSON en el test no hace falta
    // el bean de la app, alcanza con una instancia de Jackson 2 standalone.
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String token;
    private Long categoriaId;
    private Long insumoHarinaId;
    private Long productoPanId;

    // JUnit 5 crea una instancia nueva de la clase por cada @Test, así que este sufijo es
    // distinto en cada método — evita que los tests choquen entre sí por nombre de categoría
    // o código de barras duplicado (ambos con constraint UNIQUE), ya que la base de este test
    // es real (Testcontainers) y no se resetea entre métodos.
    private final String sufijo = java.util.UUID.randomUUID().toString().substring(0, 8);

    @BeforeEach
    void setUp() throws Exception {
        token = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        categoriaId = crear("/api/categorias", """
                { "nombre": "Panificados-%s" }
                """.formatted(sufijo)).get("id").asLong();

        insumoHarinaId = crear("/api/inventario/insumos", """
                { "nombre": "Harina", "unidadMedida": "KG", "stockMinimo": 5, "costoUnitario": 100.00 }
                """).get("id").asLong();

        // Stock inicial del insumo: sin esto, producir fallaría por falta de stock.
        crear("/api/inventario/movimientos", """
                { "itemTipo": "INSUMO", "itemId": %d, "tipo": "ENTRADA", "cantidad": 20, "motivo": "Stock inicial de prueba" }
                """.formatted(insumoHarinaId));

        productoPanId = crear("/api/productos", """
                {
                  "nombre": "Pan francés",
                  "categoriaId": %d,
                  "tipo": "ELABORADO",
                  "seVendePorPeso": false,
                  "precioVenta": 1500.00,
                  "unidadMedida": "UNIDAD",
                  "codigoBarras": "779%s",
                  "stockMinimo": 0
                }
                """.formatted(categoriaId, sufijo)).get("id").asLong();

        crear("/api/produccion/recetas", """
                { "productoId": %d, "items": [ { "insumoId": %d, "cantidad": 0.5 } ] }
                """.formatted(productoPanId, insumoHarinaId));
    }

    @Test
    void confirmarOrdenDeProduccionDescuentaElInsumoYSumaElStockDelProducto() throws Exception {
        // 10 panes * 0.5 kg de harina por pan = 5 kg de harina consumidos
        crear("/api/produccion/ordenes", """
                { "productoId": %d, "cantidad": 10 }
                """.formatted(productoPanId));

        JsonNode harina = obtener("/api/inventario/insumos/" + insumoHarinaId);
        assertThat(harina.get("stockActual").decimalValue()).isEqualByComparingTo("15.000");

        JsonNode pan = obtener("/api/productos/" + productoPanId);
        assertThat(pan.get("stockActual").decimalValue()).isEqualByComparingTo("10.000");
    }

    @Test
    void siNoHayStockSuficienteDeInsumoLaOrdenDeProduccionFalla() throws Exception {
        // 20 kg disponibles / 0.5 kg por pan = alcanza para 40; pedir 1000 panes no alcanza
        mockMvc.perform(autenticado(post("/api/produccion/ordenes"))
                        .contentType("application/json")
                        .content("""
                                { "productoId": %d, "cantidad": 1000 }
                                """.formatted(productoPanId)))
                .andExpect(status().isConflict());

        // No debe haber quedado nada aplicado a medias
        JsonNode harina = obtener("/api/inventario/insumos/" + insumoHarinaId);
        assertThat(harina.get("stockActual").decimalValue()).isEqualByComparingTo("20.000");

        JsonNode pan = obtener("/api/productos/" + productoPanId);
        assertThat(pan.get("stockActual").decimalValue()).isEqualByComparingTo("0.000");
    }

    @Test
    void confirmarCompraSumaStockDeInsumoYActualizaElCostoUnitario() throws Exception {
        Long proveedorId = crear("/api/proveedores", """
                { "nombre": "Molino SA" }
                """).get("id").asLong();

        crear("/api/compras", """
                { "proveedorId": %d, "items": [ { "insumoId": %d, "cantidad": 50, "costoUnitario": 130.50 } ] }
                """.formatted(proveedorId, insumoHarinaId));

        JsonNode harina = obtener("/api/inventario/insumos/" + insumoHarinaId);
        // 20 kg iniciales + 50 kg comprados
        assertThat(harina.get("stockActual").decimalValue()).isEqualByComparingTo("70.000");
        assertThat(harina.get("costoUnitario").decimalValue()).isEqualByComparingTo("130.50");
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                { "email": "%s", "password": "%s" }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private JsonNode crear(String url, String jsonBody) throws Exception {
        String response = mockMvc.perform(autenticado(post(url))
                        .contentType("application/json")
                        .content(jsonBody))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response);
    }

    private JsonNode obtener(String url) throws Exception {
        String response = mockMvc.perform(autenticado(get(url)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response);
    }

    private MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + token);
    }
}
