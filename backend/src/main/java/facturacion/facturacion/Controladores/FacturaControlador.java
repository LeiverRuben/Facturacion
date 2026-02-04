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
    private final facturacion.facturacion.Repositorios.EmpresaRepositorio empresaRepositorio;

    @GetMapping
    public java.util.List<Factura> listar() {
        return facturaService.listarAll();
    }

    @PostMapping
    public ResponseEntity<?> crearFactura(@RequestBody @jakarta.validation.Valid FacturaRequestDTO request) {
        try {
            Factura factura = facturaService.crearFacturaCompleta(request);

            String claveAcceso = facturaService.generarClaveAcceso(factura);
            factura.setClaveAcceso(claveAcceso);
            facturaService.guardar(factura);

            String xmlSinFirma = facturaService.generarXMLFactura(factura);

            // Obtener configuración de empresa dinámica
            facturacion.facturacion.Entidades.Empresa empresa = empresaRepositorio.findById(1L).orElse(null);
            String rutaFirma = (empresa != null && empresa.getRutaFirma() != null) ? empresa.getRutaFirma() : "";
            String claveFirma = (empresa != null && empresa.getClaveFirma() != null) ? empresa.getClaveFirma() : "";

            if (rutaFirma.isEmpty() || claveFirma.isEmpty()) {
                throw new RuntimeException("No hay firma electrónica configurada en 'Empresa'.");
            }

            String xmlFirmado = firmaService.firmarXML(
                    xmlSinFirma,
                    rutaFirma,
                    claveFirma);

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

            // --- CORRECCIÓN: REGENERAR XML SIEMPRE ---
            try {
                facturaService.generarXMLFactura(factura);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Error al regenerar XML: " + e.getMessage());
            }

            // Obtener configuración de empresa dinámica
            facturacion.facturacion.Entidades.Empresa empresa = empresaRepositorio.findById(1L).orElse(null);
            String rutaFirma = (empresa != null && empresa.getRutaFirma() != null) ? empresa.getRutaFirma() : "";
            String claveFirma = (empresa != null && empresa.getClaveFirma() != null) ? empresa.getClaveFirma() : "";

            if (rutaFirma.isEmpty() || claveFirma.isEmpty()) {
                throw new RuntimeException("No hay firma electrónica configurada en 'Empresa'.");
            }

            // 1. Validar y Leer XML firmado
            String nombreArchivo = "factura_" + factura.getSecuencial() + "_firmado.xml";
            java.nio.file.Path pathXml = java.nio.file.Paths.get("C:\\facturaSRI", nombreArchivo);

            // CORRECCIÓN FINAL: Eliminar el firmado anterior para forzar re-firma con los
            // datos nuevos
            try {
                java.nio.file.Files.deleteIfExists(pathXml);
            } catch (Exception e) {
                System.out.println("No se pudo eliminar el XML firmado antiguo: " + e.getMessage());
            }

            if (!java.nio.file.Files.exists(pathXml)) {
                // INTENTO DE RECUPERACIÓN: Firmar si existe el XML sin firma
                String nombreSinFirma = "factura_" + factura.getSecuencial() + ".xml";
                java.nio.file.Path pathSinFirma = java.nio.file.Paths.get("C:\\facturaSRI", nombreSinFirma);

                if (java.nio.file.Files.exists(pathSinFirma)) {
                    try {
                        firmaService.firmarXML(pathSinFirma.toString(), rutaFirma, claveFirma);
                    } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                .body("Error al intentar firmar XML existente: " + e.getMessage());
                    }
                } else {
                    return ResponseEntity.badRequest()
                            .body("No se encuentra el XML firmado ni el XML original para firmar: "
                                    + pathXml.toString());
                }
            }

            byte[] xmlBytes = java.nio.file.Files.readAllBytes(pathXml);

            // 2. Enviar a Recepción
            String respuestaRecepcion = sriServicio.enviarComprobante(xmlBytes);
            String estadoRecepcion = sriServicio.extraerEstado(respuestaRecepcion);
            String msgRecepcion = sriServicio.extraerMensaje(respuestaRecepcion);

            boolean procederAutorizacion = "RECIBIDA".equals(estadoRecepcion)
                    || (msgRecepcion != null && msgRecepcion.contains("CLAVE ACCESO REGISTRADA"));

            if (procederAutorizacion) {
                // 3. Solicitar Autorización
                String respuestaAutorizacion = sriServicio.autorizarComprobante(factura.getClaveAcceso());
                String estadoAutorizacion = sriServicio.extraerEstado(respuestaAutorizacion);

                // --- MANEJO DE "EN PROCESAMIENTO" (Retry Loop Robusto) ---
                int intentos = 0;
                while ("EN_PROCESO".equals(estadoAutorizacion) && intentos < 10) {
                    try {
                        System.out.println("SRI en proceso... Esperando 2s (Intento " + (intentos + 1) + ")");
                        Thread.sleep(2000); // Esperar 2 seg
                        respuestaAutorizacion = sriServicio.autorizarComprobante(factura.getClaveAcceso());
                        estadoAutorizacion = sriServicio.extraerEstado(respuestaAutorizacion);
                        intentos++;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                // -------------------------------------

                factura.setEstadoSri(estadoAutorizacion);
                String msgError = sriServicio.extraerMensaje(respuestaAutorizacion);
                System.out.println("SRI RESPUESTA COMPLETA (Autorizacion): " + msgError);
                factura.setMensajeSri(
                        msgError == null ? "" : (msgError.length() > 250 ? msgError.substring(0, 250) : msgError));

                if ("AUTORIZADO".equals(estadoAutorizacion)) {
                    factura.setEstado(3); // Autorizada
                    factura.setFechaAutorizacion(java.time.LocalDateTime.now());
                    /*
                     * String emailCliente = factura.getCliente().getClienteEmail();
                     * if (emailCliente != null && !emailCliente.isEmpty()) {
                     * try {
                     * byte[] pdfBytes = pdfGenServicio.generarPdfFactura(factura);
                     * emailServicio.enviarNotificacionFactura(emailCliente,
                     * factura.getSecuencial(), pdfBytes, xmlBytes, estadoAutorizacion, "");
                     * } catch (Exception ex) {}
                     * }
                     */
                    // --------------------------------
                } else {
                    factura.setEstado(2); // Enviada pero no autorizada (Rechazada, etc)
                }

            } else {
                // Error en recepción
                factura.setEstadoSri(estadoRecepcion);
                System.out.println("SRI RESPUESTA COMPLETA (Recepcion): " + msgRecepcion);
                factura.setMensajeSri(msgRecepcion == null ? ""
                        : (msgRecepcion.length() > 250 ? msgRecepcion.substring(0, 250) : msgRecepcion));
                factura.setEstado(2); // Fallo en recepción
            }

            facturaService.guardar(factura);

            // Convertir XMLs a String para devolver al frontend si es necesario
            String xmlSinFirmaStr = java.nio.file.Files
                    .exists(java.nio.file.Paths.get("C:\\facturaSRI", "factura_" + factura.getSecuencial() + ".xml"))
                            ? new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("C:\\facturaSRI",
                                    "factura_" + factura.getSecuencial() + ".xml")))
                            : null;

            String xmlFirmadoStr = java.nio.file.Files.exists(pathXml)
                    ? new String(xmlBytes)
                    : null;

            return ResponseEntity.ok(new RespuestaFactura(
                    "Proceso SRI Completado. Estado: " + factura.getEstadoSri() + ". Mensaje: "
                            + factura.getMensajeSri(),
                    factura.getFacturaId(),
                    factura.getClaveAcceso(),
                    xmlSinFirmaStr,
                    xmlFirmadoStr));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error crítico en proceso SRI: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/email")
    public ResponseEntity<?> enviarEmailManual(@PathVariable Long id) {
        try {
            Factura factura = facturaService.buscarPorId(id);
            if (factura == null)
                return ResponseEntity.notFound().build();

            String emailCliente = factura.getCliente().getClienteEmail();
            if (emailCliente == null || emailCliente.isEmpty()) {
                return ResponseEntity.badRequest().body("El cliente no tiene email registrado.");
            }

            byte[] pdfBytes = pdfGenServicio.generarPdfFactura(factura);

            // Re-leer XML firmado si existe
            byte[] xmlBytes = null;
            String nombreArchivo = "factura_" + factura.getSecuencial() + "_firmado.xml";
            java.nio.file.Path pathXml = java.nio.file.Paths.get("C:\\facturaSRI", nombreArchivo);
            if (java.nio.file.Files.exists(pathXml)) {
                xmlBytes = java.nio.file.Files.readAllBytes(pathXml);
            }

            emailServicio.enviarNotificacionFactura(
                    emailCliente,
                    factura.getSecuencial(),
                    pdfBytes,
                    xmlBytes,
                    factura.getEstadoSri(),
                    factura.getMensajeSri());

            return ResponseEntity.ok(java.util.Collections.singletonMap("mensaje", "Correo enviado a " + emailCliente));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error enviando correo: " + e.getMessage());
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

    @GetMapping("/{id}/xml")
    public ResponseEntity<?> descargarXml(@PathVariable Long id) {
        try {
            Factura factura = facturaService.buscarPorId(id);
            if (factura == null)
                return ResponseEntity.notFound().build();

            String nombreArchivo = "factura_" + factura.getSecuencial() + "_firmado.xml";
            java.nio.file.Path pathXml = java.nio.file.Paths.get("C:\\facturaSRI", nombreArchivo);

            if (!java.nio.file.Files.exists(pathXml)) {
                // Intentar con el sin firma
                nombreArchivo = "factura_" + factura.getSecuencial() + ".xml";
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

    @GetMapping("/test-config")
    public ResponseEntity<?> testConfig() {
        facturacion.facturacion.Entidades.Empresa empresa = empresaRepositorio.findById(1L).orElse(null);
        String path = (empresa != null) ? empresa.getRutaFirma() : "NULL";
        boolean passOk = (empresa != null && empresa.getClaveFirma() != null);
        return ResponseEntity.ok("Path: " + path + " | Pass: " + (passOk ? "OK" : "NULL"));
    }

}