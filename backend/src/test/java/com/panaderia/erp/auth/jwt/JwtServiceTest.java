package com.panaderia.erp.auth.jwt;

import com.panaderia.erp.core.usuario.Rol;
import com.panaderia.erp.core.usuario.Usuario;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-with-at-least-32-characters-long";

    private final JwtService jwtService = new JwtService(SECRET, 60);
    private final Usuario usuario = new Usuario("Ana", "ana@panaderia.local", "hash", Rol.VENDEDOR);

    @Test
    void generaUnTokenDelQueSePuedeExtraerElEmail() {
        String token = jwtService.generarToken(usuario);

        assertThat(jwtService.extraerEmail(token)).isEqualTo("ana@panaderia.local");
    }

    @Test
    void unTokenValidoParaSuPropioEmailEsValido() {
        String token = jwtService.generarToken(usuario);

        assertThat(jwtService.esTokenValido(token, "ana@panaderia.local")).isTrue();
    }

    @Test
    void unTokenNoEsValidoParaOtroEmail() {
        String token = jwtService.generarToken(usuario);

        assertThat(jwtService.esTokenValido(token, "otro@panaderia.local")).isFalse();
    }

    @Test
    void unTokenExpiradoNoEsValido() throws InterruptedException {
        JwtService jwtServiceExpirado = new JwtService(SECRET, 0);
        String token = jwtServiceExpirado.generarToken(usuario);

        Thread.sleep(50);

        assertThat(jwtServiceExpirado.esTokenValido(token, "ana@panaderia.local")).isFalse();
    }
}
