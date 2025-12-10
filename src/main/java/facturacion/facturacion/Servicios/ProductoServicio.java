package facturacion.facturacion.Servicios;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import facturacion.facturacion.Entidades.Producto;
import facturacion.facturacion.Repositorios.ProductoRepositorio;

@Service
public class ProductoServicio {
    
    @Autowired
    private ProductoRepositorio productoRepositorio;

    public Producto guardar(Producto producto) {
        return productoRepositorio.save(producto);
    }

    public List<Producto> listarAll() {
        return productoRepositorio.findAll();
    }

    public Producto buscarId(long id) {
        return productoRepositorio.findById(id).orElse(null);
    }
public Producto actualizar(Long id, Producto productoActualizado) {
    // 1. Verificar si el Producto a actualizar existe
    productoRepositorio.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Producto con ID " + id + " no existe."));

    // 2. Asignar el ID al objeto (necesario para que JPA sepa que es un UPDATE)
    productoActualizado.setProductoId(id);

    // 3. Reutilizar 'guardar' para realizar el UPDATE
    return productoRepositorio.save(productoActualizado);
}

public void eliminar(long id) {
    // Se recomienda validar si el objeto existe antes de intentar eliminar
    productoRepositorio.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Producto con ID " + id + " no encontrado para eliminación."));
        
    productoRepositorio.deleteById(id);
}
}
