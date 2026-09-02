package com.panaderia.erp;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Patrón "contenedor singleton" de Testcontainers: el Postgres se levanta una única vez por JVM
 * (bloque estático) y se comparte entre todas las clases de test que extienden esta base, sin
 * usar {@code @Testcontainers}/{@code @Container}. Con esas anotaciones, la extensión de JUnit 5
 * llama a {@code stop()} sobre el contenedor al terminar la ÚLTIMA clase de test que lo usa
 * primero — como el campo es el mismo objeto compartido, eso lo apagaba para el resto de las
 * clases que corrían después en la misma JVM. Ryuk igual lo limpia al terminar la JVM.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
