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

    private String razonSocial;
    private String ruc;
    private String email;
    private String telefono;
    private String direccion;

    // Future expansion: bank details, credit days, etc.
}
