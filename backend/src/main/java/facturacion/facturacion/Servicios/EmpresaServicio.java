package facturacion.facturacion.Servicios;

import java.util.List;

import org.springframework.stereotype.Service;

import facturacion.facturacion.Dto.EmpresaDTO;
import facturacion.facturacion.Entidades.Empresa;
import facturacion.facturacion.Repositorios.EmpresaRepositorio;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmpresaServicio {

    private final EmpresaRepositorio empresaRepository;

    // LISTAR
    public List<Empresa> listar() {
        return empresaRepository.findAll();
    }

    // OBTENER POR ID
    public Empresa obtener(Long id) {
        return empresaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
    }

    // CREAR
    public Empresa crear(EmpresaDTO dto) {

        if (empresaRepository.existsByRuc(dto.getRuc())) {
            throw new RuntimeException("Ya existe una empresa con ese RUC.");
        }

        Empresa e = new Empresa();
        copiarDatos(dto, e);

        return empresaRepository.save(e);
    }

    // ACTUALIZAR
    public Empresa actualizar(Long id, EmpresaDTO dto) {
        Empresa existente = obtener(id);
        copiarDatos(dto, existente);
        return empresaRepository.save(existente);
    }

    // ELIMINAR
    public void eliminar(Long id) {
        Empresa existente = obtener(id);
        empresaRepository.delete(existente);
    }

    // MAPEO DTO → ENTIDAD
    private void copiarDatos(EmpresaDTO dto, Empresa e) {
        e.setRazonSocial(dto.getRazonSocial());
        e.setNombreComercial(dto.getNombreComercial());
        e.setRuc(dto.getRuc());

        e.setDirMatriz(dto.getDirMatriz());
        e.setDirEstablecimiento(dto.getDirEstablecimiento());

        e.setEstablecimiento(dto.getEstablecimiento());
        e.setPuntoEmision(dto.getPuntoEmision());

        e.setAmbiente(dto.getAmbiente());
        e.setTipoEmision(dto.getTipoEmision());
        e.setRutaFirma(dto.getRutaFirma());
        e.setClaveFirma(dto.getClaveFirma());
        e.setObligadoContabilidad(dto.getObligadoContabilidad());
    }
}