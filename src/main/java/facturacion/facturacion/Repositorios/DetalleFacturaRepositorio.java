package facturacion.facturacion.Repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import facturacion.facturacion.Entidades.DetalleFactura;

public interface DetalleFacturaRepositorio extends JpaRepository<DetalleFactura, Long>  {
    
}
