package facturacion.facturacion.Controladores;

import org.springframework.web.bind.annotation.*;
import facturacion.facturacion.Entidades.GuiaDeRemision;
import facturacion.facturacion.Dto.GuiaRemisionDTO;
import facturacion.facturacion.Servicios.GuiaRemisionServicio;
import facturacion.facturacion.Repositorios.GuiaDeRemisionRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import java.util.List;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/guias-remision")
@RequiredArgsConstructor
@CrossOrigin("*")
public class GuiaDeRemisionControlador {

    private final GuiaDeRemisionRepositorio repositorio;
    private final GuiaRemisionServicio servicio;
    private final facturacion.facturacion.Servicios.PdfGenServicio pdfGenServicio;

    @GetMapping
    public List<GuiaDeRemision> listarTodos() {
        return repositorio.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GuiaDeRemision> obtenerPorId(@PathVariable Long id) {
        return repositorio.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody @Valid GuiaRemisionDTO guiaDTO) {
        try {
            return ResponseEntity.ok(servicio.crearGuia(guiaDTO));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al crear la guía: " + e.getMessage());
        }
    }

    @PostMapping("/enviar-sri/{id}")
    public ResponseEntity<GuiaDeRemision> enviarSri(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(servicio.enviarSri(id));
        } catch (Exception e) {
            // Retornamos un error controlado, o lanzamos excepción
            // Para simplificar, devolvemos 500 con el mensaje en el body si es posible, o
            // dependemos de GlobalHandler
            throw new RuntimeException("Error enviando al SRI: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        try {
            GuiaDeRemision guia = repositorio.findById(id)
                    .orElseThrow(() -> new RuntimeException("Guía no encontrada"));

            byte[] pdfBytes = pdfGenServicio.generarPdfGuiaRemision(guia);

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=guia_" + guia.getSecuencial() + ".pdf")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/xml")
    public ResponseEntity<byte[]> descargarXml(@PathVariable Long id) {
        try {
            GuiaDeRemision guia = repositorio.findById(id)
                    .orElseThrow(() -> new RuntimeException("Guía no encontrada"));

            String nombreArchivo = "guia_" + guia.getSecuencial() + "_firmado.xml";
            java.nio.file.Path pathXml = java.nio.file.Paths.get("C:\\facturaSRI", nombreArchivo);

            if (!java.nio.file.Files.exists(pathXml)) {
                nombreArchivo = "guia_" + guia.getSecuencial() + ".xml";
                pathXml = java.nio.file.Paths.get("C:\\facturaSRI", nombreArchivo);
                if (!java.nio.file.Files.exists(pathXml)) {
                    return ResponseEntity.notFound().build();
                }
            }

            byte[] xmlBytes = java.nio.file.Files.readAllBytes(pathXml);

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=" + nombreArchivo)
                    .contentType(org.springframework.http.MediaType.APPLICATION_XML)
                    .body(xmlBytes);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
