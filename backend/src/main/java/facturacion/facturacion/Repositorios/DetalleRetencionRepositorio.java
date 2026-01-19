package facturacion.facturacion.Repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import facturacion.facturacion.Entidades.DetalleRetencion;

@Repository
public interface DetalleRetencionRepositorio extends JpaRepository<DetalleRetencion, Long> {
}
