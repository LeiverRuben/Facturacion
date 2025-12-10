package facturacion.facturacion.Repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import facturacion.facturacion.Entidades.FacturaPago;

public interface FacturaPagoRepositorio extends JpaRepository<FacturaPago, Long> {
    
}
