package facturacion.facturacion.Controladores;

import org.springframework.web.bind.annotation.*;
import facturacion.facturacion.Entidades.NotaDeCredito;
import facturacion.facturacion.Repositorios.NotaDeCreditoRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import java.util.List;

@RestController
@RequestMapping("/api/notas-credito")
@RequiredArgsConstructor
@CrossOrigin("*")
public class NotaDeCreditoControlador {

    private final NotaDeCreditoRepositorio repositorio;

    @GetMapping
    public List<NotaDeCredito> listarTodos() {
        return repositorio.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotaDeCredito> obtenerPorId(@PathVariable Long id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
