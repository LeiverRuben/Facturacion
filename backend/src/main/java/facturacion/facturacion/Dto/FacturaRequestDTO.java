package facturacion.facturacion.Dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class FacturaRequestDTO {
    
    private String secuencial;
    private LocalDateTime fechaEmision;

    private Double subtotal12;
    private Double subtotal0;
    private Double subtotalExento;
    private Double subtotalNoObjeto;
    private Double totalDescuento;
    private Double totalIva;
    private Double totalFactura;

    private Long clienteId;   // cliente existente / o se puede agregar un nested DTO
    private Long empresaId;   // datos del emisor

    // Lista de items
    private List<DetalleFacturaDTO> detalles;

    private List<PagoDTO> pagos;
    
}
