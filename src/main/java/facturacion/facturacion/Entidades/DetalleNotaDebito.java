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
public class DetalleNotaDebito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String razonModificacion; // Motivo del débito (Interés, etc)
    private Double valorModificacion; // Valor monetario

    @ManyToOne
    @JoinColumn(name = "nota_debito_id", nullable = false)
    private NotaDeDebito notaDeDebito;
}
