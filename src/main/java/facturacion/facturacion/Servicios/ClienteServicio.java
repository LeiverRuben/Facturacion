package facturacion.facturacion.Servicios;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import facturacion.facturacion.Dto.ClienteConDocumentoDto;
import facturacion.facturacion.Dto.DocumentoListaDTO;
import facturacion.facturacion.Entidades.Cliente;
import facturacion.facturacion.Entidades.TipoDocumento;
import facturacion.facturacion.Entidades.TipoDocumentoCliente;
import facturacion.facturacion.Repositorios.ClienteRepositorio;
import facturacion.facturacion.Repositorios.TipoDocumentoClienteRepositorio;
import facturacion.facturacion.Repositorios.TipoDocumentoRepositorio;



@Service
public class ClienteServicio {
@Autowired
    private ClienteRepositorio clienteRepositorio;
    @Autowired  
    private TipoDocumentoRepositorio tipoDocumentoRepositorio;
    @Autowired
    private TipoDocumentoClienteRepositorio tipoDocumentoClienteRepositorio;

    public Cliente guardar(ClienteConDocumentoDto clienteDto) {
        List<DocumentoListaDTO> documentos = clienteDto.getDocumentos();
        if (documentos == null || documentos.isEmpty()) {
            throw new RuntimeException("Error no tiene documentos");
        }

        Cliente cliente = clienteDto.getCliente();
        clienteRepositorio.save(cliente);
        try {

        for (DocumentoListaDTO doc : documentos) {
            TipoDocumentoCliente tipoDocumentoCliente = new TipoDocumentoCliente();
            tipoDocumentoCliente.setCliente(cliente);
            System.err.println(doc.getNumeroDocumentoCliente());
            System.err.println(doc.getTipoDocumentoId());
            tipoDocumentoCliente.setNumeroDocumentoCliente(doc.getNumeroDocumentoCliente());
            //TipoDocumento tipoDocumento=tipoDocumentoRepositorio.getById(doc.getTipoDocumentoId());
                TipoDocumento tipoDocumento = tipoDocumentoRepositorio.findById(doc.getTipoDocumentoId())
                .orElseThrow(() -> new RuntimeException("Tipo de documento no encontrado con ID: " + doc.getTipoDocumentoId()));
            tipoDocumentoCliente.setTipoDocumento(tipoDocumento);
            tipoDocumentoCliente.setTipoDocumentoFecha(LocalDateTime.now());
            tipoDocumentoClienteRepositorio.save(tipoDocumentoCliente);
        }    
        }catch (Exception e) {
            clienteRepositorio.delete(cliente);
            throw new RuntimeException("Error no pudo crear los documentos del cliente");
        
        }
        return cliente;
    }
    public List<Cliente> listarAll(){
        return clienteRepositorio.findAll();
    }

    public Cliente buscarId(Long id) {
        return clienteRepositorio.findById(id).orElse(null);
    }
    @Transactional
    public Cliente actualizar(Long id, Cliente clienteActualizado) {
        // 1. Verificar existencia: Si no existe, lanza excepción (400/404)
        clienteRepositorio.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cliente con ID " + id + " no existe para actualizar."));

        // 2. Asignar el ID al objeto que se va a guardar
        clienteActualizado.setClienteId(id);

        // 3. Guardar: Si el objeto tiene un ID, se ejecuta un UPDATE
        return clienteRepositorio.save(clienteActualizado);
    }
 
public void eliminar(Long id){

    clienteRepositorio.deleteById(id);
    }
}