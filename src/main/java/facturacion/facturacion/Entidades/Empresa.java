package facturacion.facturacion.Entidades;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter

public class Empresa {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long empresaId;

    private String razonSocial;
    private String nombreComercial;
    private String ruc;

    private String dirMatriz;
    private String dirEstablecimiento;

    private String establecimiento; 
    private String puntoEmision;

    private Integer ambiente;      // 1 pruebas, 2 producción
    private Integer tipoEmision;   // 1 normal

    private String obligadoContabilidad;  // SI / NO

    private String rutaFirma;   // Ej: C:/certificados/firma.p12
    private String claveFirma;  // password archivo p12

    private String contribuyenteEspecial;  // opcional
    private String resolucion;
    
}
