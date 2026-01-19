package facturacion.facturacion.Controladores;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import facturacion.facturacion.Dto.ConfiguracionSriDTO;
import facturacion.facturacion.Servicios.ConfiguracionSriServicio;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/configuracion-sri")
public class ConfiguracionSriControlador {
    
    private final ConfiguracionSriServicio configuracionSriServicio;

    @Autowired
    public ConfiguracionSriControlador(ConfiguracionSriServicio configuracionSriServicio) {
        this.configuracionSriServicio = configuracionSriServicio;
    }

    @GetMapping
    public ResponseEntity<List<ConfiguracionSriDTO>> getAllConfiguraciones() {
        List<ConfiguracionSriDTO> configs = configuracionSriServicio.findAll();
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConfiguracionSriDTO> getConfiguracionById(@PathVariable Long id) {
        return configuracionSriServicio.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ConfiguracionSriDTO> createConfiguracion(@Valid @RequestBody ConfiguracionSriDTO configuracionSriDTO) {
        ConfiguracionSriDTO savedConfig = configuracionSriServicio.save(configuracionSriDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedConfig);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConfiguracionSriDTO> updateConfiguracion(@PathVariable Long id, @Valid @RequestBody ConfiguracionSriDTO configuracionSriDTO) {

        if (!id.equals(configuracionSriDTO.getId_SRI())) {
            return ResponseEntity.badRequest().build();
        }

        if (configuracionSriServicio.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ConfiguracionSriDTO updatedConfig = configuracionSriServicio.save(configuracionSriDTO);
        return ResponseEntity.ok(updatedConfig);

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfiguracion(@PathVariable Long id) {
        configuracionSriServicio.delete(id);
        return ResponseEntity.noContent().build();
    }

}
