package facturacion.facturacion.Controladores;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import facturacion.facturacion.Entidades.Usuario;
import facturacion.facturacion.Servicios.UsuarioServicio;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class UsuarioControlador {

    @Autowired
    private UsuarioServicio usuarioServicio;

    @GetMapping
    public List<Usuario> listarUsuarios() {
        return usuarioServicio.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtenerUsuario(@PathVariable Long id) {
        Usuario usuario = usuarioServicio.listaUsuario(id);
        if (usuario != null) {
            return ResponseEntity.ok(usuario);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Usuario> crearUsuario(@RequestBody Usuario usuario) {
        // En una app real, validar que no exista el username/email
        Usuario nuevoUsuario = usuarioServicio.guardar(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoUsuario);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizarUsuario(@PathVariable Long id, @RequestBody Usuario usuarioDetalles) {
        Usuario usuario = usuarioServicio.listaUsuario(id);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        usuario.setNombre(usuarioDetalles.getNombre());
        usuario.setCorreo(usuarioDetalles.getCorreo());
        usuario.setUsername(usuarioDetalles.getUsername());
        usuario.setEstaActivo(usuarioDetalles.getEstaActivo());

        // Actualizar rol si se envía
        if (usuarioDetalles.getTipoUsuario() != null) {
            usuario.setTipoUsuario(usuarioDetalles.getTipoUsuario());
        }

        // Si se envía contraseña nueva, actualizarla (la lógica de servicio la
        // encriptará)
        // Nota: esto debería mejorarse para no re-encriptar si es la misma, pero por
        // ahora asumimos cambio
        if (usuarioDetalles.getPassword() != null && !usuarioDetalles.getPassword().isEmpty()) {
            usuario.setPassword(usuarioDetalles.getPassword());
        }

        Usuario usuarioActualizado = usuarioServicio.guardar(usuario);
        return ResponseEntity.ok(usuarioActualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        if (usuarioServicio.listaUsuario(id) == null) {
            return ResponseEntity.notFound().build();
        }
        usuarioServicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
