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

    @Autowired
    private KardexServicio kardexServicio;

    public Producto guardar(Producto producto) {
        Producto guardado = productoRepositorio.save(producto);
        // Registrar Kardex Inicial si hay stock
        if (guardado.getProductoStock() > 0) {
            kardexServicio.registrarMovimientoDirecto(
                    guardado,
                    "ENTRADA",
                    "Inventario Inicial",
                    guardado.getProductoStock(),
                    guardado.getProductoPrecio() // Usamos precio como costo ref por ahora
            );
        }
        return guardado;
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
        Integer oldStock = productoExistente.getProductoStock();
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
        // 4. Guardar los cambios
        Producto actualizado = productoRepositorio.save(productoExistente);

        // 5. Registrar en Kardex si hubo cambio de stock
        if (oldStock != null && !oldStock.equals(actualizado.getProductoStock())) {
            int diff = actualizado.getProductoStock() - oldStock;
            String tipo = diff > 0 ? "ENTRADA" : "SALIDA";
            String detalle = "Ajuste Manual de Inventario";

            kardexServicio.registrarMovimientoDirecto(
                    actualizado,
                    tipo,
                    detalle,
                    Math.abs(diff),
                    actualizado.getProductoPrecio());
        }

        return actualizado;
    }

    public void eliminar(long id) {
        // Se recomienda validar si el objeto existe antes de intentar eliminar
        productoRepositorio.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto con ID " + id + " no encontrado para eliminación."));

        productoRepositorio.deleteById(id);
    }
}
