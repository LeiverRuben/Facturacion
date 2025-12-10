package facturacion.facturacion.Entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter

public class FacturaPago {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long facturaPagoId;

    @ManyToOne
    private Factura factura;

    @ManyToOne
    private FormaPago formaPago;

    private Double total;

    private Integer plazo;
    private String unidadTiempo;

}
