package facturacion.facturacion.Controladores;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import facturacion.facturacion.Entidades.DetalleFactura;
import facturacion.facturacion.Servicios.DetalleFacturaServicio;

@RestController
@RequestMapping("/api/factura-detalle")

public class DetalleFacturaControlador {
        @Autowired
    private DetalleFacturaServicio facturaDetalleServicio;

    @PostMapping
    public DetalleFactura guardar(@RequestBody DetalleFactura detalle) {
        return facturaDetalleServicio.guardar(detalle);
    }

    @GetMapping
    public List<DetalleFactura> listar() {
        return facturaDetalleServicio.listarAll();
    }
    // MÉTODO PUT PARA ACTUALIZAR POR ID
    @PutMapping("/{id}") 
    public DetalleFactura actualizar(@PathVariable Long id, @RequestBody DetalleFactura detalle) {
        // Debes implementar el método actualizar en el servicio
        return facturaDetalleServicio.actualizar(id, detalle); 
    }

    @GetMapping("/{id}")
    public DetalleFactura obtenerPorId(@PathVariable Long id) {
        return facturaDetalleServicio.buscarId(id);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        facturaDetalleServicio.eliminar(id);
    }
}
