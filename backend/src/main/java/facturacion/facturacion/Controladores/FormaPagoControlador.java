package facturacion.facturacion.Controladores;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import facturacion.facturacion.Entidades.FormaPago;
import facturacion.facturacion.Servicios.FormaPagoServicio;

@RestController
@RequestMapping("/api/formapago")
@CrossOrigin(origins = "*")
public class FormaPagoControlador {

    @Autowired
    private FormaPagoServicio formaPagoServicio;

    @GetMapping
    public List<FormaPago> listar() {
        return formaPagoServicio.listar();
    }
}
