package facturacion.facturacion.Entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @jakarta.validation.constraints.NotBlank(message = "El numero de comprobante es obligatorio")
    private String numeroComprobante; // Factura del proveedor
    private LocalDateTime fechaEmision;
    private LocalDateTime fechaRegistro;

    @ManyToOne
    @JoinColumn(name = "proveedor_id")
    @jakarta.validation.constraints.NotNull(message = "El proveedor es obligatorio")
    private Proveedor proveedor;

    private Double subtotal;
    private Double totalIva;
    private Double total;

    private String estado; // PENDIENTE, RECIBIDA, ANULADA

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DetalleCompra> detalles;
}
