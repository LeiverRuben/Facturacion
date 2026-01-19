package facturacion.facturacion.Repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import facturacion.facturacion.Entidades.Caja;

@Repository
public interface CajaRepositorio extends JpaRepository<Caja, Long> {
}
