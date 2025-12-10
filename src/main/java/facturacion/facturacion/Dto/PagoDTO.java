package facturacion.facturacion.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class PagoDTO {

    private Long metodoPagoId;
    private Double total;
    private Integer plazo;
    private String unidadTiempo;
    
}
