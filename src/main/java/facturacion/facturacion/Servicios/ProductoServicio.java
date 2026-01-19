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

    @Autowired
    private facturacion.facturacion.Repositorios.CategoriaRepositorio categoriaRepositorio;

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
        // 1. Recuperar el producto existente
        Producto productoExistente = productoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto con ID " + id + " no existe."));

        // 2. Actualizar solo los campos permitidos
        productoExistente.setProductoNombre(productoActualizado.getProductoNombre());
        productoExistente.setProductoDescripcion(productoActualizado.getProductoDescripcion());
        productoExistente.setProductoPrecio(productoActualizado.getProductoPrecio());
        productoExistente.setProductoStock(productoActualizado.getProductoStock());
        productoExistente.setProductoEstado(productoActualizado.getProductoEstado());

        // 3. Manejo seguro de la categoría (Fetching managed entity)
        if (productoActualizado.getCategoria() != null
                && productoActualizado.getCategoria().getCategoriaId() != null
                && productoActualizado.getCategoria().getCategoriaId() > 0) {
            facturacion.facturacion.Entidades.Categoria copiaCategoria = categoriaRepositorio
                    .findById(productoActualizado.getCategoria().getCategoriaId())
                    .orElse(null);
            productoExistente.setCategoria(copiaCategoria);
        }

        // 4. Guardar los cambios
        return productoRepositorio.save(productoExistente);
    }

    public void eliminar(long id) {
        // Se recomienda validar si el objeto existe antes de intentar eliminar
        productoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto con ID " + id + " no encontrado para eliminación."));

        productoRepositorio.deleteById(id);
    }
}
