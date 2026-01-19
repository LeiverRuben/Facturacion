package facturacion.facturacion.Servicios;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import facturacion.facturacion.Dto.DetalleRetencionDTO;
import facturacion.facturacion.Entidades.*;
import facturacion.facturacion.Repositorios.*;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RetencionServicio {

    private final ComprobanteRetencionRepositorio retencionRepositorio;
    private final DetalleRetencionRepositorio detalleRepositorio;
    private final CompraRepositorio compraRepositorio;
    private final EmpresaRepositorio empresaRepositorio;

    @Transactional
    public ComprobanteRetencion generarRetencion(Long compraId, List<DetalleRetencionDTO> detallesDTO) {
        Compra compra = compraRepositorio.findById(compraId)
                .orElseThrow(() -> new RuntimeException("Compra no encontrada"));

        // Obtener la empresa emisora (asumimos la primera/principal por ahora)
        Empresa empresa = empresaRepositorio.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No hay empresa configurada en el sistema."));

        ComprobanteRetencion retencion = new ComprobanteRetencion();

        // Datos de cabecera
        retencion.setEmpresa(empresa);
        retencion.setProveedor(compra.getProveedor());
        retencion.setFechaEmision(LocalDateTime.now());
        retencion.setPeriodoFiscal(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/yyyy")));
        retencion.setSecuencial(generarSecuencial()); // Temporal logic
        retencion.setEstado(1); // 1 = Creado/Pendiente
        retencion.setEstadoSri("PENDIENTE");

        retencion = retencionRepositorio.save(retencion);

        List<DetalleRetencion> detalles = new ArrayList<>();
        for (DetalleRetencionDTO dto : detallesDTO) {
            DetalleRetencion det = new DetalleRetencion();
            det.setComprobanteRetencion(retencion);
            det.setCodigo(dto.getCodigo());
            det.setCodigoRetencion(dto.getCodigoRetencion());
            det.setBaseImponible(dto.getBaseImponible());
            det.setPorcentajeRetener(dto.getPorcentajeRetener());

            // Calculo valor retenido
            Double valor = dto.getBaseImponible() * (dto.getPorcentajeRetener() / 100);
            det.setValorRetenido(valor);

            // Documento Sustento (La Compra)
            det.setCodDocSustento("01"); // Factura
            det.setNumDocSustento(compra.getNumeroComprobante().replace("-", "")); // Formato simple
            det.setFechaEmisionDocSustento(compra.getFechaEmision().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            detalleRepositorio.save(det);
            detalles.add(det);
        }

        retencion.setImpuestos(detalles);
        return retencion;
    }

    private String generarSecuencial() {
        // Lógica simplificada: buscar conteo + 1
        long count = retencionRepositorio.count() + 1;
        return String.format("%09d", count);
    }

    public List<ComprobanteRetencion> listarRetenciones() {
        return retencionRepositorio.findAll();
    }
}
