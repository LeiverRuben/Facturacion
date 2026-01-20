package facturacion.facturacion.Dto;

import java.util.List;
import facturacion.facturacion.Entidades.Factura;
import lombok.Data;

@Data
public class DashboardStatsDTO {
    private long totalClientes;
    private long totalProductos;
    private long totalFacturas;
    private double ventasHoy;
    private double ventasSemana;
    private List<Factura> ultimasFacturas;
    private List<Object[]> productosBajoStock; // Simplificado
    private List<VentaDiariaDTO> ventasUltimaSemana;

    @Data
    public static class VentaDiariaDTO {
        private String dia;
        private double total;
    }
}
