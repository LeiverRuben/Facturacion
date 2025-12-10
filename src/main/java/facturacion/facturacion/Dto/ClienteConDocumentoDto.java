package facturacion.facturacion.Dto;

import java.util.List;

import facturacion.facturacion.Entidades.Cliente;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClienteConDocumentoDto {
    private Cliente Cliente;
    
    private List<DocumentoListaDTO> documentos;
}
