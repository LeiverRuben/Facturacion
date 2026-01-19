package facturacion.facturacion.Repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import facturacion.facturacion.Entidades.FormaPago;

public interface FormaPagoRepositorio extends JpaRepository<FormaPago, Long> {
    boolean existsByCodigoSri(String codigoSri);
}