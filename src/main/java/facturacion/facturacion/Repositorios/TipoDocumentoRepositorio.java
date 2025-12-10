package facturacion.facturacion.Repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import facturacion.facturacion.Entidades.TipoDocumento;

@Repository
public interface TipoDocumentoRepositorio extends JpaRepository<TipoDocumento,Long> {
    
}
