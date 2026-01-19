package facturacion.facturacion.Controladores;

import org.springframework.web.bind.annotation.*;
import facturacion.facturacion.Entidades.GuiaDeRemision;
import facturacion.facturacion.Repositorios.GuiaDeRemisionRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import java.util.List;

@RestController
@RequestMapping("/api/guias-remision")
@RequiredArgsConstructor
@CrossOrigin("*")
public class GuiaDeRemisionControlador {

    private final GuiaDeRemisionRepositorio repositorio;

    @GetMapping
    public List<GuiaDeRemision> listarTodos() {
        return repositorio.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GuiaDeRemision> obtenerPorId(@PathVariable Long id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
