package facturacion.facturacion.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DetalleFacturaDTO {

    private Long productoId;
    private Double cantidad;
    private Double precioUnitario;
    private Double descuento;
    private Double subtotal;

    private ImpuestoDetalleDTO impuesto;

}