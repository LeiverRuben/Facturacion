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
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @jakarta.validation.constraints.NotBlank(message = "La razón social es obligatoria")
    private String razonSocial;

    @jakarta.validation.constraints.NotBlank(message = "El RUC es obligatorio")
    private String ruc;

    @jakarta.validation.constraints.Email(message = "El email no es válido")
    @jakarta.validation.constraints.NotBlank(message = "El email es obligatorio")
    private String email;

    private String telefono;
    private String direccion;

    // Future expansion: bank details, credit days, etc.
}
