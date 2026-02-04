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

        // Initialize with temporary 0s to allow saving
        factura.setSubtotal12(0.0);
        factura.setSubtotal0(0.0);
        factura.setTotalDescuento(0.0);
        factura.setTotalIva(0.0);
        factura.setTotalFactura(0.0);
        factura.setCliente(cliente);
        factura.setEmpresa(empresa);
        factura.setSesionCaja(sesionCaja);
        factura.setEstado(1);
        factura.setEstadoSri("PENDIENTE");

        // SAVE FACTURA FIRST to generate ID
        factura = facturaRepository.save(factura);
        // Initializing accumulators
        double calcSubtotal12 = 0.0;
        double calcSubtotal0 = 0.0;
        double calcSubtotalNoObjeto = 0.0;
        double calcSubtotalExento = 0.0;
        double calcTotalDescuento = 0.0;
        double calcTotalIva = 0.0;

        List<DetalleFactura> detalles = new ArrayList<>();

        // Process details first to calculate totals
        for (DetalleFacturaDTO detDTO : request.getDetalles()) {
            Producto producto = productoRepository.findById(detDTO.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado."));

            DetalleFactura det = new DetalleFactura();
            det.setFactura(factura);
            det.setProducto(producto);
            det.setCantidad(detDTO.getCantidad());

            // SECURITY: We could force DB price here, but allowing frontend price if edits
            // are allowed.
            // verifying math:
            double precio = detDTO.getPrecioUnitario();
            double cantidad = detDTO.getCantidad();
            double descuento = detDTO.getDescuento();

            // Recalculate Subtotal line
            double subtotalLinea = (precio * cantidad) - descuento;
            det.setPrecioUnitario(precio);
            det.setDescuento(descuento);
            det.setSubtotal(subtotalLinea);

            // Calculate Taxes for this item
            // Logic: Check product tax rate
            double tasaIva = (producto.getProductoTasa() != null) ? producto.getProductoTasa() : 0.0;
            boolean pagaIva = tasaIva > 0;

            if (pagaIva) {
                // AUTO-UPDATE: Force 12% to 15% for SRI compliance (2025/2026)
                if (Math.abs(tasaIva - 12.0) < 0.1) {
                    tasaIva = 15.0;
                }

                calcSubtotal12 += subtotalLinea;
                // Calculate IVA for this item
                double ivaItem = subtotalLinea * (tasaIva / 100.0);
                calcTotalIva += ivaItem;
            } else {
                calcSubtotal0 += subtotalLinea;
            }
            calcTotalDescuento += descuento;

            // Prepare Validation/Persistence
            DetalleFactura detalleGuardado = detalleRepository.save(det);

            // Create Tax Detail (ImpuestoDetalle)
            ImpuestoDetalle imp = new ImpuestoDetalle();
            imp.setDetalleFactura(detalleGuardado);
            imp.setCodigo("2"); // IVA
            // Map rate to SRI code
            if (pagaIva) {
                if (Math.abs(tasaIva - 12.0) < 0.1) {
                    imp.setCodigoPorcentaje(2.0);
                    imp.setTarifa(12.0);
                } else if (Math.abs(tasaIva - 15.0) < 0.1) {
                    imp.setCodigoPorcentaje(4.0);
                    imp.setTarifa(15.0);
                } else {
                    imp.setCodigoPorcentaje(4.0);
                    imp.setTarifa(15.0); // Fallback
                }
                imp.setBaseImponible(subtotalLinea);
                imp.setValor(subtotalLinea * (tasaIva / 100.0));
            } else {
                imp.setCodigoPorcentaje(0.0);
                imp.setTarifa(0.0);
                imp.setBaseImponible(subtotalLinea);
                imp.setValor(0.0);
            }

            impuestoRepository.save(imp);
            detalles.add(det);
        }

        // Final Rounding for Header
        // Using BigDecimal logic via String format to ensure 2 decimal precision
        // matches what SRI expects
        factura.setSubtotal12(Double.parseDouble(String.format(java.util.Locale.US, "%.2f", calcSubtotal12)));
        factura.setSubtotal0(Double.parseDouble(String.format(java.util.Locale.US, "%.2f", calcSubtotal0)));
        factura.setSubtotalNoObjeto(0.0);
        factura.setSubtotalExento(0.0);
        factura.setTotalDescuento(Double.parseDouble(String.format(java.util.Locale.US, "%.2f", calcTotalDescuento)));
        factura.setTotalIva(Double.parseDouble(String.format(java.util.Locale.US, "%.2f", calcTotalIva)));

        double totalFinal = factura.getSubtotal12() + factura.getSubtotal0() + factura.getTotalIva();
        factura.setTotalFactura(Double.parseDouble(String.format(java.util.Locale.US, "%.2f", totalFinal)));

        // Save Factura with authoritative totals
        factura = facturaRepository.save(factura);
        for (PagoDTO pdto : request.getPagos()) {
            FormaPago fpago = formaPagoRepository.findById(pdto.getMetodoPagoId())
                    .orElseThrow(() -> new RuntimeException("Forma de pago no encontrada."));
            FacturaPago fp = new FacturaPago();
            fp.setFactura(factura);
            fp.setFormaPago(fpago);
            // CORRECCION: Ajustar el pago al total recalculado para evitar discrepancias
            // SRI
            fp.setTotal(factura.getTotalFactura());
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
            addOptional(doc, infoFac, "guiaRemision", null);
            infoFac.appendChild(add(doc, "razonSocialComprador",
                    factura.getCliente().getClienteNombre() + " " + factura.getCliente().getClienteApellido()));
            infoFac.appendChild(add(doc, "identificacionComprador", ident));
            addOptional(doc, infoFac, "direccionComprador", factura.getCliente().getClienteDireccion());

            // --- RECALCULATE TOTALS FROM PROCESSED DETAILS TO AVOID ROUNDING ERRORS ---
            double calcTotalSinImpuestos = 0.0;
            double calcTotalDescuento = 0.0;
            double calcTotalIva = 0.0;
            double calcTotal0 = 0.0;
            double calcTotalNoObj = 0.0;
            double calcTotalExento = 0.0;
            double calcSubtotal12 = 0.0; // Base Imponible Gravada (puede ser 15% ahora)

            // Rates detection
            String detectedCodigoPorcel = "4"; // Default 15
            String detectedTarifa = "15";

            for (DetalleFactura det : factura.getDetalles()) {
                double rawSubtotal = det.getSubtotal(); // (Cant * Precio) - Desc
                double rawDescuento = det.getDescuento();

                // Sumar descuentos
                calcTotalDescuento += rawDescuento;

                // Corrected Header Calculation: Use actual stored taxes, not product reference
                List<ImpuestoDetalle> storedTaxes = impuestoRepository.findByDetalleFactura(det);

                boolean hasStoredTax = false;
                if (storedTaxes != null && !storedTaxes.isEmpty()) {
                    for (ImpuestoDetalle imp : storedTaxes) {
                        double tarifa = imp.getTarifa();
                        double valor = imp.getValor();
                        String codigo = imp.getCodigo();

                        // AUTO-MIGRATE 12% -> 15% for XML Header (Legacy Fix)
                        if ("2".equals(codigo) && Math.abs(tarifa - 12.0) < 0.1) {
                            tarifa = 15.0; // Force 15
                            valor = imp.getBaseImponible() * 0.15; // Recalculate Value
                        }

                        if ("2".equals(codigo) && tarifa > 0) {
                            // It's IVA 12% (patched) or 15%
                            hasStoredTax = true;
                            calcSubtotal12 += imp.getBaseImponible();
                            calcTotalIva += valor; // Use patched value

                            // Capture rate (Always prefer 15 if patched)
                            if (Math.abs(tarifa - 12.0) < 0.1) {
                                detectedCodigoPorcel = "2";
                                detectedTarifa = "12";
                            } else {
                                detectedCodigoPorcel = "4";
                                detectedTarifa = "15";
                            }
                        }
                    }
                }

                // If found tax in DB, we already added to calcSubtotal12.
                // Any base NOT added to 12 should go to 0?
                // Simpler: If the item has ANY tax, the whole subtotal is 12% base?
                // Actually, ImpuestoDetalle stores the base.
                // What if it's 0% tax? Stored tax has tarifa 0.

                // Fallback if no stored taxes (legacy/error) or if stored tax was 0%
                if (!hasStoredTax) {
                    // If we didn't find a POSITIVE tax record, we assume 0%
                    // Check if valid 0% record exists?
                    // For now, if no positive tax, add to Total0
                    calcTotal0 += rawSubtotal;
                }

                // Remove the old logic that queried det.getProducto()
                /*
                 * boolean tieneIva = rawSubtotal > 0 && det.getProducto().getProductoTasa() !=
                 * null
                 * && det.getProducto().getProductoTasa() > 0;
                 * if (tieneIva) { ... } else { ... }
                 */
                calcTotalSinImpuestos += rawSubtotal;
            }

            // Apply rounding to match XML fields
            double finalTotalSinImpuestos = Double
                    .parseDouble(String.format(java.util.Locale.US, "%.2f", calcTotalSinImpuestos));
            double finalTotalDescuento = Double
                    .parseDouble(String.format(java.util.Locale.US, "%.2f", calcTotalDescuento));
            double finalValorIva = Double.parseDouble(String.format(java.util.Locale.US, "%.2f", calcTotalIva));
            double finalImporteTotal = finalTotalSinImpuestos + finalValorIva; // Sum of bases + iva

            infoFac.appendChild(
                    add(doc, "totalSinImpuestos", String.format(java.util.Locale.US, "%.2f", finalTotalSinImpuestos)));
            infoFac.appendChild(
                    add(doc, "totalDescuento", String.format(java.util.Locale.US, "%.2f", finalTotalDescuento)));

            // Bloque TotalConImpuestos
            Element totalConImpuestos = doc.createElement("totalConImpuestos");
            infoFac.appendChild(totalConImpuestos);

            // Add Taxes
            if (calcSubtotal12 > 0) {
                // Use Recalculated IVA
                totalConImpuestos
                        .appendChild(crearTotalImpuesto(doc, "2", detectedCodigoPorcel, calcSubtotal12, finalValorIva));
            }
            if (calcTotal0 > 0) {
                totalConImpuestos.appendChild(crearTotalImpuesto(doc, "2", "0", calcTotal0, 0.0));
            }

            infoFac.appendChild(add(doc, "propina", "0.00"));
            infoFac.appendChild(
                    add(doc, "importeTotal", String.format(java.util.Locale.US, "%.2f", finalImporteTotal)));
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

                    if ("99".equals(codigoFormaPago)) {
                        codigoFormaPago = "20"; // Mapear Crédito interno a Otros con utilización del sistema financiero
                    }

                    pago.appendChild(add(doc, "formaPago", codigoFormaPago));

                    // PATCH PAYMENT TOTAL: Must match finalImporteTotal (Legacy Fix)
                    // If we only have 1 payment, force it to match the new total
                    if (listaPagos.size() == 1) {
                        pago.appendChild(
                                add(doc, "total", String.format(java.util.Locale.US, "%.2f", finalImporteTotal)));
                    } else {
                        // If multiple payments, this is risky. Use stored but warn.
                        // But usually it's 1 payment.
                        pago.appendChild(add(doc, "total", String.format(java.util.Locale.US, "%.2f", fp.getTotal())));
                    }

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
                        add(doc, "total", String.format(java.util.Locale.US, "%.2f", finalImporteTotal))); // Use NEW
                                                                                                           // total
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

                        double tarifaXML = imp.getTarifa();
                        double valorXML = imp.getValor();
                        String codPorcelXML = imp.getCodigoPorcentaje() != null
                                ? String.valueOf(imp.getCodigoPorcentaje().intValue())
                                : "0";

                        // AUTO-PATCH 12% -> 15% in DETAIL (Legacy Fix)
                        if (Math.abs(tarifaXML - 12.0) < 0.1) {
                            tarifaXML = 15.0;
                            valorXML = imp.getBaseImponible() * 0.15;
                            codPorcelXML = "4"; // Code for 15%
                        }

                        impuesto.appendChild(add(doc, "codigo", imp.getCodigo()));
                        impuesto.appendChild(add(doc, "codigoPorcentaje", codPorcelXML));
                        impuesto.appendChild(add(doc, "tarifa", String.format(java.util.Locale.US, "%.1f", tarifaXML))); // Format
                                                                                                                         // to
                                                                                                                         // 1
                                                                                                                         // decimal
                        impuesto.appendChild(add(doc, "baseImponible",
                                String.format(java.util.Locale.US, "%.2f", imp.getBaseImponible())));
                        impuesto.appendChild(
                                add(doc, "valor", String.format(java.util.Locale.US, "%.2f", valorXML)));
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
                        impuesto.appendChild(add(doc, "codigoPorcentaje",
                                detectedCodigoPorcel.contains(".") ? detectedCodigoPorcel.split("\\.")[0]
                                        : detectedCodigoPorcel));
                        impuesto.appendChild(add(doc, "tarifa", detectedTarifa));
                        double valIva = det.getSubtotal() * (Double.parseDouble(detectedTarifa) / 100.0);
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