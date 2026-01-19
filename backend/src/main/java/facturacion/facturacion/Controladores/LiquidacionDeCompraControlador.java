package facturacion.facturacion.Controladores;

import org.springframework.web.bind.annotation.*;
import facturacion.facturacion.Entidades.LiquidacionDeCompra;
import facturacion.facturacion.Repositorios.LiquidacionDeCompraRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import java.util.List;

@RestController
@RequestMapping("/api/liquidaciones-compra")
@RequiredArgsConstructor
@CrossOrigin("*")
public class LiquidacionDeCompraControlador {

    private final LiquidacionDeCompraRepositorio repositorio;

    @GetMapping
    public List<LiquidacionDeCompra> listarTodos() {
        return repositorio.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<LiquidacionDeCompra> obtenerPorId(@PathVariable Long id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
