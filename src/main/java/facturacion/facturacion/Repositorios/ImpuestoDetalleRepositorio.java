package facturacion.facturacion.Repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import facturacion.facturacion.Entidades.ImpuestoDetalle;

public interface ImpuestoDetalleRepositorio extends JpaRepository<ImpuestoDetalle, Long> {
    
}
