package facturacion.facturacion.Repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import facturacion.facturacion.Entidades.ConfiguracionSRI;

@Repository
public interface ConfiguracionSriRepositorio extends JpaRepository<ConfiguracionSRI, Long> {
    
}
