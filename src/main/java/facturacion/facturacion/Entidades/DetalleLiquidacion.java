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
public class DetalleLiquidacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigoPrincipal;
    private String descripcion;
    private Double cantidad;
    private Double precioUnitario;
    private Double descuento;
    private Double precioTotalSinImpuesto;

    @ManyToOne
    @JoinColumn(name = "liquidacion_id", nullable = false)
    private LiquidacionDeCompra liquidacion;
}
