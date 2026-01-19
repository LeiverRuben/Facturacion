package facturacion.facturacion.Repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import facturacion.facturacion.Entidades.Producto;

@Repository
public interface ProductoRepositorio extends JpaRepository<Producto, Long>{
    
}
