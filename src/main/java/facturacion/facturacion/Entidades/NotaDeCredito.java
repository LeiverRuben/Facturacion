package facturacion.facturacion.Entidades;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class NotaDeCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String secuencial;
    private String claveAcceso;

    private LocalDateTime fechaEmision;
    private Double subtotal12;
    private Double subtotal0;
    private Double subtotalNoObjeto;
    private Double subtotalExento;
    private Double totalDescuento;
    private Double totalIva;
    private Double total;

    // Campos específicos de NC
    private String motivo;

    // Referencia al documento modificado
    @ManyToOne
    @JoinColumn(name = "factura_modificada_id")
    private Factura facturaModificada;

    // Tracking SRI
    private Integer estado;
    private String estadoSri;
    private LocalDateTime fechaAutorizacion;
    private String mensajeSri;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @OneToMany(mappedBy = "notaDeCredito", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DetalleNotaCredito> detalles;
}
