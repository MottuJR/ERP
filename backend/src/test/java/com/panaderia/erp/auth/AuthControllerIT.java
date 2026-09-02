package com.panaderia.erp.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panaderia.erp.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIT extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@panaderia.local";
    private static final String ADMIN_PASSWORD = "changeme123";

    @Autowired
    private MockMvc mockMvc;

    // Instancia propia y no la del contexto de Spring: desde Spring Boot 4 / Jackson 3, el
    // ObjectMapper que autoconfigura el framework es tools.jackson.databind.ObjectMapper, no
    // com.fasterxml.jackson.databind.ObjectMapper. Para parsear JSON en el test no hace falta
    // el bean de la app, alcanza con una instancia de Jackson 2 standalone.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loginConCredencialesValidasDevuelveToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginBody(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.usuario.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.usuario.rol").value("DUENO"));
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginBody(ADMIN_EMAIL, "password-incorrecta")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointProtegidoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meConTokenValidoDevuelveUsuarioAutenticado() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginBody(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).get("token").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ADMIN_EMAIL));
    }

    private String loginBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new LoginBody(email, password));
    }

    private record LoginBody(String email, String password) {
    }
}
