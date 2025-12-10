package facturacion.facturacion.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class ImpuestoDetalleDTO {
    
    private String codigo;
    private Double codigoPorcentaje;
    private Double tarifa;
    private Double baseImponible;
    private Double valor;

}