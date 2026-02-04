package facturacion.facturacion.Entidades;

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

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class DestinatarioGuia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String identificacionDestinatario;
    private String razonSocialDestinatario;
    private String dirDestinatario;
    private String motivoTraslado;
    private String docAduaneroUnico;
    private String ruta;

    // Documento sustento (opcional, ej: Factura asociada)
    private String codDocSustento;
    private String numDocSustento;
    private String numAutDocSustento;

    @ManyToOne
    @JoinColumn(name = "guia_remision_id", nullable = false)
    @JsonBackReference
    private GuiaDeRemision guiaDeRemision;

    @OneToMany(mappedBy = "destinatario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<DetalleGuia> detalles;
}
