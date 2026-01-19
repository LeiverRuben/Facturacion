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

    private String numeroComprobante; // Factura del proveedor
    private LocalDateTime fechaEmision;
    private LocalDateTime fechaRegistro;

    @ManyToOne
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    private Double subtotal;
    private Double totalIva;
    private Double total;

    private String estado; // PENDIENTE, RECIBIDA, ANULADA

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DetalleCompra> detalles;
}
