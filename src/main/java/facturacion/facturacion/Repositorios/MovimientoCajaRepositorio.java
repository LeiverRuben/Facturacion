package facturacion.facturacion.Repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import facturacion.facturacion.Entidades.MovimientoCaja;
import facturacion.facturacion.Entidades.SesionCaja;

@Repository
public interface MovimientoCajaRepositorio extends JpaRepository<MovimientoCaja, Long> {
    List<MovimientoCaja> findBySesionCaja(SesionCaja sesionCaja);
}
