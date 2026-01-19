package facturacion.facturacion.Controladores;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import facturacion.facturacion.Dto.DetalleRetencionDTO;
import facturacion.facturacion.Entidades.ComprobanteRetencion;
import facturacion.facturacion.Servicios.RetencionServicio;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/retenciones")
@CrossOrigin("*")
public class RetencionControlador {

    @Autowired
    private RetencionServicio retencionServicio;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/generar")
    public ResponseEntity<?> generarRetencion(@RequestBody Map<String, Object> request) {
        try {
            Long compraId = Long.valueOf(request.get("compraId").toString());

            // Deserializar lista de detalles de manera segura
            List<Map<String, Object>> detallesMap = (List<Map<String, Object>>) request.get("detalles");
            List<DetalleRetencionDTO> detalles = detallesMap.stream()
                    .map(map -> objectMapper.convertValue(map, DetalleRetencionDTO.class))
                    .toList();

            ComprobanteRetencion retencion = retencionServicio.generarRetencion(compraId, detalles);
            return ResponseEntity.ok(retencion);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error al generar retención: " + e.getMessage());
        }
    }

    @Autowired
    private facturacion.facturacion.Servicios.PdfGenServicio pdfGenServicio;

    @GetMapping
    public List<ComprobanteRetencion> listar() {
        return retencionServicio.listarRetenciones();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        try {
            // Buscamos la retención por ID (necesitamos añadir método findById en Servicio
            // si no existe, o filtrar)
            // Por simplicidad, asumiremos que listar() trae todo y filtramos (ineficiente
            // pero rápido para demo)
            // O mejor, añadimos un método rápido en el repo.
            ComprobanteRetencion retencion = retencionServicio.listarRetenciones().stream()
                    .filter(r -> r.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Retención no encontrada"));

            byte[] pdfBytes = pdfGenServicio.generarPdfRetencion(retencion);

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=retencion_" + retencion.getSecuencial() + ".pdf")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
