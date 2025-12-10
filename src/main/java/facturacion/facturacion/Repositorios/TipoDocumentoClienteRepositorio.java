package facturacion.facturacion.Repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import facturacion.facturacion.Entidades.Cliente;
import facturacion.facturacion.Entidades.TipoDocumentoCliente;

@Repository
public interface TipoDocumentoClienteRepositorio extends JpaRepository<TipoDocumentoCliente, String> {

    List<TipoDocumentoCliente> findByCliente(Cliente cliente);
    
}