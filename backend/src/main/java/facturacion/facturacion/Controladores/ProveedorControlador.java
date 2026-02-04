package facturacion.facturacion.Controladores;

import facturacion.facturacion.Entidades.Proveedor;
import facturacion.facturacion.Servicios.ProveedorServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
@CrossOrigin(origins = "*")
public class ProveedorControlador {

    @Autowired
    private ProveedorServicio proveedorServicio;

    @GetMapping
    public List<Proveedor> listar() {
        return proveedorServicio.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Proveedor> obtener(@PathVariable Long id) {
        Proveedor proveedor = proveedorServicio.obtenerPorId(id);
        if (proveedor != null) {
            return ResponseEntity.ok(proveedor);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public Proveedor crear(@RequestBody @jakarta.validation.Valid Proveedor proveedor) {
        return proveedorServicio.guardar(proveedor);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Proveedor> actualizar(@PathVariable Long id,
            @RequestBody @jakarta.validation.Valid Proveedor proveedor) {
        Proveedor actualizado = proveedorServicio.actualizar(id, proveedor);
        if (actualizado != null) {
            return ResponseEntity.ok(actualizado);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        proveedorServicio.eliminar(id);
        return ResponseEntity.ok().build();
    }
}
