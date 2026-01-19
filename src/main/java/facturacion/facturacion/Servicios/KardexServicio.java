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

    public List<Kardex> obtenerKardexPorProducto(Long productoId) {
        return kardexRepositorio.findByProductoProductoIdOrderByFechaAsc(productoId);
    }

    public Producto obtenerProductoPorId(Long id) {
        return productoRepositorio.findById(id).orElse(null);
    }
}
