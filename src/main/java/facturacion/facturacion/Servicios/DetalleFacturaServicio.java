package facturacion.facturacion.Servicios;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import facturacion.facturacion.Entidades.DetalleFactura;
import facturacion.facturacion.Entidades.Factura;
import facturacion.facturacion.Entidades.Producto;
import facturacion.facturacion.Repositorios.DetalleFacturaRepositorio;
import facturacion.facturacion.Repositorios.FacturaRepositorio;
import facturacion.facturacion.Repositorios.ProductoRepositorio;

@Service
public class DetalleFacturaServicio {

    @Autowired 
    private ProductoRepositorio productoRepositorio; 
    @Autowired
    private FacturaRepositorio facturaRepositorio;
    @Autowired
    private DetalleFacturaRepositorio facturaDetalleRepositorio;

    public DetalleFactura guardar(DetalleFactura detalle) {
        
        // 1. VERIFICAR Y CARGAR ENTIDADES COMPLETAS (Factura y Producto)

        // a) Cargar Producto para obtener precio y tasa de IVA
        if (detalle.getProducto() == null || detalle.getProducto().getProductoId() == 0L) {
            throw new IllegalArgumentException("Debe especificar un Producto válido (productoId).");
        }
        
        Producto productoCompleto = productoRepositorio.findById(detalle.getProducto().getProductoId())
            .orElseThrow(() -> new IllegalArgumentException("Producto con ID " + detalle.getProducto().getProductoId() + " no existe."));
        
        detalle.setProducto(productoCompleto); // Asignar el objeto Producto completo

        // b) Cargar Factura (para asegurar que existe)
        if (detalle.getFactura() == null || detalle.getFactura().getFacturaId() == null) {
            throw new IllegalArgumentException("Debe especificar una Factura existente (facturaId).");
        }

        Factura facturaCompleta = facturaRepositorio.findById(detalle.getFactura().getFacturaId())
            .orElseThrow(() -> new IllegalArgumentException("Factura con ID " + detalle.getFactura().getFacturaId() + " no existe."));
        
        detalle.setFactura(facturaCompleta); // Asignar el objeto Factura completo

        // 2. CÁLCULOS ROBUSTOS

        // Usar precio del JSON, o el precio del producto si no viene en el JSON
        Double precioUnitarioUsar = detalle.getPrecioUnitario();
        if (precioUnitarioUsar == null) {
            precioUnitarioUsar = productoCompleto.getProductoPrecio();
        }

        if (precioUnitarioUsar == null || detalle.getCantidad() == null) {
            throw new IllegalStateException("El producto/detalle no tiene un precio o cantidad definida.");
        }
        
        double subtotal = detalle.getCantidad() * precioUnitarioUsar;
        double tasaIva = productoCompleto.getProductoTasa() != null ? productoCompleto.getProductoTasa() : 0.0;
        double iva = subtotal * (tasaIva / 100.0);
        double total = subtotal + iva;

        // 3. ASIGNACIÓN Y GUARDADO
        detalle.setPrecioUnitario(precioUnitarioUsar); // Sobrescribe el valor final usado
        detalle.setSubtotal(subtotal);
        detalle.setIva(iva);
        detalle.setTotal(total);

        return facturaDetalleRepositorio.save(detalle);
    }

    public List<DetalleFactura> listarAll() {
        return facturaDetalleRepositorio.findAll();
    }

    public DetalleFactura buscarId(Long id) {
        return facturaDetalleRepositorio.findById(id).orElse(null);
    }
public DetalleFactura actualizar(Long id, DetalleFactura detalleActualizado) {

    facturaDetalleRepositorio.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Detalle de Factura con ID " + id + " no existe."));

    detalleActualizado.setDetalleId(id);

    return guardar(detalleActualizado); 
    }

public void eliminar(Long id) {
    // Busca el detalle, si no existe lanza una excepción
    DetalleFactura detalle = facturaDetalleRepositorio.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Detalle de Factura con ID " + id + " no encontrado para eliminación."));
    
    facturaDetalleRepositorio.delete(detalle);
    }
}