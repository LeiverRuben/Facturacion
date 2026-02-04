package facturacion.facturacion.Dto;

import lombok.Data;
import java.util.List;

@Data
public class NotaCreditoDTO {
    private Long facturaId;
    private String motivo;

    // Lista de items a devolver o ajustar
    private List<DetalleNCDTO> detalles;

    @Data
    public static class DetalleNCDTO {
        private Long productoId;
        private Double cantidad; // Cantidad a devolver
        private Double precioUnitario; // Precio al que se vendió
        private Double descuento;
        private String descripcion;
    }
}
