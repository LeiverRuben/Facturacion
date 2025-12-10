package facturacion.facturacion.Entidades;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class TipoDocumentoCliente {
    @ManyToOne
    @JoinColumn(name="clienteId",nullable=false)
    private Cliente cliente;
    @ManyToOne
    @JoinColumn(name="tipoDocumentoId",nullable=false)
    private TipoDocumento tipoDocumento;
    @Id
    private String numeroDocumentoCliente;
    private LocalDateTime tipoDocumentoFecha;

}
