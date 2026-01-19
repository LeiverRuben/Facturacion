package facturacion.facturacion.Servicios;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import facturacion.facturacion.Entidades.TipoDocumento;
import facturacion.facturacion.Repositorios.TipoDocumentoRepositorio;



@Service
public class TipoDocumentoServicio {
@Autowired    
private TipoDocumentoRepositorio tipoDocumentoRepositorio;

public TipoDocumento guardar(TipoDocumento tipoDocumento) {
    return tipoDocumentoRepositorio.save(tipoDocumento);
}
public List<TipoDocumento> listarAll() {
        return tipoDocumentoRepositorio.findAll();
    }

    public TipoDocumento buscarId(Long id) {
        return tipoDocumentoRepositorio.findById(id).orElse(null);
    }

    public void eliminar(Long id) {
        tipoDocumentoRepositorio.deleteById(id);
    }
}
