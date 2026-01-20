package facturacion.facturacion.Controladores;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import facturacion.facturacion.Entidades.SesionCaja;
import facturacion.facturacion.Servicios.CajaServicio;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/caja")
@CrossOrigin("*")
public class CajaControlador {

    @Autowired
    private CajaServicio cajaServicio;

    @GetMapping("/estado")
    public ResponseEntity<?> obtenerEstado() {
        String username = obtenerUsuarioActual();
        Optional<SesionCaja> sesion = cajaServicio.obtenerSesionActiva(username);

        if (sesion.isPresent()) {
            return ResponseEntity.ok(sesion.get());
        } else {
            return ResponseEntity.ok(Map.of("mensaje", "No hay sesión abierta", "estado", "CERRADA"));
        }
    }

    @PostMapping("/abrir")
    public ResponseEntity<?> abrirCaja(@RequestBody Map<String, Object> request) {
        try {
            String username = obtenerUsuarioActual();
            // Default Caja ID 1 si no se envía (caso simple)
            Long cajaId = request.containsKey("cajaId") ? Long.valueOf(request.get("cajaId").toString()) : 1L;
            Double montoInicial = Double.valueOf(request.get("montoInicial").toString());

            SesionCaja sesion = cajaServicio.abrirCaja(cajaId, montoInicial, username);
            return ResponseEntity.ok(sesion);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/cerrar")
    public ResponseEntity<?> cerrarCaja(@RequestBody Map<String, Object> request) {
        try {
            Long sesionId = Long.valueOf(request.get("sesionId").toString());
            Double montoFinal = Double.valueOf(request.get("montoFinal").toString());
            Double totalVentas = Double.valueOf(request.get("totalVentas").toString());

            SesionCaja sesion = cajaServicio.cerrarCaja(sesionId, montoFinal, totalVentas);
            return ResponseEntity.ok(sesion);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/movimiento")
    public ResponseEntity<?> registrarMovimiento(@RequestBody Map<String, Object> request) {
        try {
            Long sesionId = Long.valueOf(request.get("sesionId").toString());
            String tipo = request.get("tipo").toString(); // INGRESO, EGRESO
            Double monto = Double.valueOf(request.get("monto").toString());
            String descripcion = request.get("descripcion").toString();

            facturacion.facturacion.Entidades.MovimientoCaja mov = cajaServicio.registrarMovimiento(sesionId, tipo,
                    monto, descripcion);
            return ResponseEntity.ok(mov);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/resumen/{sesionId}")
    public ResponseEntity<?> obtenerResumen(@PathVariable Long sesionId) {
        try {
            Map<String, Object> resumen = cajaServicio.obtenerResumenSesion(sesionId);
            return ResponseEntity.ok(resumen);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/historial")
    public java.util.List<SesionCaja> obtenerHistorial() {
        return cajaServicio.obtenerHistorial();
    }

    private String obtenerUsuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<?> eliminarSesion(@PathVariable Long id) {
        try {
            cajaServicio.eliminarSesion(id);
            return ResponseEntity.ok(Map.of("mensaje", "Sesión eliminada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
