package facturacion.facturacion.Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
  @jakarta.validation.constraints.NotBlank(message = "El nombre del producto es obligatorio")
  private String productoNombre;

  @Column(name = "producto_descripcion")
  private String productoDescripcion;

  @Column(name = "producto_precio")
  @jakarta.validation.constraints.NotNull(message = "El precio es obligatorio")
  @jakarta.validation.constraints.Min(value = 0, message = "El precio no puede ser negativo")
  private Double productoPrecio;

  @Column(name = "producto_stock")
  @jakarta.validation.constraints.Min(value = 0, message = "El stock no puede ser negativo")
  private Integer productoStock = 0;

  @Column(name = "producto_tasa")
  private Double productoTasa;

  @Column(name = "producto_estado")
  private Boolean productoEstado;

  @ManyToOne
  @JoinColumn(name = "categoria_id")
  private Categoria categoria;
}
