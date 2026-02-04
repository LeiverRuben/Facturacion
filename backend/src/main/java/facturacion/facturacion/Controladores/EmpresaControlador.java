package facturacion.facturacion.Controladores;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import facturacion.facturacion.Dto.EmpresaDTO;
import facturacion.facturacion.Entidades.Empresa;
import facturacion.facturacion.Servicios.EmpresaServicio;

@RestController
@RequestMapping("/api/empresa")
@CrossOrigin(origins = "*")
public class EmpresaControlador {

    @Autowired
    private EmpresaServicio empresaServicio;

    @Autowired
    private facturacion.facturacion.Servicios.FirmaElectronicaServicio firmaServicio;

    @GetMapping
    public ResponseEntity<Empresa> obtenerEmpresa() {
        try {
            // Asumimos Singleton ID 1
            Empresa empresa = empresaServicio.obtener(1L);
            return ResponseEntity.ok(empresa);
        } catch (Exception e) {
            // Si no existe, podría retornar 404
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping
    public ResponseEntity<Empresa> actualizarEmpresa(@RequestBody EmpresaDTO empresaDto) {
        try {
            // ID 1 fijo
            Empresa actualizada = empresaServicio.actualizar(1L, empresaDto);
            return ResponseEntity.ok(actualizada);
        } catch (Exception e) {
            // Si falla (ej. no existe), intentamos crear si es el primer uso
            try {
                Empresa creada = empresaServicio.crear(empresaDto);
                return ResponseEntity.ok(creada);
            } catch (Exception ex) {
                return ResponseEntity.badRequest().build();
            }
        }
    }

    @PostMapping("/test-firma")
    public ResponseEntity<String> testFirma(@RequestBody EmpresaDTO dto) {
        String resultado = firmaServicio.verificarFirma(dto.getRutaFirma(), dto.getClaveFirma());
        if (resultado.startsWith("ERROR")) {
            return ResponseEntity.badRequest().body(resultado);
        }
        return ResponseEntity.ok(resultado);
    }
}
