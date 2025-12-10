package facturacion.facturacion.Controladores;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import facturacion.facturacion.Dto.ClienteConDocumentoDto;
import facturacion.facturacion.Entidades.Cliente;
import facturacion.facturacion.Servicios.ClienteServicio;


@RestController
@RequestMapping("/api/cliente")
public class ClienteControlador {
    
    @Autowired
    private ClienteServicio clienteServicio;

    @PostMapping
    public Cliente guardar(@RequestBody ClienteConDocumentoDto cliente) {
        return clienteServicio.guardar(cliente);
    }
    
    @GetMapping
    public List<Cliente> listarAll() {
        return clienteServicio.listarAll();
    }
    @GetMapping("/{id}")
    public Cliente buscarId(@PathVariable long Id) {
        return clienteServicio.buscarId(Id);
    }
    @PutMapping("/{id}")
    public Cliente actualizar(@PathVariable Long id, @RequestBody Cliente clienteActualizado) {
    // ESTE MÉTODO FALTABA O NO FUE DETECTADO POR SPRING
    return clienteServicio.actualizar(id, clienteActualizado);
    }
    
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable long id) {
        clienteServicio.eliminar(id);
    }
    
}
