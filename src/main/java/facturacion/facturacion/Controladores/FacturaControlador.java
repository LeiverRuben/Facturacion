package facturacion.facturacion.Controladores;

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

    @PostMapping
    public ResponseEntity<?> crearFactura(@RequestBody FacturaRequestDTO request) {
        try {

            Factura factura = facturaService.crearFacturaCompleta(request);

            String claveAcceso = facturaService.generarClaveAcceso(factura);
            factura.setClaveAcceso(claveAcceso);

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

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CONTADOR', 'ROLE_VENDEDOR')")
    @PostMapping("/enviar-sri/{id}")
    public ResponseEntity<?> enviarSri(@PathVariable Long id) {
        try {
            Factura factura = facturaService.buscarPorId(id); // Asumiendo que existe este método o usar repositorio
            if (factura == null) {
                return ResponseEntity.notFound().build();
            }

            // Simulación de envío
            factura.setEstado(3); // Autorizada
            factura.setFechaAutorizacion(java.time.LocalDateTime.now());
            factura.setMensajeSri("AUTORIZADO");

            facturaService.guardar(factura); // Asumiendo método guardar

            return ResponseEntity.ok(new RespuestaFactura(
                    "Factura enviada y autorizada por SRI (Simulación)",
                    factura.getFacturaId(),
                    factura.getClaveAcceso(),
                    null,
                    null));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error en envío SRI: " + e.getMessage());
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