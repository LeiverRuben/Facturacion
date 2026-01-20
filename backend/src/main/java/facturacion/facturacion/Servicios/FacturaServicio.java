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

import org.springframework.security.core.context.SecurityContextHolder;

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
    private final SesionCajaRepositorio sesionCajaRepository;
    private final UsuarioRepositorio usuarioRepository;

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

        // Validar Sesión de Caja Activa
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuarioActual = usuarioRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado."));

        SesionCaja sesionCaja = sesionCajaRepository.findSesionAbiertaPorUsuario(usuarioActual)
                .orElseThrow(
                        () -> new RuntimeException("⚠️ CAJA CERRADA: No se puede facturar sin abrir turno en caja."));

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
        factura.setEstadoSri("PENDIENTE");
        factura.setCliente(cliente);
        factura.setEmpresa(empresa);
        factura.setSesionCaja(sesionCaja); // Vincular sesión
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
            facturaEl.setAttribute("version", "1.0.0");
            doc.appendChild(facturaEl);

            // 1. InfoTributaria
            Element infoTrib = doc.createElement("infoTributaria");
            facturaEl.appendChild(infoTrib);

            infoTrib.appendChild(add(doc, "ambiente", factura.getEmpresa().getAmbiente()));
            infoTrib.appendChild(add(doc, "tipoEmision", "1"));
            infoTrib.appendChild(add(doc, "razonSocial", factura.getEmpresa().getRazonSocial()));
            addOptional(doc, infoTrib, "nombreComercial", factura.getEmpresa().getNombreComercial());
            infoTrib.appendChild(add(doc, "ruc", factura.getEmpresa().getRuc()));
            infoTrib.appendChild(add(doc, "claveAcceso", factura.getClaveAcceso()));
            infoTrib.appendChild(add(doc, "codDoc", "01"));
            infoTrib.appendChild(add(doc, "estab", factura.getEmpresa().getEstablecimiento()));
            infoTrib.appendChild(add(doc, "ptoEmi", factura.getEmpresa().getPuntoEmision()));
            infoTrib.appendChild(add(doc, "secuencial", factura.getSecuencial()));
            infoTrib.appendChild(add(doc, "dirMatriz", factura.getEmpresa().getDirEstablecimiento()));

            // 2. InfoFactura
            Element infoFac = doc.createElement("infoFactura");
            facturaEl.appendChild(infoFac);

            infoFac.appendChild(add(doc, "fechaEmision",
                    factura.getFechaEmision().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
            addOptional(doc, infoFac, "dirEstablecimiento", factura.getEmpresa().getDirEstablecimiento());
            addOptional(doc, infoFac, "contribuyenteEspecial", factura.getEmpresa().getContribuyenteEspecial());
            infoFac.appendChild(add(doc, "obligadoContabilidad",
                    factura.getEmpresa().getObligadoContabilidad() != null
                            ? factura.getEmpresa().getObligadoContabilidad()
                            : "NO"));

            String ident = factura.getCliente().getIdentificacion();
            String tipoIdent = getTipoIdentificacion(ident);

            infoFac.appendChild(add(doc, "tipoIdentificacionComprador", tipoIdent));
            addOptional(doc, infoFac, "guiaRemision", null); // Placeholder si existiera
            infoFac.appendChild(add(doc, "razonSocialComprador",
                    factura.getCliente().getClienteNombre() + " " + factura.getCliente().getClienteApellido()));
            infoFac.appendChild(add(doc, "identificacionComprador", ident));
            addOptional(doc, infoFac, "direccionComprador", factura.getCliente().getClienteDireccion());

            infoFac.appendChild(add(doc, "totalSinImpuestos",
                    String.format(java.util.Locale.US, "%.2f", factura.getSubtotal12() + factura.getSubtotal0()
                            + factura.getSubtotalNoObjeto() + factura.getSubtotalExento())));
            infoFac.appendChild(
                    add(doc, "totalDescuento",
                            String.format(java.util.Locale.US, "%.2f", factura.getTotalDescuento())));

            // Bloque TotalConImpuestos
            Element totalConImpuestos = doc.createElement("totalConImpuestos");
            infoFac.appendChild(totalConImpuestos);

            // Determinar Tasa de IVA (12% vs 15%)
            String codigoPorcentajeIva = "4"; // Default 15% (Code 4)
            String tarifaIva = "15";
            if (factura.getSubtotal12() > 0 && factura.getTotalIva() > 0) {
                double tasaCalculada = factura.getTotalIva() / factura.getSubtotal12();
                if (Math.abs(tasaCalculada - 0.12) < 0.01) {
                    codigoPorcentajeIva = "2"; // 12%
                    tarifaIva = "12";
                } else if (Math.abs(tasaCalculada - 0.15) < 0.01) {
                    codigoPorcentajeIva = "4"; // 15%
                    tarifaIva = "15";
                } else if (Math.abs(tasaCalculada - 0.13) < 0.01) {
                    codigoPorcentajeIva = "10"; // 13% IVA Diferenciado
                    tarifaIva = "13";
                } else if (Math.abs(tasaCalculada - 0.05) < 0.01) {
                    codigoPorcentajeIva = "5"; // 5% Materiales Construcción
                    tarifaIva = "5";
                }
            }

            // IVA General (usando subtotal12 como base gravada general)
            if (factura.getSubtotal12() > 0) {
                totalConImpuestos.appendChild(crearTotalImpuesto(doc, "2", codigoPorcentajeIva, factura.getSubtotal12(),
                        factura.getTotalIva()));
            }
            // IVA 0%
            if (factura.getSubtotal0() > 0) {
                totalConImpuestos.appendChild(crearTotalImpuesto(doc, "2", "0", factura.getSubtotal0(), 0.0));
            }
            // IVA No Objeto (6)
            if (factura.getSubtotalNoObjeto() > 0) {
                totalConImpuestos.appendChild(crearTotalImpuesto(doc, "2", "6", factura.getSubtotalNoObjeto(), 0.0));
            }
            // IVA Exento (7)
            if (factura.getSubtotalExento() > 0) {
                totalConImpuestos.appendChild(crearTotalImpuesto(doc, "2", "7", factura.getSubtotalExento(), 0.0));
            }

            infoFac.appendChild(add(doc, "propina", "0.00"));
            infoFac.appendChild(
                    add(doc, "importeTotal", String.format(java.util.Locale.US, "%.2f", factura.getTotalFactura())));
            infoFac.appendChild(add(doc, "moneda", "DOLAR"));

            // Pagos
            Element pagos = doc.createElement("pagos");
            infoFac.appendChild(pagos);

            List<FacturaPago> listaPagos = facturaPagoRepository.findByFactura(factura);
            if (listaPagos != null && !listaPagos.isEmpty()) {
                for (FacturaPago fp : listaPagos) {
                    Element pago = doc.createElement("pago");
                    String codigoFormaPago = fp.getFormaPago() != null && fp.getFormaPago().getCodigoSri() != null
                            ? fp.getFormaPago().getCodigoSri()
                            : "01";

                    pago.appendChild(add(doc, "formaPago", codigoFormaPago));
                    pago.appendChild(add(doc, "total", String.format(java.util.Locale.US, "%.2f", fp.getTotal())));
                    if (!"01".equals(codigoFormaPago)) {
                        pago.appendChild(
                                add(doc, "plazo", fp.getPlazo() != null ? String.valueOf(fp.getPlazo()) : "0"));
                        pago.appendChild(
                                add(doc, "unidadTiempo", fp.getUnidadTiempo() != null ? fp.getUnidadTiempo() : "dias"));
                    }
                    pagos.appendChild(pago);
                }
            } else {
                Element pago = doc.createElement("pago");
                pago.appendChild(add(doc, "formaPago", "01")); // Efectivo
                pago.appendChild(
                        add(doc, "total", String.format(java.util.Locale.US, "%.2f", factura.getTotalFactura())));
                pagos.appendChild(pago);
            }

            // 3. Detalles
            Element detallesEl = doc.createElement("detalles");
            facturaEl.appendChild(detallesEl);

            for (DetalleFactura det : factura.getDetalles()) {
                Element d = doc.createElement("detalle");
                String codigoPrincipal = det.getProducto().getProductoSerial();
                if (codigoPrincipal == null || codigoPrincipal.isEmpty()) {
                    codigoPrincipal = String.valueOf(det.getProducto().getProductoId());
                }

                d.appendChild(add(doc, "codigoPrincipal", codigoPrincipal));
                addOptional(doc, d, "codigoAuxiliar", det.getProducto().getProductoId());
                d.appendChild(add(doc, "descripcion", det.getProducto().getProductoNombre()));
                d.appendChild(
                        add(doc, "cantidad", String.format(java.util.Locale.US, "%.2f", (double) det.getCantidad())));
                d.appendChild(add(doc, "precioUnitario",
                        String.format(java.util.Locale.US, "%.2f", det.getPrecioUnitario())));
                d.appendChild(add(doc, "descuento", String.format(java.util.Locale.US, "%.2f", det.getDescuento())));
                d.appendChild(add(doc, "precioTotalSinImpuesto",
                        String.format(java.util.Locale.US, "%.2f", det.getSubtotal())));

                Element impuestosDet = doc.createElement("impuestos");
                d.appendChild(impuestosDet);

                List<ImpuestoDetalle> impDetalles = impuestoRepository.findByDetalleFactura(det);

                if (impDetalles != null && !impDetalles.isEmpty()) {
                    for (ImpuestoDetalle imp : impDetalles) {
                        Element impuesto = doc.createElement("impuesto");
                        impuesto.appendChild(add(doc, "codigo", imp.getCodigo()));
                        impuesto.appendChild(add(doc, "codigoPorcentaje", imp.getCodigoPorcentaje()));
                        impuesto.appendChild(add(doc, "tarifa", imp.getTarifa()));
                        impuesto.appendChild(add(doc, "baseImponible",
                                String.format(java.util.Locale.US, "%.2f", imp.getBaseImponible())));
                        impuesto.appendChild(
                                add(doc, "valor", String.format(java.util.Locale.US, "%.2f", imp.getValor())));
                        impuestosDet.appendChild(impuesto);
                    }
                } else {
                    // Fallback logic
                    Element impuesto = doc.createElement("impuesto");
                    impuesto.appendChild(add(doc, "codigo", "2")); // IVA

                    // Usar la misma lógica de tasa que arriba
                    boolean tieneIva = det.getSubtotal() > 0
                            && (det.getProducto().getProductoTasa() != null && det.getProducto().getProductoTasa() > 0);

                    if (tieneIva) {
                        impuesto.appendChild(add(doc, "codigoPorcentaje", codigoPorcentajeIva));
                        impuesto.appendChild(add(doc, "tarifa", tarifaIva));
                        double valIva = det.getSubtotal() * (Double.parseDouble(tarifaIva) / 100.0);
                        impuesto.appendChild(add(doc, "baseImponible",
                                String.format(java.util.Locale.US, "%.2f", det.getSubtotal())));
                        impuesto.appendChild(add(doc, "valor", String.format(java.util.Locale.US, "%.2f", valIva)));
                    } else {
                        impuesto.appendChild(add(doc, "codigoPorcentaje", "0"));
                        impuesto.appendChild(add(doc, "tarifa", "0"));
                        impuesto.appendChild(add(doc, "baseImponible",
                                String.format(java.util.Locale.US, "%.2f", det.getSubtotal())));
                        impuesto.appendChild(add(doc, "valor", "0.00"));
                    }
                    impuestosDet.appendChild(impuesto);
                }
                detallesEl.appendChild(d);
            }

            // 4. Info Adicional
            Element infoAdicional = doc.createElement("infoAdicional");
            boolean tieneInfo = false;

            if (factura.getCliente().getClienteEmail() != null && !factura.getCliente().getClienteEmail().isEmpty()) {
                infoAdicional.appendChild(crearCampoAdicional(doc, "Email", factura.getCliente().getClienteEmail()));
                tieneInfo = true;
            }
            if (factura.getCliente().getClienteTelefono() != null
                    && !factura.getCliente().getClienteTelefono().isEmpty()) {
                infoAdicional
                        .appendChild(crearCampoAdicional(doc, "Telefono", factura.getCliente().getClienteTelefono()));
                tieneInfo = true;
            }
            // Agregamos Dirección aquí si existe, ya que es común ponerla en infoAdicional
            // si no se usa el campo estricto xml 1.0.0
            // Pero según manual 1.0.0, direccionComprador va en infoFactura. Lo hemos
            // puesto arriba.
            // Dejamos esto como extra info si se desea.

            if (tieneInfo) {
                facturaEl.appendChild(infoAdicional);
            }

            File carpeta = new File("C:\\facturaSRI");
            if (!carpeta.exists())
                carpeta.mkdirs();
            String ruta = "C:\\facturaSRI\\factura_" + factura.getSecuencial() + ".xml";
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(doc), new StreamResult(new File(ruta)));
            return ruta;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al generar XML: " + e.getMessage());
        }
    }

    private Element crearTotalImpuesto(Document doc, String codigo, String codigoPorcentaje, Double base,
            Double valor) {
        Element totalImpuesto = doc.createElement("totalImpuesto");
        totalImpuesto.appendChild(add(doc, "codigo", codigo));
        totalImpuesto.appendChild(add(doc, "codigoPorcentaje", codigoPorcentaje));
        totalImpuesto.appendChild(add(doc, "baseImponible", String.format("%.2f", base).replace(",", ".")));
        totalImpuesto.appendChild(add(doc, "valor", String.format("%.2f", valor).replace(",", ".")));
        return totalImpuesto;
    }

    private Element crearCampoAdicional(Document doc, String nombre, String valor) {
        Element campo = doc.createElement("campoAdicional");
        campo.setAttribute("nombre", nombre);
        campo.appendChild(doc.createTextNode(valor));
        return campo;
    }

    private String getTipoIdentificacion(String identificacion) {
        if (identificacion == null)
            return "07"; // Consumidor Final
        if (identificacion.equals("9999999999999"))
            return "07";
        if (identificacion.length() == 10)
            return "05"; // Cédula
        if (identificacion.length() == 13)
            return "04"; // RUC
        return "06"; // Pasaporte / Otros
    }

    private Element add(Document doc, String tag, Object value) {
        Element e = doc.createElement(tag);
        e.appendChild(doc.createTextNode(value != null ? String.valueOf(value) : ""));
        return e;
    }

    private void addOptional(Document doc, Element parent, String tagName, Object value) {
        if (value != null && !String.valueOf(value).trim().isEmpty()) {
            parent.appendChild(add(doc, tagName, value));
        }
    }

    public Factura buscarPorId(Long id) {
        return facturaRepository.findById(id).orElse(null);
    }

    public Factura guardar(Factura factura) {
        return facturaRepository.save(factura);
    }

    public List<Factura> listarAll() {
        return facturaRepository.findAll();
    }

    @Transactional
    public void anularFactura(Long id) {
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada"));

        // No eliminamos, cambiamos estado a ANULADA (4)
        // 4 = ANULADA
        factura.setEstado(4);
        factura.setEstadoSri("ANULADA");
        factura.setMensajeSri("Factura anulada por el usuario");

        // Devolver stock
        for (DetalleFactura det : factura.getDetalles()) {
            Producto p = det.getProducto();
            p.setProductoStock(p.getProductoStock() + det.getCantidad().intValue());
            productoRepository.save(p);
        }

        facturaRepository.save(factura);
    }

}