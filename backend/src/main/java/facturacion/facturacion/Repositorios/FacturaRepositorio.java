package facturacion.facturacion.Repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import facturacion.facturacion.Entidades.Factura;

import facturacion.facturacion.Entidades.SesionCaja;
import java.util.List;

@Repository
public interface FacturaRepositorio extends JpaRepository<Factura, Long> {
    List<Factura> findBySesionCaja(SesionCaja sesionCaja);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(f.totalFactura) FROM Factura f WHERE f.fechaEmision BETWEEN :start AND :end")
    Double sumTotalFacturaByFechaEmisionBetween(
            @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
            @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT f FROM Factura f ORDER BY f.facturaId DESC LIMIT 5")
    List<Factura> findTop5ByOrderByFacturaIdDesc();

    List<Factura> findByFechaEmisionBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}
