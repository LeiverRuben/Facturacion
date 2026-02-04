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
    private final facturacion.facturacion.Servicios.NotaCreditoServicio servicio;

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

    @PostMapping("/crear")
    public ResponseEntity<?> crear(@RequestBody facturacion.facturacion.Dto.NotaCreditoDTO dto) {
        try {
            return ResponseEntity.ok(servicio.crear(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al crear Nota de Crédito: " + e.getMessage());
        }
    }

    @PostMapping("/enviar-sri/{id}")
    public ResponseEntity<?> enviarSri(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(servicio.enviarSRI(id));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error al enviar al SRI: " + e.getMessage());
        }
    }
}
