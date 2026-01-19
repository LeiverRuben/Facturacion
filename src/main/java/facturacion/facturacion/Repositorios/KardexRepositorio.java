package facturacion.facturacion.Repositorios;

import facturacion.facturacion.Entidades.Kardex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KardexRepositorio extends JpaRepository<Kardex, Long> {
    List<Kardex> findByProductoProductoIdOrderByFechaAsc(Long productoId);
}
