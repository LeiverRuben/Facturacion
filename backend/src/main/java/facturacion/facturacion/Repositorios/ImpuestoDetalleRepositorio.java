package facturacion.facturacion.Repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import facturacion.facturacion.Entidades.ImpuestoDetalle;

public interface ImpuestoDetalleRepositorio extends JpaRepository<ImpuestoDetalle, Long> {
    java.util.List<facturacion.facturacion.Entidades.ImpuestoDetalle> findByDetalleFactura(
            facturacion.facturacion.Entidades.DetalleFactura detalleFactura);
}
