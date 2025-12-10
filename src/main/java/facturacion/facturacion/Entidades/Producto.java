package facturacion.facturacion.Entidades;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "producto")
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "producto_id")
    private long productoId;

    @Column(name = "producto_serial") 
    private String productoSerial;
    
    @Column(name = "producto_nombre")
    private String productoNombre;
    
    @Column(name = "producto_precio")
    private Double productoPrecio;
    
    @Column(name = "producto_stock")
    private Integer productoStock;
    
    @Column(name = "producto_tasa")
    private Double productoTasa;
    
    @Column(name = "producto_estado")
    private Integer productoEstado;
}
