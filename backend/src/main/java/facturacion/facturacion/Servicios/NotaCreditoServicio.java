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

import facturacion.facturacion.Dto.NotaCreditoDTO;
import facturacion.facturacion.Entidades.*;
import facturacion.facturacion.Repositorios.*;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotaCreditoServicio {

    private final NotaDeCreditoRepositorio ncRepositorio;
    private final FacturaRepositorio facturaRepositorio;
    private final ProductoRepositorio productoRepositorio;
    // Removed unused EmpresaRepositorio
    private final KardexServicio kardexServicio;
    private final FirmaElectronicaServicio firmaServicio;
    private final SriServicio sriServicio;

    @Transactional
    public NotaDeCredito crear(NotaCreditoDTO dto) {
        Factura factura = facturaRepositorio.findById(dto.getFacturaId())
                .orElseThrow(() -> new RuntimeException("Factura no encontrada"));

        if (!"AUTORIZADO".equals(factura.getEstadoSri())) {
            throw new RuntimeException("Solo se pueden emitir Notas de Crédito a facturas AUTORIZADAS.");
        }

        NotaDeCredito nc = new NotaDeCredito();
        nc.setFacturaModificada(factura);
        nc.setCliente(factura.getCliente());
        nc.setEmpresa(factura.getEmpresa());
        nc.setFechaEmision(java.time.LocalDateTime.now());
        nc.setMotivo(dto.getMotivo());

        long count = ncRepositorio.count() + 1;
        String secuencial = String.format("%09d", count);
        nc.setSecuencial(secuencial);

        nc.setEstado(1);
        nc.setEstadoSri("PENDIENTE");

        List<DetalleNotaCredito> detalles = new ArrayList<>();
        double subtotal12 = 0;
        double subtotal0 = 0;
        double totalIva = 0;
        double totalDescuento = 0;

        for (NotaCreditoDTO.DetalleNCDTO itemDTO : dto.getDetalles()) {
            Producto producto = productoRepositorio.findById(itemDTO.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado ID: " + itemDTO.getProductoId()));

            DetalleNotaCredito det = new DetalleNotaCredito();
            det.setNotaDeCredito(nc);
            det.setProducto(producto);
            det.setDescripcion(producto.getProductoNombre());

            double cantidad = itemDTO.getCantidad();
            double precio = itemDTO.getPrecioUnitario();
            double descuento = itemDTO.getDescuento();
            double subtotalLinea = (precio * cantidad) - descuento;

            det.setCantidad(cantidad);
            det.setPrecioUnitario(precio);
            det.setDescuento(descuento);
            det.setSubtotal(subtotalLinea);

            if (producto.getProductoTasa() != null && producto.getProductoTasa() == 12.0) {
                subtotal12 += subtotalLinea;
                totalIva += subtotalLinea * (producto.getProductoTasa() / 100.0);
            } else {
                subtotal0 += subtotalLinea;
            }
            totalDescuento += descuento;

            detalles.add(det);

            // AUTO-DEVOLUCIÓN A INVENTARIO (Kardex)
            // FIXED: Signature (Producto, Tipo, Detalle, Cantidad, Prioridad, Total)
            // KardexServicio signature: (Producto, String tipo, String detalle, Integer
            // cantidad, Double costoUnitario, Double totalMovimiento)
            kardexServicio.registrarMovimiento(
                    producto,
                    "ENTRADA",
                    "Devolución Venta (NC " + secuencial + ")",
                    (int) cantidad,
                    producto.getProductoPrecio() != null ? producto.getProductoPrecio() : 0.0,
                    0.0 // totalMovimiento (se recalcula dentro o se pasa 0 si no es relevante aqui)
            );
            producto.setProductoStock(producto.getProductoStock() + (int) cantidad);
            productoRepositorio.save(producto);
        }

        nc.setDetalles(detalles);
        nc.setSubtotal12(subtotal12);
        nc.setSubtotal0(subtotal0);
        nc.setTotalIva(totalIva);
        nc.setTotalDescuento(totalDescuento);
        nc.setTotal(subtotal12 + subtotal0 + totalIva);
        nc.setSubtotalNoObjeto(0.0);
        nc.setSubtotalExento(0.0);

        String claveAcceso = generarClaveAcceso(nc);
        nc.setClaveAcceso(claveAcceso);

        return ncRepositorio.save(nc);
    }

    public facturacion.facturacion.Dto.RespuestaSriDTO enviarSRI(Long id) {
        NotaDeCredito nc = ncRepositorio.findById(id).orElseThrow(() -> new RuntimeException("NC no encontrada"));

        try {
            // 1. Generar XML
            String pathXml = generarXMLNotaCredito(nc);

            // 2. Firmar
            String rutaFirma = nc.getEmpresa().getRutaFirma();
            String claveFirma = nc.getEmpresa().getClaveFirma();

            if (rutaFirma == null || "PENDIENTE".equals(rutaFirma)) {
                throw new RuntimeException("No hay firma electrónica configurada.");
            }

            String pathFirmado = firmaServicio.firmarXML(pathXml, rutaFirma, claveFirma);

            // 3. Enviar (Recepción)
            byte[] xmlBytes = java.nio.file.Files.readAllBytes(new File(pathFirmado).toPath());
            String responseRecepcion = sriServicio.enviarComprobante(xmlBytes);
            String estadoRecepcion = sriServicio.extraerEstado(responseRecepcion);

            if ("RECIBIDA".equals(estadoRecepcion)) {
                // 4. Autorizar
                String responseAuth = sriServicio.autorizarComprobante(nc.getClaveAcceso());
                String estadoAuth = sriServicio.extraerEstado(responseAuth);
                String mensajeAuth = sriServicio.extraerMensaje(responseAuth);

                nc.setEstadoSri(estadoAuth);
                nc.setMensajeSri(mensajeAuth);
                if ("AUTORIZADO".equals(estadoAuth)) {
                    nc.setFechaAutorizacion(java.time.LocalDateTime.now());
                }
                ncRepositorio.save(nc);
                return new facturacion.facturacion.Dto.RespuestaSriDTO(estadoAuth, mensajeAuth,
                        java.time.LocalDateTime.now().toString());

            } else {
                String mensaje = sriServicio.extraerMensaje(responseRecepcion);
                nc.setEstadoSri(estadoRecepcion);
                nc.setMensajeSri(mensaje);
                ncRepositorio.save(nc);
                return new facturacion.facturacion.Dto.RespuestaSriDTO(estadoRecepcion, mensaje, null);
            }

        } catch (Exception e) {
            nc.setMensajeSri("Error interno: " + e.getMessage());
            ncRepositorio.save(nc);
            return new facturacion.facturacion.Dto.RespuestaSriDTO("ERROR", e.getMessage(), null);
        }
    }

    private String generarClaveAcceso(NotaDeCredito nc) {
        String fecha = nc.getFechaEmision().toLocalDate()
                .format(DateTimeFormatter.ofPattern("ddMMyyyy"));
        String tipoComprobante = "04"; // 04 = Nota de Crédito
        String ruc = nc.getEmpresa().getRuc();
        String ambiente = String.valueOf(nc.getEmpresa().getAmbiente());
        String estab = nc.getEmpresa().getEstablecimiento();
        String ptoEmi = nc.getEmpresa().getPuntoEmision();
        String secuencial = nc.getSecuencial();
        String codigoNumerico = "12345678";
        String tipoEmision = "1";

        String claveSinDV = fecha + tipoComprobante + ruc + ambiente +
                estab + ptoEmi + secuencial + codigoNumerico + tipoEmision;
        String dv = calcularDigitoVerificador(claveSinDV);
        return claveSinDV + dv;
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

    public String generarXMLNotaCredito(NotaDeCredito nc) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("notaCredito");
            root.setAttribute("id", "comprobante");
            root.setAttribute("version", "1.0.0");
            doc.appendChild(root);

            // 1. InfoTributaria
            Element infoTrib = doc.createElement("infoTributaria");
            root.appendChild(infoTrib);

            infoTrib.appendChild(add(doc, "ambiente", nc.getEmpresa().getAmbiente()));
            infoTrib.appendChild(add(doc, "tipoEmision", "1"));
            infoTrib.appendChild(add(doc, "razonSocial", nc.getEmpresa().getRazonSocial()));
            infoTrib.appendChild(add(doc, "ruc", nc.getEmpresa().getRuc()));
            infoTrib.appendChild(add(doc, "claveAcceso", nc.getClaveAcceso()));
            infoTrib.appendChild(add(doc, "codDoc", "04"));
            infoTrib.appendChild(add(doc, "estab", nc.getEmpresa().getEstablecimiento()));
            infoTrib.appendChild(add(doc, "ptoEmi", nc.getEmpresa().getPuntoEmision()));
            infoTrib.appendChild(add(doc, "secuencial", nc.getSecuencial()));
            infoTrib.appendChild(add(doc, "dirMatriz", nc.getEmpresa().getDirMatriz()));

            // 2. InfoNotaCredito
            Element infoNC = doc.createElement("infoNotaCredito");
            root.appendChild(infoNC);

            infoNC.appendChild(add(doc, "fechaEmision",
                    nc.getFechaEmision().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
            infoNC.appendChild(add(doc, "dirEstablecimiento", nc.getEmpresa().getDirEstablecimiento()));
            infoNC.appendChild(add(doc, "tipoIdentificacionComprador", "05"));
            infoNC.appendChild(add(doc, "razonSocialComprador",
                    nc.getCliente().getClienteNombre() + " " + nc.getCliente().getClienteApellido()));
            infoNC.appendChild(add(doc, "identificacionComprador",
                    nc.getCliente().getIdentificacion() != null ? nc.getCliente().getIdentificacion()
                            : "9999999999999"));
            infoNC.appendChild(add(doc, "obligadoContabilidad", nc.getEmpresa().getObligadoContabilidad()));
            infoNC.appendChild(add(doc, "codDocModificado", "01"));
            infoNC.appendChild(add(doc, "numDocModificado", nc.getFacturaModificada().getSecuencial()));
            infoNC.appendChild(add(doc, "fechaEmisionDocSustento", nc.getFacturaModificada().getFechaEmision()
                    .toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
            infoNC.appendChild(add(doc, "totalSinImpuestos",
                    String.format(java.util.Locale.US, "%.2f", nc.getSubtotal12() + nc.getSubtotal0())));
            infoNC.appendChild(
                    add(doc, "valorModificacion", String.format(java.util.Locale.US, "%.2f", nc.getTotal())));
            infoNC.appendChild(add(doc, "moneda", "DOLAR"));

            Element totalImp = doc.createElement("totalConImpuestos");
            infoNC.appendChild(totalImp);

            if (nc.getSubtotal12() > 0) {
                Element imp = doc.createElement("totalImpuesto");
                imp.appendChild(add(doc, "codigo", "2"));
                imp.appendChild(add(doc, "codigoPorcentaje", "2"));
                imp.appendChild(
                        add(doc, "baseImponible", String.format(java.util.Locale.US, "%.2f", nc.getSubtotal12())));
                imp.appendChild(add(doc, "valor", String.format(java.util.Locale.US, "%.2f", nc.getTotalIva())));
                totalImp.appendChild(imp);
            }
            if (nc.getSubtotal0() > 0) {
                Element imp = doc.createElement("totalImpuesto");
                imp.appendChild(add(doc, "codigo", "2"));
                imp.appendChild(add(doc, "codigoPorcentaje", "0"));
                imp.appendChild(
                        add(doc, "baseImponible", String.format(java.util.Locale.US, "%.2f", nc.getSubtotal0())));
                imp.appendChild(add(doc, "valor", "0.00"));
                totalImp.appendChild(imp);
            }

            infoNC.appendChild(add(doc, "motivo", nc.getMotivo()));

            // 3. Detalles
            Element detalles = doc.createElement("detalles");
            root.appendChild(detalles);

            for (DetalleNotaCredito det : nc.getDetalles()) {
                Element d = doc.createElement("detalle");
                d.appendChild(add(doc, "codigoInterno", det.getProducto().getProductoSerial()));
                d.appendChild(add(doc, "descripcion", det.getDescripcion()));
                d.appendChild(add(doc, "cantidad", String.format(java.util.Locale.US, "%.2f", det.getCantidad())));
                d.appendChild(add(doc, "precioUnitario",
                        String.format(java.util.Locale.US, "%.2f", det.getPrecioUnitario())));
                d.appendChild(add(doc, "descuento", String.format(java.util.Locale.US, "%.2f", det.getDescuento())));
                d.appendChild(add(doc, "precioTotalSinImpuesto",
                        String.format(java.util.Locale.US, "%.2f", det.getSubtotal())));

                Element impuestos = doc.createElement("impuestos");
                d.appendChild(impuestos);
                Element imp = doc.createElement("impuesto");
                imp.appendChild(add(doc, "codigo", "2"));
                if (det.getProducto().getProductoTasa() != null && det.getProducto().getProductoTasa() > 0) {
                    imp.appendChild(add(doc, "codigoPorcentaje", "2"));
                    imp.appendChild(add(doc, "tarifa", "12"));
                    imp.appendChild(
                            add(doc, "baseImponible", String.format(java.util.Locale.US, "%.2f", det.getSubtotal())));
                    imp.appendChild(
                            add(doc, "valor", String.format(java.util.Locale.US, "%.2f", det.getSubtotal() * 0.12)));
                } else {
                    imp.appendChild(add(doc, "codigoPorcentaje", "0"));
                    imp.appendChild(add(doc, "tarifa", "0"));
                    imp.appendChild(
                            add(doc, "baseImponible", String.format(java.util.Locale.US, "%.2f", det.getSubtotal())));
                    imp.appendChild(add(doc, "valor", "0.00"));
                }
                impuestos.appendChild(imp);

                detalles.appendChild(d);
            }

            File carpeta = new File("C:\\facturas_xml\\notas_credito");
            if (!carpeta.exists())
                carpeta.mkdirs();
            String ruta = "C:\\facturas_xml\\notas_credito\\NC_" + nc.getSecuencial() + ".xml";

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(doc), new StreamResult(new File(ruta)));
            return ruta;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error creando XML NC: " + e.getMessage());
        }
    }

    private Element add(Document doc, String tag, Object value) {
        Element e = doc.createElement(tag);
        e.appendChild(doc.createTextNode(value != null ? String.valueOf(value) : ""));
        return e;
    }
}
