package facturacion.facturacion.Servicios;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import facturacion.facturacion.Dto.ConfiguracionSriDTO;
import facturacion.facturacion.Entidades.ConfiguracionSRI;
import facturacion.facturacion.Repositorios.ConfiguracionSriRepositorio;
import jakarta.transaction.Transactional;

@Service
public class ConfiguracionSriServicio {
    
    private final ConfiguracionSriRepositorio configuracionSriRepositorio;

    public ConfiguracionSriServicio(ConfiguracionSriRepositorio configuracionSriRepositorio) {
        this.configuracionSriRepositorio = configuracionSriRepositorio;
    }

    @Transactional
    public List<ConfiguracionSriDTO> findAll() {
        return configuracionSriRepositorio.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<ConfiguracionSriDTO> findById(Long id) {
        return configuracionSriRepositorio.findById(id)
                .map(this::convertToDto);
    }

    @Transactional
    public ConfiguracionSriDTO save(ConfiguracionSriDTO configuracionSriDTO) {
        ConfiguracionSRI entity = convertToEntity(configuracionSriDTO);
        ConfiguracionSRI savedEntity = configuracionSriRepositorio.save(entity);
        return convertToDto(savedEntity);
    }

    @Transactional
    public void delete(Long id) {
        configuracionSriRepositorio.deleteById(id);
    }

    private ConfiguracionSriDTO convertToDto(ConfiguracionSRI entity) {
        if (entity == null) {
            return null;
        }
        ConfiguracionSriDTO dto = new ConfiguracionSriDTO();
        dto.setId_SRI(entity.getId_SRI());
        dto.setRUC(entity.getRUC());
        dto.setEmision(entity.getEmision());
        dto.setDireccionMatriz(entity.getDireccionMatriz());
        dto.setUrlPruebas(entity.getUrlPruebas());
        return dto;
    }

    private ConfiguracionSRI convertToEntity(ConfiguracionSriDTO dto) {
        if (dto == null) {
            return null;
        }
        ConfiguracionSRI entity = new ConfiguracionSRI();
        // Only set the ID when the DTO provides a valid (non-null, >0) id.
        // For creation requests the id should be ignored so the database can auto-generate it.
        if (dto.getId_SRI() != null && dto.getId_SRI() > 0) {
            entity.setId_SRI(dto.getId_SRI());
        }
        entity.setRUC(dto.getRUC());
        entity.setEmision(dto.getEmision());
        entity.setDireccionMatriz(dto.getDireccionMatriz());
        entity.setUrlPruebas(dto.getUrlPruebas());
        return entity;
    }
    
}
