package facturacion.facturacion.Controladores;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import facturacion.facturacion.Dto.DashboardStatsDTO;
import facturacion.facturacion.Entidades.Factura;
import facturacion.facturacion.Repositorios.ClienteRepositorio;
import facturacion.facturacion.Repositorios.FacturaRepositorio;
import facturacion.facturacion.Repositorios.ProductoRepositorio;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardControlador {

    @Autowired
    private FacturaRepositorio facturaRepositorio;

    @Autowired
    private ClienteRepositorio clienteRepositorio;

    @Autowired
    private ProductoRepositorio productoRepositorio;

    @GetMapping("/stats")
    public DashboardStatsDTO getStats() {
        DashboardStatsDTO stats = new DashboardStatsDTO();

        // 1. Conteos Básicos
        stats.setTotalClientes(clienteRepositorio.count());
        stats.setTotalProductos(productoRepositorio.count());
        stats.setTotalFacturas(facturaRepositorio.count());

        // 2. Ventas de Hoy
        LocalDateTime inicioHoy = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime finHoy = LocalDateTime.now().with(LocalTime.MAX);
        Double totalHoy = facturaRepositorio.sumTotalFacturaByFechaEmisionBetween(inicioHoy, finHoy);
        stats.setVentasHoy(totalHoy != null ? totalHoy : 0.0);

        // 3. Ventas de la Semana (Lunes a Domingo o últimos 7 días - Usaremos Inicio de
        // Semana actual)
        LocalDateTime inicioSemana = LocalDateTime.now().minusDays(LocalDateTime.now().getDayOfWeek().getValue() - 1)
                .with(LocalTime.MIN);
        Double totalSemana = facturaRepositorio.sumTotalFacturaByFechaEmisionBetween(inicioSemana, finHoy);
        stats.setVentasSemana(totalSemana != null ? totalSemana : 0.0);

        // 4. Últimas 5 Facturas
        List<Factura> ultimas = facturaRepositorio.findTop5ByOrderByFacturaIdDesc();
        stats.setUltimasFacturas(ultimas);

        // 5. Ventas de la Última Semana (Desglose Diario)
        LocalDateTime hace7Dias = LocalDateTime.now().minusDays(6).with(LocalTime.MIN);
        List<Factura> facturasSemana = facturaRepositorio.findByFechaEmisionBetween(hace7Dias, finHoy);

        // Inicializar mapa con los últimos 7 días en orden
        java.util.Map<String, Double> ventasPorDiaMap = new java.util.LinkedHashMap<>();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("EEE",
                java.util.Locale.forLanguageTag("es-ES"));

        for (int i = 6; i >= 0; i--) {
            LocalDateTime dia = LocalDateTime.now().minusDays(i);
            String label = dia.format(formatter); // Lun, Mar, etc.
            // Capitalizar la primera letra (lun -> Lun)
            label = label.substring(0, 1).toUpperCase() + label.substring(1);
            ventasPorDiaMap.put(label, 0.0);
        }

        // Sumarizar ventas reales (comparando por dia del mes para simplicidad en este
        // scope)
        // Nota: Si el rango cruza meses/años, lo ideal es usar LocalDate como clave,
        // pero aqui usamo el label para mapear rápido
        // Para mayor precisión usaremos LocalDate como clave intermedia
        java.util.Map<java.time.LocalDate, Double> ventasPorFecha = new java.util.LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            ventasPorFecha.put(java.time.LocalDate.now().minusDays(i), 0.0);
        }

        for (Factura f : facturasSemana) {
            java.time.LocalDate fecha = f.getFechaEmision().toLocalDate();
            ventasPorFecha.merge(fecha, f.getTotalFactura(), Double::sum);
        }

        // Convertir a DTO
        java.util.List<DashboardStatsDTO.VentaDiariaDTO> ventasDiarias = new java.util.ArrayList<>();

        for (java.util.Map.Entry<java.time.LocalDate, Double> entry : ventasPorFecha.entrySet()) {
            DashboardStatsDTO.VentaDiariaDTO dto = new DashboardStatsDTO.VentaDiariaDTO();
            String label = entry.getKey().format(formatter);
            label = label.substring(0, 1).toUpperCase() + label.substring(1);

            dto.setDia(label);
            dto.setTotal(entry.getValue());
            ventasDiarias.add(dto);
        }
        stats.setVentasUltimaSemana(ventasDiarias);

        return stats;
    }
}
