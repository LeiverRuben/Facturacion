package facturacion.facturacion.Repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import facturacion.facturacion.Entidades.Factura;

import facturacion.facturacion.Entidades.SesionCaja;
import java.util.List;

@Repository
public interface FacturaRepositorio extends JpaRepository<Factura, Long> {
    List<Factura> findBySesionCaja(SesionCaja sesionCaja);
}
