package facturacion.facturacion.Controladores;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import facturacion.facturacion.Dto.FacturaRequestDTO;
import facturacion.facturacion.Entidades.Factura;
import facturacion.facturacion.Servicios.FacturaServicio;
import facturacion.facturacion.Servicios.FirmaElectronicaServicio;

import org.springframework.security.access.prepost.PreAuthorize;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
@CrossOrigin("*")
public class FacturaControlador {

    private final FacturaServicio facturaService;
    private final FirmaElectronicaServicio firmaService;

    @GetMapping
    public java.util.List<Factura> listar() {
        return facturaService.listarAll();
    }

    @PostMapping
    public ResponseEntity<?> crearFactura(@RequestBody FacturaRequestDTO request) {
        try {

            Factura factura = facturaService.crearFacturaCompleta(request);

            String claveAcceso = facturaService.generarClaveAcceso(factura);
            factura.setClaveAcceso(claveAcceso);
            facturaService.guardar(factura); // GUARDA LA CLAVE EN BD

            String xmlSinFirma = facturaService.generarXMLFactura(factura);

            String xmlFirmado = firmaService.firmarXML(
                    xmlSinFirma,
                    factura.getEmpresa().getRutaFirma(),
                    factura.getEmpresa().getClaveFirma());

            return ResponseEntity.ok(new RespuestaFactura(
                    "Factura creada correctamente",
                    factura.getFacturaId(),
                    claveAcceso,
                    xmlSinFirma,
                    xmlFirmado));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    "Error al crear factura: " + e.getMessage());
        }
    }

    private final facturacion.facturacion.Servicios.SriServicio sriServicio;
    @Autowired
    private facturacion.facturacion.Servicios.EmailServicio emailServicio;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CONTADOR', 'ROLE_VENDEDOR')")
    @PostMapping("/enviar-sri/{id}")
    public ResponseEntity<?> enviarSri(@PathVariable Long id) {
        try {
            Factura factura = facturaService.buscarPorId(id);
            if (factura == null) {
                return ResponseEntity.notFound().build();
            }

            // 1. Validar y Leer XML firmado
            String nombreArchivo = "factura_" + factura.getSecuencial() + "_firmado.xml";
            java.nio.file.Path pathXml = java.nio.file.Paths.get("C:\\facturaSRI", nombreArchivo);

            if (!java.nio.file.Files.exists(pathXml)) {
                return ResponseEntity.badRequest().body("No se encuentra el XML firmado: " + pathXml.toString());
            }

            byte[] xmlBytes = java.nio.file.Files.readAllBytes(pathXml);

            // 2. Enviar a Recepción
            String respuestaRecepcion = sriServicio.enviarComprobante(xmlBytes);
            String estadoRecepcion = sriServicio.extraerEstado(respuestaRecepcion);

            if ("RECIBIDA".equals(estadoRecepcion)) {
                // 3. Solicitar Autorización
                String respuestaAutorizacion = sriServicio.autorizarComprobante(factura.getClaveAcceso());
                String estadoAutorizacion = sriServicio.extraerEstado(respuestaAutorizacion);

                factura.setEstadoSri(estadoAutorizacion);
                factura.setMensajeSri(sriServicio.extraerMensaje(respuestaAutorizacion));

                if ("AUTORIZADO".equals(estadoAutorizacion)) {
                    factura.setEstado(3); // Autorizada
                    factura.setFechaAutorizacion(java.time.LocalDateTime.now());

                    // --- ENVIAR EMAIL AUTOMÁTICO ---
                    String emailCliente = factura.getCliente().getClienteEmail();
                    if (emailCliente != null && !emailCliente.isEmpty()
                            && !emailCliente.equalsIgnoreCase("consumidor@mail.com")) {
                        try {
                            byte[] pdfBytes = pdfGenServicio.generarPdfFactura(factura);
                            emailServicio.enviarFacturaAutorizada(emailCliente, factura.getSecuencial(), pdfBytes,
                                    xmlBytes);
                        } catch (Exception ex) {
                            System.err.println("No se pudo enviar el email: " + ex.getMessage());
                            // No detiene el flujo, solo loguea el error
                        }
                    }
                    // --------------------------------
                } else {
                    factura.setEstado(2); // Enviada pero no autorizada (Rechazada, etc)
                }

            } else {
                // Error en recepción
                factura.setEstadoSri(estadoRecepcion);
                factura.setMensajeSri(sriServicio.extraerMensaje(respuestaRecepcion));
                factura.setEstado(2); // Fallo en recepción
            }

            facturaService.guardar(factura);

            return ResponseEntity.ok(new RespuestaFactura(
                    "Proceso SRI Completado. Estado: " + factura.getEstadoSri() + ". Mensaje: "
                            + factura.getMensajeSri(),
                    factura.getFacturaId(),
                    factura.getClaveAcceso(),
                    null,
                    null));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error crítico en proceso SRI: " + e.getMessage());
        }
    }

    @Autowired
    private facturacion.facturacion.Servicios.PdfGenServicio pdfGenServicio;

    @GetMapping("/{id}/pdf")
    public ResponseEntity<?> descargarPdf(@PathVariable Long id) {
        try {
            Factura factura = facturaService.buscarPorId(id);
            if (factura == null) {
                return ResponseEntity.notFound().build();
            }

            byte[] pdfBytes = pdfGenServicio.generarPdfFactura(factura);

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=factura_" + factura.getSecuencial() + ".pdf")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/anular")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CONTADOR')")
    public ResponseEntity<?> anularFactura(@PathVariable Long id) {
        try {
            facturaService.anularFactura(id);
            return ResponseEntity.ok(java.util.Collections.singletonMap("mensaje", "Factura anulada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(java.util.Collections.singletonMap("error", "Error al anular factura: " + e.getMessage()));
        }
    }

    record RespuestaFactura(
            String mensaje,
            Long facturaId,
            String claveAcceso,
            String xmlSinFirma,
            String xmlFirmado) {
    }

}