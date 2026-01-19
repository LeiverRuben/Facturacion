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
public class ConfiguracionSRI {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_SRI;

    private String RUC;
    private String emision;
    private String direccionMatriz;
    private String urlPruebas;
    private String firmaElectronica;
    private String claveAccesoFirma;

}
