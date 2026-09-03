package com.panaderia.erp.auth.controller;

import com.panaderia.erp.auth.dto.ActualizarUsuarioRequest;
import com.panaderia.erp.auth.dto.CrearUsuarioRequest;
import com.panaderia.erp.auth.dto.UsuarioResponse;
import com.panaderia.erp.core.exception.ConflictoException;
import com.panaderia.erp.core.exception.RecursoNoEncontradoException;
import com.panaderia.erp.core.usuario.Usuario;
import com.panaderia.erp.core.usuario.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('DUENO')")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ConflictoException("Ya existe un usuario con ese email");
        }

        Usuario usuario = new Usuario(
                request.nombre(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.rol());
        usuario.setPorcentajeComision(request.porcentajeComision());

        usuario = usuarioRepository.save(usuario);

        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(usuario));
    }

    @PutMapping("/{id}")
    @Transactional
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));

        if (!usuario.getEmail().equalsIgnoreCase(request.email()) && usuarioRepository.existsByEmail(request.email())) {
            throw new ConflictoException("Ya existe un usuario con ese email");
        }

        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setRol(request.rol());
        usuario.setActivo(request.activo());
        usuario.setPorcentajeComision(request.porcentajeComision());

        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        return UsuarioResponse.from(usuario);
    }
}
