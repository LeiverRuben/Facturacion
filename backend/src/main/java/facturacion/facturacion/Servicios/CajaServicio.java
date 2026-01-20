package facturacion.facturacion.Servicios;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import facturacion.facturacion.Entidades.Caja;
import facturacion.facturacion.Entidades.SesionCaja;
import facturacion.facturacion.Entidades.Usuario;
import facturacion.facturacion.Repositorios.CajaRepositorio;
import facturacion.facturacion.Repositorios.SesionCajaRepositorio;
import facturacion.facturacion.Repositorios.UsuarioRepositorio;

@Service
public class CajaServicio {

        @Autowired
        private SesionCajaRepositorio sesionCajaRepositorio;

        @Autowired
        private CajaRepositorio cajaRepositorio;

        @Autowired
        private UsuarioRepositorio usuarioRepositorio;

        // Verificar si el usuario ya tiene sesión abierta
        public Optional<SesionCaja> obtenerSesionActiva(String username) {
                Usuario usuario = usuarioRepositorio.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
                return sesionCajaRepositorio.findSesionAbiertaPorUsuario(usuario);
        }

        // Abrir Caja
        public SesionCaja abrirCaja(Long cajaId, Double montoInicial, String username) {
                // 1. Validar usuario
                Usuario usuario = usuarioRepositorio.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

                // 2. Verificar que no tenga otra abierta
                if (sesionCajaRepositorio.findSesionAbiertaPorUsuario(usuario).isPresent()) {
                        throw new RuntimeException("El usuario ya tiene una caja abierta.");
                }

                // 3. Buscar Caja
                Caja caja = cajaRepositorio.findById(cajaId)
                                .orElseThrow(() -> new RuntimeException("Caja no encontrada"));

                // 4. Crear nueva sesión
                SesionCaja sesion = new SesionCaja();
                sesion.setCaja(caja);
                sesion.setUsuario(usuario);
                sesion.setFechaApertura(LocalDateTime.now());
                sesion.setMontoInicial(montoInicial);
                sesion.setEstado("ABIERTA");

                return sesionCajaRepositorio.save(sesion);
        }

        // Cerrar Caja
        public SesionCaja cerrarCaja(Long sesionId, Double montoFinal, Double totalVentasFrontend) {
                SesionCaja sesion = sesionCajaRepositorio.findById(sesionId)
                                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

                if (!"ABIERTA".equals(sesion.getEstado())) {
                        throw new RuntimeException("La sesión ya está cerrada.");
                }

                // Calcular total de ventas real desde la base de datos para asegurar integridad
                java.util.List<facturacion.facturacion.Entidades.Factura> facturas = facturaRepositorio
                                .findBySesionCaja(sesion);
                Double totalVentasReal = facturas.stream()
                                .mapToDouble(facturacion.facturacion.Entidades.Factura::getTotalFactura)
                                .sum();

                sesion.setFechaCierre(LocalDateTime.now());
                sesion.setMontoFinal(montoFinal);
                sesion.setTotalVentasEfectivo(totalVentasReal); // Usar el calculado
                sesion.setEstado("CERRADA");

                return sesionCajaRepositorio.save(sesion);
        }

        @Autowired
        private facturacion.facturacion.Repositorios.MovimientoCajaRepositorio movimientoCajaRepositorio;

        @Autowired
        private facturacion.facturacion.Repositorios.FacturaRepositorio facturaRepositorio;

        // ... (previous methods)

        // Registrar Movimiento (Ingreso/Egreso)
        public facturacion.facturacion.Entidades.MovimientoCaja registrarMovimiento(Long sesionId, String tipo,
                        Double monto, String descripcion) {
                SesionCaja sesion = sesionCajaRepositorio.findById(sesionId)
                                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

                if (!"ABIERTA".equals(sesion.getEstado())) {
                        throw new RuntimeException("La sesión está cerrada, no se pueden registrar movimientos.");
                }

                facturacion.facturacion.Entidades.MovimientoCaja mov = new facturacion.facturacion.Entidades.MovimientoCaja();
                mov.setSesionCaja(sesion);
                mov.setUsuario(sesion.getUsuario());
                mov.setTipo(tipo);
                mov.setMonto(monto);
                mov.setDescripcion(descripcion);
                mov.setFecha(LocalDateTime.now());

                return movimientoCajaRepositorio.save(mov);
        }

        // Obtener Resumen en Vivo
        public java.util.Map<String, Object> obtenerResumenSesion(Long sesionId) {
                SesionCaja sesion = sesionCajaRepositorio.findById(sesionId)
                                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

                // 1. Obtener Movimientos
                java.util.List<facturacion.facturacion.Entidades.MovimientoCaja> movimientos = movimientoCajaRepositorio
                                .findBySesionCaja(sesion);

                Double totalIngresos = movimientos.stream()
                                .filter(m -> "INGRESO".equals(m.getTipo()))
                                .mapToDouble(facturacion.facturacion.Entidades.MovimientoCaja::getMonto)
                                .sum();

                Double totalEgresos = movimientos.stream()
                                .filter(m -> "EGRESO".equals(m.getTipo()))
                                .mapToDouble(facturacion.facturacion.Entidades.MovimientoCaja::getMonto)
                                .sum();

                // 2. Obtener Ventas (Facturas)
                java.util.List<facturacion.facturacion.Entidades.Factura> facturas = facturaRepositorio
                                .findBySesionCaja(sesion);
                Double totalVentas = facturas.stream()
                                .mapToDouble(facturacion.facturacion.Entidades.Factura::getTotalFactura)
                                .sum();

                // 3. Calcular Saldo Actual
                // Formula: Saldo Inicial + Ventas + Ingresos - Egresos
                Double saldoActual = sesion.getMontoInicial() + totalVentas + totalIngresos - totalEgresos;

                return java.util.Map.of(
                                "sesion", sesion,
                                "totalIngresos", totalIngresos,
                                "totalEgresos", totalEgresos,
                                "totalVentas", totalVentas,
                                "saldoActual", saldoActual,
                                "movimientos", movimientos);
        }

        // Obtener Historial Completo
        public java.util.List<SesionCaja> obtenerHistorial() {
                return sesionCajaRepositorio.findAllByOrderByFechaAperturaDesc();
        }

        // Eliminar Sesión (Solo si no tiene facturas)
        public void eliminarSesion(Long sesionId) {
                SesionCaja sesion = sesionCajaRepositorio.findById(sesionId)
                                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

                // 1. Verificar si tiene facturas asociadas
                java.util.List<facturacion.facturacion.Entidades.Factura> facturas = facturaRepositorio
                                .findBySesionCaja(sesion);

                if (!facturas.isEmpty()) {
                        throw new RuntimeException(
                                        "No se puede eliminar la sesión porque tiene ventas registradas. Debe anular las facturas primero.");
                }

                // 2. Eliminar Movimientos asociados (Cascada manual si no está en JPA)
                java.util.List<facturacion.facturacion.Entidades.MovimientoCaja> movimientos = movimientoCajaRepositorio
                                .findBySesionCaja(sesion);
                movimientoCajaRepositorio.deleteAll(movimientos);

                // 3. Eliminar la sesión
                sesionCajaRepositorio.delete(sesion);
        }
}
