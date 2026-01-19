package facturacion.facturacion.Repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import facturacion.facturacion.Entidades.CatalogoImpuesto;
import java.util.Optional;

public interface CatalogoImpuestoRepositorio extends JpaRepository<CatalogoImpuesto, Long> {
    Optional<CatalogoImpuesto> findByCodigoAndCodigoPorcentaje(String codigo, String codigoPorcentaje);
}
