package facturacion.facturacion.Entidades;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CatalogoImpuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Código del impuesto (Ej: 2 para IVA, 3 para ICE)
    private String codigo;

    // Código del porcentaje (Ej: 0, 2, 3, etc. según tabla 17/18)
    private String codigoPorcentaje;

    // Descripción legible (Ej: "IVA 12%", "IVA 0%")
    private String descripcion;

    // El valor numérico del porcentaje (Ej: 12.00, 0.00, 15.00)
    private Double porcentaje;
}
