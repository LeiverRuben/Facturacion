package facturacion.facturacion.Entidades;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter

public class ImpuestoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long impuestoId;

    @OneToOne
    @JoinColumn(name = "detalle_id")
    private DetalleFactura detalleFactura;

    private String codigo;
    private Double codigoPorcentaje;
    private Double tarifa;
    private Double baseImponible;
    private Double valor;
    
}
