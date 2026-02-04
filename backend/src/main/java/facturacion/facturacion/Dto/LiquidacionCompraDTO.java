package facturacion.facturacion.Dto;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LiquidacionCompraDTO {
    @NotNull(message = "El proveedor es obligatorio")
    private Long proveedorId; // Cliente entity acting as provider

    // Lista de items
    @jakarta.validation.constraints.NotEmpty(message = "Debe haber al menos un detalle")
    private List<DetalleLCDTO> detalles;

    @Data
    public static class DetalleLCDTO {
        private Long productoId;
        private Double cantidad;
        private Double precioUnitario;
        // El descuento no es tan común en LC pero lo mantenemos por consistencia
        private Double descuento;
    }
}
