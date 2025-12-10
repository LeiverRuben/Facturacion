package facturacion.facturacion.Repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import facturacion.facturacion.Entidades.Categoria;

@Repository
public interface CategoriaRepositorio extends JpaRepository<Categoria, Long> {
    
}
