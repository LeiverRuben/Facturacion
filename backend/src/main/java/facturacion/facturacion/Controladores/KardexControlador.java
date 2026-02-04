package facturacion.facturacion.Controladores;

import facturacion.facturacion.Entidades.Kardex;
import facturacion.facturacion.Servicios.KardexServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kardex")
@CrossOrigin(origins = "*")
public class KardexControlador {

    @Autowired
    private KardexServicio kardexServicio;

    @GetMapping
    public List<Kardex> listarTodos() {
        return kardexServicio.listarTodos();
    }

    @GetMapping("/producto/{id}")
    public List<Kardex> obtenerPorProducto(@PathVariable Long id) {
        return kardexServicio.obtenerKardexPorProducto(id);
    }

    @PostMapping("/sincronizar")
    public void sincronizar() {
        kardexServicio.sincronizarInventario();
    }
}
