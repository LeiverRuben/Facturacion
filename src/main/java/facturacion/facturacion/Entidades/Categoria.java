package facturacion.facturacion.Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    private long categoriaId;

    @Column(name = "categoria_nombre")
    private String categoriaNombre;

    @Column(name = "categoria_descripcion")
    private String categoriaDescripcion;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;
    
}
