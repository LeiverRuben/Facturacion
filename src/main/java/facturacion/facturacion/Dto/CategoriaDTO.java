package facturacion.facturacion.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoriaDTO {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "ID generado por el servidor")
    @JsonProperty(access = Access.READ_ONLY)
    private Long categoriaId;
    
    private String categoriaNombre;
    private String categoriaDescripcion;

}
