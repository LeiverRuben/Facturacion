package facturacion.facturacion.Entidades;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class TipoDocumento {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long TipoDocumentoId;
    private String tipoDocumentoNombre;
    private String tipoDocumentoCodigo;
}
