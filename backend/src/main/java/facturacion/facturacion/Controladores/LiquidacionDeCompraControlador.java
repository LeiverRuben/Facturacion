package facturacion.facturacion.Controladores;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import facturacion.facturacion.Dto.LiquidacionCompraDTO;
import facturacion.facturacion.Dto.RespuestaSriDTO;
import facturacion.facturacion.Entidades.LiquidacionDeCompra;
import facturacion.facturacion.Repositorios.LiquidacionDeCompraRepositorio;
import facturacion.facturacion.Servicios.LiquidacionCompraServicio;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/liquidaciones-compra")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LiquidacionDeCompraControlador {

    private final LiquidacionCompraServicio lcServicio;
    private final LiquidacionDeCompraRepositorio lcRepositorio;
    private final facturacion.facturacion.Servicios.PdfGenServicio pdfGenServicio;

    @GetMapping
    public List<LiquidacionDeCompra> listar() {
        return lcRepositorio.findAll();
    }

    @PostMapping
    public ResponseEntity<LiquidacionDeCompra> crear(@RequestBody @jakarta.validation.Valid LiquidacionCompraDTO dto) {
        return ResponseEntity.ok(lcServicio.crear(dto));
    }

    @PostMapping("/{id}/enviar-sri")
    public ResponseEntity<RespuestaSriDTO> enviarSri(@PathVariable Long id) {
        return ResponseEntity.ok(lcServicio.enviarSRI(id));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        try {
            LiquidacionDeCompra liquidacion = lcRepositorio.findById(id)
                    .orElseThrow(() -> new RuntimeException("Liquidación no encontrada"));

            byte[] pdfBytes = pdfGenServicio.generarPdfLiquidacionCompra(liquidacion);

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=liquidacion_" + liquidacion.getSecuencial() + ".pdf")
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
            LiquidacionDeCompra liquidacion = lcRepositorio.findById(id)
                    .orElseThrow(() -> new RuntimeException("Liquidación no encontrada"));

            String nombreArchivo = "liquidacion_" + liquidacion.getSecuencial() + "_firmado.xml";
            java.nio.file.Path pathXml = java.nio.file.Paths.get("C:\\facturaSRI", nombreArchivo);

            if (!java.nio.file.Files.exists(pathXml)) {
                nombreArchivo = "liquidacion_" + liquidacion.getSecuencial() + ".xml";
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
