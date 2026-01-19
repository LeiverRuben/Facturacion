package facturacion.facturacion.Servicios;

import java.util.List;

import org.springframework.stereotype.Service;

import facturacion.facturacion.Dto.FormaPagoDTO;
import facturacion.facturacion.Entidades.FormaPago;
import facturacion.facturacion.Repositorios.FormaPagoRepositorio;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FormaPagoServicio {
    private final FormaPagoRepositorio formaPagoRepository;

    public List<FormaPago> listar() {
        return formaPagoRepository.findAll();
    }

    public FormaPago obtener(Long id) {
        return formaPagoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Forma de pago no encontrada"));
    }

    public FormaPago crear(FormaPagoDTO dto) {

        if (formaPagoRepository.existsByCodigoSri(dto.getCodigoSri())) {
            throw new RuntimeException("El código SRI ya está registrado.");
        }

        FormaPago fp = new FormaPago();
        copiarDatos(dto, fp);

        return formaPagoRepository.save(fp);
    }

    public FormaPago actualizar(Long id, FormaPagoDTO dto) {
        FormaPago existente = obtener(id);
        copiarDatos(dto, existente);
        return formaPagoRepository.save(existente);
    }

    public void eliminar(Long id) {
        FormaPago fp = obtener(id);
        formaPagoRepository.delete(fp);
    }

    private void copiarDatos(FormaPagoDTO dto, FormaPago fp) {
        fp.setNombre(dto.getNombre());
        fp.setCodigoSri(dto.getCodigoSri());
    }
}
