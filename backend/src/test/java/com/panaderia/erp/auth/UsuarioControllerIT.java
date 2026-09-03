package com.panaderia.erp.auth;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CRUD de usuarios (pantalla de administración de la Fase 4/5): alta, listado y edición de
 * atributos (incluido el porcentaje de comisión), todo restringido a DUENO. Usa Postgres real
 * vía Testcontainers, igual que {@link AuthControllerIT}.
 */
class UsuarioControllerIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@panaderia.local";
    private static final String ADMIN_PASSWORD = "changeme123";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Ver ProduccionCompraFlowIT: la base es real y no se resetea entre tests, así que el
    // sufijo evita choques por el email UNIQUE si este test corre más de una vez en la JVM.
    private final String sufijo = java.util.UUID.randomUUID().toString().substring(0, 8);

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                { "email": "%s", "password": "%s" }
                                """.formatted(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        token = objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void crearUsuarioLoAgregaAlListado() throws Exception {
        JsonNode creado = crear("""
                { "nombre": "Vendedora Test", "email": "vendedora-%s@panaderia.local",
                  "password": "clave1234", "rol": "VENDEDOR", "porcentajeComision": 5.00 }
                """.formatted(sufijo));

        assertThat(creado.get("rol").asText()).isEqualTo("VENDEDOR");
        assertThat(creado.get("porcentajeComision").decimalValue()).isEqualByComparingTo("5.00");

        String listadoJson = mockMvc.perform(autenticado(get("/api/usuarios")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode listado = objectMapper.readTree(listadoJson);

        boolean estaEnElListado = false;
        for (JsonNode usuario : listado) {
            if (usuario.get("id").asLong() == creado.get("id").asLong()) {
                estaEnElListado = true;
            }
        }
        assertThat(estaEnElListado).isTrue();
    }

    @Test
    void crearUsuarioConEmailDuplicadoFalla() throws Exception {
        mockMvc.perform(autenticado(post("/api/usuarios"))
                        .contentType("application/json")
                        .content("""
                                { "nombre": "Otro Admin", "email": "%s", "password": "clave1234", "rol": "ENCARGADO" }
                                """.formatted(ADMIN_EMAIL)))
                .andExpect(status().isConflict());
    }

    @Test
    void actualizarUsuarioCambiaSusAtributosSinTocarLaContraseñaSiNoSeEnvia() throws Exception {
        Long id = crear("""
                { "nombre": "Encargado Test", "email": "encargado-%s@panaderia.local",
                  "password": "clave1234", "rol": "ENCARGADO" }
                """.formatted(sufijo)).get("id").asLong();

        String actualizadoJson = mockMvc.perform(autenticado(put("/api/usuarios/" + id))
                        .contentType("application/json")
                        .content("""
                                { "nombre": "Encargado Editado", "email": "encargado-%s@panaderia.local",
                                  "rol": "VENDEDOR", "activo": true, "porcentajeComision": 8.50 }
                                """.formatted(sufijo)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode actualizado = objectMapper.readTree(actualizadoJson);

        assertThat(actualizado.get("nombre").asText()).isEqualTo("Encargado Editado");
        assertThat(actualizado.get("rol").asText()).isEqualTo("VENDEDOR");
        assertThat(actualizado.get("activo").asBoolean()).isTrue();
        assertThat(actualizado.get("porcentajeComision").decimalValue()).isEqualByComparingTo("8.50");

        // La contraseña original (no reenviada) sigue funcionando.
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                { "email": "encargado-%s@panaderia.local", "password": "clave1234" }
                                """.formatted(sufijo)))
                .andExpect(status().isOk());
    }

    private JsonNode crear(String jsonBody) throws Exception {
        String response = mockMvc.perform(autenticado(post("/api/usuarios"))
                        .contentType("application/json")
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response);
    }

    private MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + token);
    }
}
