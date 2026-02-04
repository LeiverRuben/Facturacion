package facturacion.facturacion.Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cliente_id")
    private Long clienteId;

    @NotBlank(message = "el nombre no puede estar vacio")
    @Size(min = 3, max = 200, message = "el nombre debe tener entre 3 a 200 caracteres")
    @Column(length = 200, nullable = false)
    private String clienteNombre;
    private String clienteDireccion;
    @NotBlank(message = "El apellido no puede estar vacío")
    private String clienteApellido;

    private String clienteTelefono;

    @Email(message = "El correo no es válido")
    @NotBlank(message = "El email es obligatorio")
    private String clienteEmail;

    private Boolean clienteEstado;

    @Column(length = 13)
    @NotBlank(message = "La identificación es obligatoria")
    private String identificacion;
}
