package facturacion.facturacion.Repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import facturacion.facturacion.Entidades.NotaDeCredito;

public interface NotaDeCreditoRepositorio extends JpaRepository<NotaDeCredito, Long> {
}
