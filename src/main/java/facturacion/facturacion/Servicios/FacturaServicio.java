package facturacion.facturacion.Servicios;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import facturacion.facturacion.Dto.DetalleFacturaDTO;
import facturacion.facturacion.Dto.FacturaRequestDTO;
import facturacion.facturacion.Dto.PagoDTO;

import facturacion.facturacion.Entidades.*;
import facturacion.facturacion.Repositorios.*;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FacturaServicio {

    private final FacturaRepositorio facturaRepository;
    private final ClienteRepositorio clienteRepository;
    private final EmpresaRepositorio empresaRepository;
    private final ProductoRepositorio productoRepository;
    private final DetalleFacturaRepositorio detalleRepository;
    private final ImpuestoDetalleRepositorio impuestoRepository;
    private final FormaPagoRepositorio formaPagoRepository;
    private final FacturaPagoRepositorio facturaPagoRepository;

    @Transactional
    public Factura crearFacturaCompleta(FacturaRequestDTO request) {
        if (request.getDetalles() == null || request.getDetalles().isEmpty())
            throw new RuntimeException("Debe ingresar al menos un detalle.");
        if (request.getPagos() == null || request.getPagos().isEmpty())
            throw new RuntimeException("Debe registrar al menos una forma de pago.");
        if (request.getClienteId() == null)
            throw new RuntimeException("Debe seleccionar un cliente.");
        if (request.getEmpresaId() == null)
            throw new RuntimeException("Debe seleccionar una empresa emisora.");
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado."));
        Empresa empresa = empresaRepository.findById(request.getEmpresaId())
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada."));
        Factura factura = new Factura();
        factura.setSecuencial(request.getSecuencial());
        factura.setFechaEmision(request.getFechaEmision());
        factura.setSubtotal12(request.getSubtotal12());
        factura.setSubtotal0(request.getSubtotal0());
        factura.setSubtotalExento(request.getSubtotalExento());
        factura.setSubtotalNoObjeto(request.getSubtotalNoObjeto());
        factura.setTotalDescuento(request.getTotalDescuento());
        factura.setTotalIva(request.getTotalIva());
        factura.setTotalFactura(request.getTotalFactura());
        factura.setEstado(1);
        factura.setCliente(cliente);
        factura.setEmpresa(empresa);
        factura = facturaRepository.save(factura);
        List<DetalleFactura> detalles = new ArrayList<>();
        for (DetalleFacturaDTO detDTO : request.getDetalles()) {
            Producto producto = productoRepository.findById(detDTO.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado."));
            DetalleFactura det = new DetalleFactura();
            det.setFactura(factura);
            det.setProducto(producto);
            det.setCantidad(detDTO.getCantidad());
            det.setPrecioUnitario(detDTO.getPrecioUnitario());
            det.setDescuento(detDTO.getDescuento());
            det.setSubtotal(detDTO.getSubtotal());
            DetalleFactura detalleGuardado = detalleRepository.save(det);
            ImpuestoDetalle imp = new ImpuestoDetalle();
            imp.setDetalleFactura(detalleGuardado);
            imp.setCodigo(detDTO.getImpuesto().getCodigo());
            imp.setCodigoPorcentaje(detDTO.getImpuesto().getCodigoPorcentaje());
            imp.setTarifa(detDTO.getImpuesto().getTarifa());
            imp.setBaseImponible(detDTO.getImpuesto().getBaseImponible());
            imp.setValor(detDTO.getImpuesto().getValor());
            impuestoRepository.save(imp);
            detalles.add(det);
        }
        for (PagoDTO pdto : request.getPagos()) {
            FormaPago fpago = formaPagoRepository.findById(pdto.getMetodoPagoId())
                    .orElseThrow(() -> new RuntimeException("Forma de pago no encontrada."));
            FacturaPago fp = new FacturaPago();
            fp.setFactura(factura);
            fp.setFormaPago(fpago);
            fp.setTotal(pdto.getTotal());
            fp.setPlazo(pdto.getPlazo());
            fp.setUnidadTiempo(pdto.getUnidadTiempo());
            facturaPagoRepository.save(fp);
        }
        factura.setDetalles(detalles);
        return factura;
    }

    public String generarClaveAcceso(Factura factura) {
        String fecha = factura.getFechaEmision().toLocalDate()
                .format(DateTimeFormatter.ofPattern("ddMMyyyy"));
        String tipoComprobante = "01";
        String ruc = factura.getEmpresa().getRuc();
        String ambiente = String.valueOf(factura.getEmpresa().getAmbiente()); // 1 pruebas, 2 producción
        String estab = factura.getEmpresa().getEstablecimiento();
        String ptoEmi = factura.getEmpresa().getPuntoEmision();
        String secuencial = factura.getSecuencial();
        String codigoNumerico = generarCodigoNumerico();
        String tipoEmision = "1";
        String claveSinDV = fecha + tipoComprobante + ruc + ambiente +
                estab + ptoEmi + secuencial + codigoNumerico + tipoEmision;
        String dv = calcularDigitoVerificador(claveSinDV);
        return claveSinDV + dv;
    }

    private String generarCodigoNumerico() {
        int numero = (int) (Math.random() * 99999999);
        return String.format("%08d", numero);
    }

    private String calcularDigitoVerificador(String cadena) {
        int factor = 2;
        int suma = 0;
        for (int i = cadena.length() - 1; i >= 0; i--) {
            suma += Character.getNumericValue(cadena.charAt(i)) * factor;
            factor++;
            if (factor > 7)
                factor = 2;
        }
        int dv = 11 - (suma % 11);
        if (dv == 11)
            return "0";
        if (dv == 10)
            return "1";
        return String.valueOf(dv);
    }

    public String generarXMLFactura(Factura factura) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element facturaEl = doc.createElement("factura");
            facturaEl.setAttribute("id", "comprobante");
            facturaEl.setAttribute("version", "2.0.0");
            doc.appendChild(facturaEl);

            Element infoTrib = doc.createElement("infoTributaria");
            facturaEl.appendChild(infoTrib);

            infoTrib.appendChild(add(doc, "razonSocial", factura.getEmpresa().getRazonSocial()));
            infoTrib.appendChild(add(doc, "nombreComercial", factura.getEmpresa().getNombreComercial()));
            infoTrib.appendChild(add(doc, "ruc", factura.getEmpresa().getRuc()));
            infoTrib.appendChild(add(doc, "claveAcceso", factura.getClaveAcceso()));
            infoTrib.appendChild(add(doc, "codDoc", "01"));
            infoTrib.appendChild(add(doc, "estab", factura.getEmpresa().getEstablecimiento()));
            infoTrib.appendChild(add(doc, "ptoEmi", factura.getEmpresa().getPuntoEmision()));
            infoTrib.appendChild(add(doc, "secuencial", factura.getSecuencial()));
            infoTrib.appendChild(add(doc, "dirMatriz", factura.getEmpresa().getDirEstablecimiento()));

            Element infoFac = doc.createElement("infoFactura");
            facturaEl.appendChild(infoFac);

            infoFac.appendChild(add(doc, "fechaEmision",
                    factura.getFechaEmision().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
            infoFac.appendChild(add(doc, "razonSocialComprador", factura.getCliente().getClienteNombre()));
            infoFac.appendChild(add(doc, "identificacionComprador", factura.getCliente().getClienteTelefono()));
            infoFac.appendChild(add(doc, "totalSinImpuestos", factura.getSubtotal12() + factura.getSubtotal0()));
            infoFac.appendChild(add(doc, "importeTotal", factura.getTotalFactura()));

            Element detallesEl = doc.createElement("detalles");
            facturaEl.appendChild(detallesEl);

            factura.getDetalles().forEach(det -> {
                Element d = doc.createElement("detalle");
                d.appendChild(add(doc, "descripcion", det.getProducto().getProductoNombre()));
                d.appendChild(add(doc, "cantidad", det.getCantidad()));
                d.appendChild(add(doc, "precioUnitario", det.getPrecioUnitario()));
                d.appendChild(add(doc, "descuento", det.getDescuento()));
                d.appendChild(add(doc, "precioTotalSinImpuesto", det.getSubtotal()));
                detallesEl.appendChild(d);
            });
            File carpeta = new File("C:\\facturaSRI");
            if (!carpeta.exists())
                carpeta.mkdirs();
            String ruta = "C:\\facturaSRI\\factura_" + factura.getSecuencial() + ".xml";
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(new File(ruta)));
            return ruta;

        } catch (Exception e) {
            throw new RuntimeException("Error al generar XML: " + e.getMessage());
        }
    }

    private Element add(Document doc, String tag, Object value) {
        Element e = doc.createElement(tag);
        e.appendChild(doc.createTextNode(String.valueOf(value)));
        return e;
    }

    public Factura buscarPorId(Long id) {
        return facturaRepository.findById(id).orElse(null);
    }

    public Factura guardar(Factura factura) {
        return facturaRepository.save(factura);
    }
}