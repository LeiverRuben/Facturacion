package facturacion.facturacion.Controladores;

import org.springframework.web.bind.annotation.*;
import facturacion.facturacion.Entidades.ComprobanteRetencion;
import facturacion.facturacion.Repositorios.ComprobanteRetencionRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import java.util.List;

@RestController
@RequestMapping("/api/comprobantes-retencion")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ComprobanteRetencionControlador {

    private final ComprobanteRetencionRepositorio repositorio;

    @GetMapping
    public List<ComprobanteRetencion> listarTodos() {
        return repositorio.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComprobanteRetencion> obtenerPorId(@PathVariable Long id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
