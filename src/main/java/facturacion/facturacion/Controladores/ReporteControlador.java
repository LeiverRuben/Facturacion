package facturacion.facturacion.Controladores;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lowagie.text.DocumentException;

import facturacion.facturacion.Servicios.ReporteServicio;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/reportes")
public class ReporteControlador {

    @Autowired
    private ReporteServicio reporteServicio;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CONTADOR')")
    @GetMapping("/clientes/pdf")
    public ResponseEntity<byte[]> descargarReporteClientes() {
        try {
            byte[] pdfBytes = reporteServicio.generarReporteClientesPdf();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_clientes.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (DocumentException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CONTADOR')")
    @GetMapping("/ventas/pdf")
    public ResponseEntity<byte[]> descargarReporteVentas() {
        try {
            byte[] pdfBytes = reporteServicio.generarReporteVentasPdf();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_ventas.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (DocumentException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CONTADOR')")
    @GetMapping("/productos/excel")
    public ResponseEntity<byte[]> descargarReporteProductos() {
        try {
            byte[] excelBytes = reporteServicio.generarReporteProductosExcel();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_productos.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(excelBytes);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
