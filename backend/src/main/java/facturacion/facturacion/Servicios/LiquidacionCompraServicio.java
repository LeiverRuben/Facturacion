package facturacion.facturacion.Servicios;

import java.io.File;
import java.time.LocalDateTime;
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

import facturacion.facturacion.Dto.LiquidacionCompraDTO;
import facturacion.facturacion.Dto.RespuestaSriDTO;
import facturacion.facturacion.Entidades.*;
import facturacion.facturacion.Repositorios.*;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
@RequiredArgsConstructor
public class LiquidacionCompraServicio {

    private final LiquidacionDeCompraRepositorio lcRepositorio;
    private final ProveedorRepositorio proveedorRepositorio;
    private final ProductoRepositorio productoRepositorio;
    private final EmpresaRepositorio empresaRepositorio;
    private final KardexServicio kardexServicio;
    private final FirmaElectronicaServicio firmaServicio;
    private final SriServicio sriServicio;
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        System.out.println("LiquidacionCompraServicio: Verificando esquema de base de datos...");
        try {
            // Fix para Liquidación de Compra
            jdbcTemplate.execute("ALTER TABLE liquidacion_de_compra MODIFY COLUMN cliente_id BIGINT NULL DEFAULT NULL");
            System.out.println("OK: Fix aplicado a liquidacion_de_compra (cliente_id -> NULLABLE)");
        } catch (Exception e) {
            System.err.println("WARN: No se pudo modificar liquidacion_de_compra: " + e.getMessage());
        }

        try {
            // Fix para Guía de Remisión (Lo hacemos aquí para asegurar que corra)
            jdbcTemplate.execute("ALTER TABLE guia_de_remision MODIFY COLUMN cliente_id BIGINT NULL DEFAULT NULL");
            System.out.println("OK: Fix aplicado a guia_de_remision (cliente_id -> NULLABLE)");
        } catch (Exception e) {
            System.err.println("WARN: No se pudo modificar guia_de_remision: " + e.getMessage());
        }
    }

    @Transactional
    public LiquidacionDeCompra crear(LiquidacionCompraDTO dto) {
        Proveedor proveedor = proveedorRepositorio.findById(dto.getProveedorId())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        Empresa empresa = empresaRepositorio.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No hay empresa configurada"));

        LiquidacionDeCompra lc = new LiquidacionDeCompra();
        lc.setProveedor(proveedor);
        lc.setEmpresa(empresa);
        lc.setFechaEmision(LocalDateTime.now());

        long count = lcRepositorio.count() + 1;
        String secuencial = String.format("%09d", count);
        lc.setSecuencial(secuencial);

        lc.setEstado(1);
        lc.setEstadoSri("PENDIENTE");

        List<DetalleLiquidacion> detalles = new ArrayList<>();
        double subtotal12 = 0;
        double subtotal0 = 0;
        double totalDescuento = 0;
        double totalIva = 0;

        for (LiquidacionCompraDTO.DetalleLCDTO itemDTO : dto.getDetalles()) {
            Producto producto = productoRepositorio.findById(itemDTO.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado ID: " + itemDTO.getProductoId()));

            DetalleLiquidacion det = new DetalleLiquidacion();
            det.setLiquidacion(lc);
            det.setProducto(producto);
            det.setCodigoPrincipal(producto.getProductoSerial());
            det.setDescripcion(producto.getProductoNombre());

            double cantidad = itemDTO.getCantidad();
            double precio = itemDTO.getPrecioUnitario();
            double descuento = itemDTO.getDescuento() != null ? itemDTO.getDescuento() : 0.0;
            double subtotalLinea = (precio * cantidad) - descuento;

            det.setCantidad(cantidad);
            det.setPrecioUnitario(precio);
            det.setDescuento(descuento);
            det.setPrecioTotalSinImpuesto(subtotalLinea);

            if (producto.getProductoTasa() != null && producto.getProductoTasa() == 12.0) {
                subtotal12 += subtotalLinea;
                totalIva += subtotalLinea * 0.12;
            } else {
                subtotal0 += subtotalLinea;
            }
            totalDescuento += descuento;

            detalles.add(det);

            // ACTUALIZACIÓN INVENTARIO: Liquidación de Compra AUMENTA inventario
            kardexServicio.registrarMovimiento(
                    producto,
                    "ENTRADA",
                    "Liquidación Compra " + secuencial,
                    (int) cantidad,
                    precio, // Costo de compra
                    subtotalLinea);
            // Update logic inside kardex or here
            producto.setProductoStock(producto.getProductoStock() + (int) cantidad);
            productoRepositorio.save(producto);
        }

        lc.setDetalles(detalles);
        lc.setSubtotal12(subtotal12);
        lc.setSubtotal0(subtotal0);
        lc.setSubtotalNoObjeto(0.0);
        lc.setSubtotalExento(0.0);
        lc.setTotalDescuento(totalDescuento);
        lc.setTotalIva(totalIva);
        lc.setTotal(subtotal12 + subtotal0 + totalIva);

        String claveAcceso = generarClaveAcceso(lc);
        lc.setClaveAcceso(claveAcceso);

        return lcRepositorio.save(lc);
    }

    public RespuestaSriDTO enviarSRI(Long id) {
        LiquidacionDeCompra lc = lcRepositorio.findById(id).orElseThrow(() -> new RuntimeException("LC no encontrada"));

        try {
            String pathXml = generarXML(lc);
            String rutaFirma = lc.getEmpresa().getRutaFirma();
            String claveFirma = lc.getEmpresa().getClaveFirma();

            if (rutaFirma == null || "PENDIENTE".equals(rutaFirma)) {
                throw new RuntimeException("No hay firma electrónica configurada.");
            }

            String pathFirmado = firmaServicio.firmarXML(pathXml, rutaFirma, claveFirma);

            byte[] xmlBytes = java.nio.file.Files.readAllBytes(new File(pathFirmado).toPath());
            String responseRecepcion = sriServicio.enviarComprobante(xmlBytes);
            String estadoRecepcion = sriServicio.extraerEstado(responseRecepcion);
            String mensaje = sriServicio.extraerMensaje(responseRecepcion);

            if ("RECIBIDA".equals(estadoRecepcion)) {
                String responseAuth = sriServicio.autorizarComprobante(lc.getClaveAcceso());
                String estadoAuth = sriServicio.extraerEstado(responseAuth);
                String mensajeAuth = sriServicio.extraerMensaje(responseAuth);

                lc.setEstadoSri(estadoAuth);
                lc.setMensajeSri(mensajeAuth);
                if ("AUTORIZADO".equals(estadoAuth)) {
                    lc.setFechaAutorizacion(LocalDateTime.now());
                }
                lcRepositorio.save(lc);
                return new RespuestaSriDTO(estadoAuth, mensajeAuth, LocalDateTime.now().toString());

            } else {
                lc.setEstadoSri(estadoRecepcion);
                lc.setMensajeSri(mensaje);
                lcRepositorio.save(lc);
                return new RespuestaSriDTO(estadoRecepcion, mensaje, null);
            }

        } catch (Exception e) {
            lc.setMensajeSri("Error interno: " + e.getMessage());
            lcRepositorio.save(lc);
            return new RespuestaSriDTO("ERROR", e.getMessage(), null);
        }
    }

    // Lógica Privada

    private String generarClaveAcceso(LiquidacionDeCompra lc) {
        String fecha = lc.getFechaEmision().toLocalDate().format(DateTimeFormatter.ofPattern("ddMMyyyy"));
        String tipoComprobante = "03"; // 03 = Liquidación de Compra
        String ruc = lc.getEmpresa().getRuc();
        String ambiente = String.valueOf(lc.getEmpresa().getAmbiente());
        String estab = lc.getEmpresa().getEstablecimiento();
        String ptoEmi = lc.getEmpresa().getPuntoEmision();
        String secuencial = lc.getSecuencial();
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

    public String generarXML(LiquidacionDeCompra lc) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement("liquidacionCompra");
            root.setAttribute("id", "comprobante");
            root.setAttribute("version", "1.0.0");
            doc.appendChild(root);

            // InfoTributaria
            Element infoTrib = doc.createElement("infoTributaria");
            root.appendChild(infoTrib);

            infoTrib.appendChild(add(doc, "ambiente", lc.getEmpresa().getAmbiente()));
            infoTrib.appendChild(add(doc, "tipoEmision", "1"));
            infoTrib.appendChild(add(doc, "razonSocial", lc.getEmpresa().getRazonSocial()));
            infoTrib.appendChild(add(doc, "ruc", lc.getEmpresa().getRuc()));
            infoTrib.appendChild(add(doc, "claveAcceso", lc.getClaveAcceso()));
            infoTrib.appendChild(add(doc, "codDoc", "03"));
            infoTrib.appendChild(add(doc, "estab", lc.getEmpresa().getEstablecimiento()));
            infoTrib.appendChild(add(doc, "ptoEmi", lc.getEmpresa().getPuntoEmision()));
            infoTrib.appendChild(add(doc, "secuencial", lc.getSecuencial()));
            infoTrib.appendChild(add(doc, "dirMatriz", lc.getEmpresa().getDirMatriz()));

            // InfoLiquidacionCompra
            Element infoLC = doc.createElement("infoLiquidacionCompra");
            root.appendChild(infoLC);

            infoLC.appendChild(add(doc, "fechaEmision",
                    lc.getFechaEmision().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

            String dirEst = lc.getEmpresa().getDirEstablecimiento();
            if (dirEst == null || dirEst.trim().isEmpty()) {
                dirEst = lc.getEmpresa().getDirMatriz();
            }
            if (dirEst == null || dirEst.trim().isEmpty()) {
                dirEst = "S/D";
            }
            infoLC.appendChild(add(doc, "dirEstablecimiento", dirEst));
            infoLC.appendChild(add(doc, "obligadoContabilidad", lc.getEmpresa().getObligadoContabilidad()));
            infoLC.appendChild(add(doc, "tipoIdentificacionProveedor", "04")); // RUC por defecto para Proveedor
            infoLC.appendChild(add(doc, "razonSocialProveedor",
                    lc.getProveedor().getRazonSocial()));
            infoLC.appendChild(add(doc, "identificacionProveedor", lc.getProveedor().getRuc()));
            infoLC.appendChild(add(doc, "direccionProveedor",
                    lc.getProveedor().getDireccion() != null ? lc.getProveedor().getDireccion() : "S/D"));
            infoLC.appendChild(add(doc, "totalSinImpuestos",
                    String.format(java.util.Locale.US, "%.2f", lc.getSubtotal12() + lc.getSubtotal0())));
            infoLC.appendChild(
                    add(doc, "totalDescuento", String.format(java.util.Locale.US, "%.2f", lc.getTotalDescuento())));
            infoLC.appendChild(add(doc, "importeTotal", String.format(java.util.Locale.US, "%.2f", lc.getTotal())));
            infoLC.appendChild(add(doc, "moneda", "DOLAR"));

            // Pagos? Simplificado: Sin Utilización Sistema Financiero
            Element pagos = doc.createElement("pagos");
            Element pago = doc.createElement("pago");
            pago.appendChild(add(doc, "formaPago", "01"));
            pago.appendChild(add(doc, "total", String.format(java.util.Locale.US, "%.2f", lc.getTotal())));
            pagos.appendChild(pago);
            infoLC.appendChild(pagos);

            // TotalConImpuestos
            Element totalImp = doc.createElement("totalConImpuestos");
            infoLC.appendChild(totalImp);

            if (lc.getSubtotal12() > 0) {
                Element imp = doc.createElement("totalImpuesto");
                imp.appendChild(add(doc, "codigo", "2"));
                imp.appendChild(add(doc, "codigoPorcentaje", "2"));
                imp.appendChild(
                        add(doc, "baseImponible", String.format(java.util.Locale.US, "%.2f", lc.getSubtotal12())));
                imp.appendChild(add(doc, "valor", String.format(java.util.Locale.US, "%.2f", lc.getTotalIva())));
                totalImp.appendChild(imp);
            }
            if (lc.getSubtotal0() > 0) {
                Element imp = doc.createElement("totalImpuesto");
                imp.appendChild(add(doc, "codigo", "2"));
                imp.appendChild(add(doc, "codigoPorcentaje", "0"));
                imp.appendChild(
                        add(doc, "baseImponible", String.format(java.util.Locale.US, "%.2f", lc.getSubtotal0())));
                imp.appendChild(add(doc, "valor", "0.00"));
                totalImp.appendChild(imp);
            }

            // Detalles
            Element detalles = doc.createElement("detalles");
            root.appendChild(detalles);

            for (DetalleLiquidacion det : lc.getDetalles()) {
                Element d = doc.createElement("detalle");
                d.appendChild(add(doc, "codigoPrincipal", det.getCodigoPrincipal()));
                d.appendChild(add(doc, "descripcion", det.getDescripcion()));
                d.appendChild(add(doc, "cantidad", String.format(java.util.Locale.US, "%.2f", det.getCantidad())));
                d.appendChild(add(doc, "precioUnitario",
                        String.format(java.util.Locale.US, "%.2f", det.getPrecioUnitario())));
                d.appendChild(add(doc, "descuento", String.format(java.util.Locale.US, "%.2f", det.getDescuento())));
                d.appendChild(add(doc, "precioTotalSinImpuesto",
                        String.format(java.util.Locale.US, "%.2f", det.getPrecioTotalSinImpuesto())));

                Element impuestos = doc.createElement("impuestos");
                d.appendChild(impuestos);
                Element imp = doc.createElement("impuesto");
                imp.appendChild(add(doc, "codigo", "2"));
                if (det.getProducto().getProductoTasa() != null && det.getProducto().getProductoTasa() > 0) {
                    imp.appendChild(add(doc, "codigoPorcentaje", "2"));
                    imp.appendChild(add(doc, "tarifa", "12"));
                    imp.appendChild(add(doc, "baseImponible",
                            String.format(java.util.Locale.US, "%.2f", det.getPrecioTotalSinImpuesto())));
                    imp.appendChild(add(doc, "valor",
                            String.format(java.util.Locale.US, "%.2f", det.getPrecioTotalSinImpuesto() * 0.12)));
                } else {
                    imp.appendChild(add(doc, "codigoPorcentaje", "0"));
                    imp.appendChild(add(doc, "tarifa", "0"));
                    imp.appendChild(add(doc, "baseImponible",
                            String.format(java.util.Locale.US, "%.2f", det.getPrecioTotalSinImpuesto())));
                    imp.appendChild(add(doc, "valor", "0.00"));
                }
                impuestos.appendChild(imp);

                detalles.appendChild(d);
            }

            File carpeta = new File("C:\\facturas_xml\\liquidaciones_compra");
            if (!carpeta.exists())
                carpeta.mkdirs();
            String ruta = "C:\\facturas_xml\\liquidaciones_compra\\LC_" + lc.getSecuencial() + ".xml";

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(doc), new StreamResult(new File(ruta)));
            return ruta;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error creando XML LC: " + e.getMessage());
        }
    }

    private Element add(Document doc, String tag, Object value) {
        Element e = doc.createElement(tag);
        e.appendChild(doc.createTextNode(value != null ? String.valueOf(value) : ""));
        return e;
    }
}
