package facturacion.facturacion.Repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import facturacion.facturacion.Entidades.NotaDeDebito;

public interface NotaDeDebitoRepositorio extends JpaRepository<NotaDeDebito, Long> {
}
