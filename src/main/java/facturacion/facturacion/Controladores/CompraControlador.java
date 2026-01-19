package facturacion.facturacion.Controladores;

import facturacion.facturacion.Entidades.Compra;
import facturacion.facturacion.Servicios.CompraServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compras")
// @CrossOrigin handled by SecurityConfig
public class CompraControlador {

    @Autowired
    private CompraServicio compraServicio;

    @GetMapping
    public List<Compra> listar() {
        return compraServicio.listarTodas();
    }

    @PostMapping
    public ResponseEntity<Compra> crear(@RequestBody Compra compra) {
        return ResponseEntity.ok(compraServicio.guardarCompra(compra));
    }
}
