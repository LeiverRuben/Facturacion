package facturacion.facturacion.Controladores;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import facturacion.facturacion.Entidades.Categoria;
import facturacion.facturacion.Servicios.CategoriaServicio;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

@RestController
@RequestMapping("/api/categoria")
public class CategoriaControlador {

    @Autowired
    private CategoriaServicio categoriaServicio;

    @PostMapping
    public Categoria guardar(@RequestBody Categoria categoria) {
        return categoriaServicio.guardar(categoria);
    }

    @GetMapping
    public List<Categoria> listar() {
        return categoriaServicio.listarAll();
    }

    @GetMapping("/{id}")
    public Categoria obtenerPorId(@PathVariable Long id) {
        return categoriaServicio.buscarId(id);
    }

    @PutMapping("/{id}")
    public Categoria actualizar(@PathVariable Long id, @RequestBody Categoria categoria) {
        return categoriaServicio.actualizar(id, categoria); 
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        categoriaServicio.eliminar(id);
    }
    
}
