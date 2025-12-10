package facturacion.facturacion.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ConfiguracionSriDTO", description = "DTO para Configuración del SRI")
public class ConfiguracionSriDTO {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "ID generado por el servidor")
    @JsonProperty(access = Access.READ_ONLY)
    private Long id_SRI;

    @JsonProperty("RUC")
    private String RUC;

    private String emision;
    private String direccionMatriz;
    private String urlPruebas;

}
