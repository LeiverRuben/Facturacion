package facturacion.facturacion.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RespuestaSriDTO {
    private String estado;
    private String mensaje;
    private String fechaAutorizacion;
}
