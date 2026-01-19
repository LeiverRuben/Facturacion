package facturacion.facturacion.Servicios;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import facturacion.facturacion.Entidades.TipoDocumentoCliente;
import facturacion.facturacion.Repositorios.TipoDocumentoClienteRepositorio;


@Service
public class TipoDocumentoClienteServicio {
@Autowired    
private TipoDocumentoClienteRepositorio tipoDocumentoClienteRepositorio;

public TipoDocumentoCliente guardar(TipoDocumentoCliente cliente) {
    return tipoDocumentoClienteRepositorio.save(cliente);
}
public List<TipoDocumentoCliente> listarAll() {
        return tipoDocumentoClienteRepositorio.findAll();
    }

    public TipoDocumentoCliente buscarId(String id) {
        return tipoDocumentoClienteRepositorio.findById(id).orElse(null);
    }

    public void eliminar(String id) {
        tipoDocumentoClienteRepositorio.deleteById(id);
    }
}
