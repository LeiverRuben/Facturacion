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
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long facturaId;

    private String secuencial; // 9 dígitos
    private String claveAcceso; // generado por SRI

    private LocalDateTime fechaEmision;
    private Double subtotal12;
    private Double subtotal0;
    private Double subtotalNoObjeto;
    private Double subtotalExento;
    private Double totalDescuento;
    private Double totalIva;
    private Double totalFactura;

    private Integer estado; // 1=Registrada,2=Enviada,3=Autorizada
    private LocalDateTime fechaAutorizacion;
    private String mensajeSri;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DetalleFactura> detalles;

}
