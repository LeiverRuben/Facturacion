package facturacion.facturacion.Repositorios;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import facturacion.facturacion.Entidades.SesionCaja;
import facturacion.facturacion.Entidades.Usuario;

@Repository
public interface SesionCajaRepositorio extends JpaRepository<SesionCaja, Long> {

    // Buscar si un usuario tiene una sesión ABIERTA
    @Query("SELECT s FROM SesionCaja s WHERE s.usuario = :usuario AND s.estado = 'ABIERTA'")
    Optional<SesionCaja> findSesionAbiertaPorUsuario(@Param("usuario") Usuario usuario);

    // Historial ordenado
    java.util.List<SesionCaja> findAllByOrderByFechaAperturaDesc();
}
