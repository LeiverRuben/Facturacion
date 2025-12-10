package facturacion.facturacion.Repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import facturacion.facturacion.Entidades.Empresa;

public interface EmpresaRepositorio extends JpaRepository<Empresa, Long> {
    boolean existsByRuc(String ruc);
}