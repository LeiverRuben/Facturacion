package facturacion.facturacion.Servicios;

import facturacion.facturacion.Entidades.Kardex;
import facturacion.facturacion.Entidades.Producto;
import facturacion.facturacion.Repositorios.KardexRepositorio;
import facturacion.facturacion.Repositorios.ProductoRepositorio; // Asumiendo que existe
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KardexServicio {

    @Autowired
    private KardexRepositorio kardexRepositorio;

    @Autowired
    private ProductoRepositorio productoRepositorio; // Necesitamos actualizar saldo producto

    public void registrarMovimiento(Producto producto, String tipo, String detalle, Integer cantidad,
            Double costoUnitario, Double totalMovimiento) {
        Kardex k = new Kardex();
        k.setFecha(LocalDateTime.now());
        k.setProducto(producto);
        k.setTipoMovimiento(tipo);
        k.setDetalle(detalle);
        k.setCantidad(cantidad);
        k.setCostoUnitario(costoUnitario);
        k.setTotalMovimiento(totalMovimiento);

        // Calcular Saldos (Lógica simple: último saldo + movimiento)
        // En un sistema real se requiere recalcular promedio ponderado.
        // Aquí simplificaremos: Saldo = Stock Actual del Producto (que ya debería
        // haberse actualizado o se actualiza aqui)

        // Opción: Actualizar el producto aquí
        int currentStock = (producto.getProductoStock() != null) ? producto.getProductoStock() : 0;

        if (tipo.equals("ENTRADA")) {
            currentStock += cantidad;
            producto.setProductoStock(currentStock);
            // Actualizar precio? costo promedio? Por ahora simple.
            if (costoUnitario > 0)
                producto.setProductoPrecio(costoUnitario * 1.30);
        } else if (tipo.equals("SALIDA")) {
            currentStock -= cantidad;
            producto.setProductoStock(currentStock);
        }
        productoRepositorio.save(producto);

        k.setSaldoCantidad(producto.getProductoStock());
        k.setSaldoTotal(producto.getProductoStock() * costoUnitario); // Aproximación
        k.setCostoPromedio(costoUnitario);

        kardexRepositorio.save(k);
    }

    // Nuevo método para cuando el stock YA FUE actualizado externamente (ej: al
    // crear producto o editar manual)
    public void registrarMovimientoDirecto(Producto producto, String tipo, String detalle, Integer cantidad,
            Double costoUnitario) {
        Kardex k = new Kardex();
        k.setFecha(LocalDateTime.now());
        k.setProducto(producto);
        k.setTipoMovimiento(tipo);
        k.setDetalle(detalle);
        k.setCantidad(Math.abs(cantidad)); // Siempre positivo en visualización
        k.setCostoUnitario(costoUnitario);
        k.setTotalMovimiento(k.getCantidad() * costoUnitario);

        // El producto YA TIENE el stock final actualizado
        k.setSaldoCantidad(producto.getProductoStock());
        // Saldo Total Estimado
        k.setSaldoTotal(producto.getProductoStock() * costoUnitario);

        kardexRepositorio.save(k);
    }

    public List<Kardex> obtenerKardexPorProducto(Long productoId) {
        return kardexRepositorio.findByProductoProductoIdOrderByFechaAsc(productoId);
    }

    public List<Kardex> listarTodos() {
        return kardexRepositorio.findAll();
    }

    public Producto obtenerProductoPorId(Long id) {
        return productoRepositorio.findById(id).orElse(null);
    }

    public void sincronizarInventario() {
        List<Producto> productos = productoRepositorio.findAll();
        for (Producto p : productos) {
            // Verificar si tiene movimientos
            List<Kardex> movimientos = kardexRepositorio.findByProductoProductoIdOrderByFechaAsc(p.getProductoId());
            if (movimientos.isEmpty() && p.getProductoStock() > 0) {
                // Crear entrada inicial
                registrarMovimientoDirecto(
                        p,
                        "ENTRADA",
                        "Sincronización de Inventario (Inicial)",
                        p.getProductoStock(),
                        p.getProductoPrecio() // Costo estimado
                );
            }
        }
    }
}
