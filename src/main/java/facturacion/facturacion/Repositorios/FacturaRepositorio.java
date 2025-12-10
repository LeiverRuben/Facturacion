package facturacion.facturacion.Repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import facturacion.facturacion.Entidades.Factura;

@Repository
public interface  FacturaRepositorio extends JpaRepository<Factura, Long> {
    
}
