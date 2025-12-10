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

import facturacion.facturacion.Entidades.Producto;
import facturacion.facturacion.Servicios.ProductoServicio;

@RestController
@RequestMapping("/api/productos")
public class ProductoControlador {

    @Autowired
    private ProductoServicio productoServicio;

    @PostMapping
    public Producto guardar(@RequestBody Producto producto) {
        return productoServicio.guardar(producto);
    }

    @GetMapping
    public List<Producto> listar() {
        return productoServicio.listarAll();
    }

    @GetMapping("/{id}")
    public Producto obtenerPorId(@PathVariable Long id) {
        return productoServicio.buscarId(id);
    }
    @PutMapping("/{id}")
    public Producto actualizar(@PathVariable Long id, @RequestBody Producto producto) {
    return productoServicio.actualizar(id, producto); 
}

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        productoServicio.eliminar(id);
    }
}
