package facturacion.facturacion.Entidades;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.validation.constraints.NotBlank;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class GuiaDeRemision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String secuencial;
    private String claveAcceso;
    private LocalDateTime fechaEmision;

    // Info Guía Remisión
    private String dirPartida;
    @NotBlank(message = "Identificación del transportista es obligatoria")
    private String transportistaIdentificacion;
    @NotBlank(message = "Razón social del transportista es obligatoria")
    private String transportistaRazonSocial;
    @NotBlank(message = "Placa es obligatoria")
    private String placa;
    private LocalDate fechaIniTransporte;
    private LocalDate fechaFinTransporte;

    // Tracking SRI
    private Integer estado;
    private String estadoSri;
    private LocalDateTime fechaAutorizacion;
    private String mensajeSri;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    // Una guía puede tener múltiples destinatarios (puntos de llegada)
    @OneToMany(mappedBy = "guiaDeRemision", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<DestinatarioGuia> destinatarios;
}
