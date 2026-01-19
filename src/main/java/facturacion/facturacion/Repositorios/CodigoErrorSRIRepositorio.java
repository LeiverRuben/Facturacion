package facturacion.facturacion.Repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import facturacion.facturacion.Entidades.CodigoErrorSRI;
import java.util.Optional;

public interface CodigoErrorSRIRepositorio extends JpaRepository<CodigoErrorSRI, Long> {
    Optional<CodigoErrorSRI> findByCodigo(String codigo);
}
