package facturacion.facturacion.Dto;

import lombok.Data;

@Data
public class DetalleRetencionDTO {
    private String codigo; // 1=Renta, 2=IVA, 6=ISD
    private String codigoRetencion; // Código del SRI (ej: 312)
    private Double baseImponible;
    private Double porcentajeRetener;
}
