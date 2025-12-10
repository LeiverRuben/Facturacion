package facturacion.facturacion.Repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import facturacion.facturacion.Entidades.Cliente;

@Repository
public interface  ClienteRepositorio extends JpaRepository<Cliente, Long> {

}
