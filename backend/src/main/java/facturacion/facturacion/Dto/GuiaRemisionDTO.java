package facturacion.facturacion.Dto;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GuiaRemisionDTO {
    // Info Transportista
    @NotBlank(message = "Identificación del transportista es obligatoria")
    private String transportistaIdentificacion;
    @NotBlank(message = "Razón social del transportista es obligatoria")
    private String transportistaRazonSocial;
    @NotBlank(message = "Placa es obligatoria")
    private String placa;

    // Info Guía
    private String dirPartida;
    @jakarta.validation.constraints.NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaIniTransporte;
    @jakarta.validation.constraints.NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate fechaFinTransporte;

    // Destinatarios
    private List<DestinatarioDTO> destinatarios;

    @Data
    public static class DestinatarioDTO {
        private String identificacionDestinatario;
        private String razonSocialDestinatario;
        private String dirDestinatario;
        private String motivoTraslado;
        private String docAduaneroUnico;
        private String ruta;

        private String codDocSustento; // "01" factura
        private String numDocSustento; // 001-001-000000123
        private String numAutDocSustento; // Autorización de la factura (opcional si es físico, obligatorio electrónico)

        private List<DetalleGuiaDTO> detalles;
    }

    @Data
    public static class DetalleGuiaDTO {
        private String codigoInterno;
        private String descripcion;
        private Double cantidad;
    }
}
