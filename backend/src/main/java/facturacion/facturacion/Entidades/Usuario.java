package facturacion.facturacion.Entidades;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @jakarta.validation.constraints.NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @jakarta.validation.constraints.Email(message = "Correo inválido")
    @jakarta.validation.constraints.NotBlank(message = "El correo es obligatorio")
    private String correo;

    @jakarta.validation.constraints.NotBlank(message = "La contraseña es obligatoria")
    private String password;

    @jakarta.validation.constraints.NotBlank(message = "El username es obligatorio")
    private String username;
    private String estaActivo;

    @ManyToOne
    @JoinColumn(name = "tipousuario_id")
    private TipoUsuario tipoUsuario;

}
