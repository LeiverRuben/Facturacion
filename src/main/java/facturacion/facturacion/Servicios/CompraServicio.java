package facturacion.facturacion.Servicios;

import facturacion.facturacion.Entidades.Compra;
import facturacion.facturacion.Entidades.DetalleCompra;
import facturacion.facturacion.Repositorios.CompraRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CompraServicio {

    @Autowired
    private CompraRepositorio compraRepositorio;

    @Autowired
    private KardexServicio kardexServicio;

    public List<Compra> listarTodas() {
        return compraRepositorio.findAll();
    }

    @Transactional
    public Compra guardarCompra(Compra compra) {
        // Asignar fecha si no viene
        if (compra.getFechaEmision() == null)
            compra.setFechaEmision(LocalDateTime.now());
        compra.setFechaRegistro(LocalDateTime.now());
        compra.setEstado("RECIBIDA"); // Asumimos recepción inmediata

        // Calcular totales si no vienen
        double total = 0;
        for (DetalleCompra det : compra.getDetalles()) {
            det.setCompra(compra);
            det.setSubtotal(det.getCantidad() * det.getCostoUnitario());
            total += det.getSubtotal();
        }
        compra.setTotal(total); // Simplificado sin impuestos por brevedad

        Compra guardada = compraRepositorio.save(compra);

        // Registrar en Kardex
        for (DetalleCompra det : guardada.getDetalles()) {
            // CRÍTICO: El producto que viene en el detalle puede tener solo el ID.
            // Cargamos el producto completo para tener el stock actual.
            // (Usamos det.getProducto().getProductoId() porque es lo único seguro)
            Long prodId = det.getProducto().getProductoId();
            facturacion.facturacion.Entidades.Producto prodReal = kardexServicio.obtenerProductoPorId(prodId);

            if (prodReal != null) {
                kardexServicio.registrarMovimiento(
                        prodReal,
                        "ENTRADA",
                        "Compra #" + guardada.getNumeroComprobante(),
                        det.getCantidad(),
                        det.getCostoUnitario(),
                        det.getSubtotal());
            }
        }

        return guardada;
    }
}
