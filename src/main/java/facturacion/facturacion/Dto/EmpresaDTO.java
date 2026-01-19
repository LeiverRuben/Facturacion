package facturacion.facturacion.Dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class EmpresaDTO {
    
    private String razonSocial;
    private String nombreComercial;
    private String ruc;

    private String dirMatriz;
    private String dirEstablecimiento;

    private String establecimiento;
    private String puntoEmision;

    private Integer ambiente;
    private Integer tipoEmision;

    private String obligadoContabilidad;

    private String rutaFirma;
    private String claveFirma;

    private String contribuyenteEspecial;
    private String resolucion;

}
