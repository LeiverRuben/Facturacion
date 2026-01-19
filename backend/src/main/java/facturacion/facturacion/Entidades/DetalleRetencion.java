package facturacion.facturacion.Entidades;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class DetalleRetencion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo; // 1=Renta, 2=IVA, 6=ISD
    private String codigoRetencion; // Código del SRI (ej: 312)
    private Double baseImponible;
    private Double porcentajeRetener;
    private Double valorRetenido;

    // Documento sustento
    private String codDocSustento;
    private String numDocSustento;
    private String fechaEmisionDocSustento;

    @ManyToOne
    @JoinColumn(name = "retencion_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private ComprobanteRetencion comprobanteRetencion;
}
