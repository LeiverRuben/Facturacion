package facturacion.facturacion.Repositorios;

import facturacion.facturacion.Entidades.Compra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompraRepositorio extends JpaRepository<Compra, Long> {
}
