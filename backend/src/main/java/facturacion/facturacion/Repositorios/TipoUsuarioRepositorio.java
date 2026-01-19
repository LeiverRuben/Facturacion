package facturacion.facturacion.Repositorios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import facturacion.facturacion.Entidades.TipoUsuario;

@Repository
public interface TipoUsuarioRepositorio extends JpaRepository<TipoUsuario, Long> {
    Optional<TipoUsuario> findByRol(String rol);
}
