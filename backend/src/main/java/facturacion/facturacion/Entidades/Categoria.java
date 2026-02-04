package facturacion.facturacion.Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "Categoria")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "categoriaId")
    private Long categoriaId;

    @Column(name = "categoria_nombre")
    @jakarta.validation.constraints.NotBlank(message = "El nombre de la categoría es obligatorio")
    private String categoriaNombre;

    @Column(name = "categoria_descripcion")
    private String categoriaDescripcion;

}
