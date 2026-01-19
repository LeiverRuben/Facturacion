package facturacion.facturacion.Entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Kardex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fecha;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private String tipoMovimiento; // ENTRADA, SALIDA, AJUSTE
    private String detalle; // "Compra #123", "Venta #456"

    // Movimiento
    private Integer cantidad;
    private Double costoUnitario;
    private Double totalMovimiento;

    // Saldos (Promedio Ponderado)
    private Integer saldoCantidad;
    private Double costoPromedio;
    private Double saldoTotal;
}
