package facturacion.facturacion.Servicios;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.io.File;

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
            // Documento Sustento (La Compra)
            det.setCodDocSustento("01"); // Factura

            // Correction: Ensure 15 digits for SRI (numDocSustento)
            String rawNum = compra.getNumeroComprobante() != null ? compra.getNumeroComprobante().replace("-", "")
                    : "0";
            if (!rawNum.matches("\\d+")) {
                rawNum = rawNum.replaceAll("\\D", ""); // Remove non-digits
            }
            if (rawNum.isEmpty())
                rawNum = "0";

            // Pad Left with zeros to 15 digits
            String paddedNum = String.format("%15s", rawNum).replace(' ', '0');
            if (paddedNum.length() > 15)
                paddedNum = paddedNum.substring(paddedNum.length() - 15);

            det.setNumDocSustento(paddedNum);
            det.setFechaEmisionDocSustento(compra.getFechaEmision().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            detalleRepositorio.save(det);
            detalles.add(det);
        }

        retencion.setImpuestos(detalles);
        return retencion;
    }

    // Services
    private final FirmaElectronicaServicio firmaService;
    private final SriServicio sriService;

    // XML Generation Imports
    private static final String FOLDER_SRI = "C:\\facturaSRI"; // Centralizar configuración si es posible

    public String generarClaveAcceso(ComprobanteRetencion retencion) {
        String fecha = retencion.getFechaEmision().toLocalDate()
                .format(DateTimeFormatter.ofPattern("ddMMyyyy"));
        String tipoComprobante = "07"; // 07 = Retención
        String ruc = retencion.getEmpresa().getRuc();
        String ambiente = String.valueOf(retencion.getEmpresa().getAmbiente());
        String estab = retencion.getEmpresa().getEstablecimiento();
        String ptoEmi = retencion.getEmpresa().getPuntoEmision();
        String secuencial = retencion.getSecuencial();
        String codigoNumerico = generarCodigoNumerico();
        String tipoEmision = "1"; // Normal

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

    @Transactional
    public ComprobanteRetencion enviarSri(Long id) {
        ComprobanteRetencion retencion = retencionRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Retención no encontrada"));

        if ("AUTORIZADO".equals(retencion.getEstadoSri())) {
            throw new RuntimeException("El comprobante ya está AUTORIZADO");
        }

        try {
            // 1. Generar Clave Acceso si no tiene
            if (retencion.getClaveAcceso() == null || retencion.getClaveAcceso().isEmpty()) {
                retencion.setClaveAcceso(generarClaveAcceso(retencion));
                retencionRepositorio.save(retencion);
            }

            // 2. Generar XML
            String xmlPath = generarXMLRetencion(retencion);

            // 3. Firmar XML
            // Recueperar ruta firma y clave de empresa (Asumimos que están en entidad
            // Empresa)
            String p12Path = retencion.getEmpresa().getRutaFirma();
            String p12Pass = retencion.getEmpresa().getClaveFirma();

            if (p12Path == null || p12Pass == null || p12Path.trim().isEmpty() || "PENDIENTE".equals(p12Path)) {
                throw new RuntimeException(
                        "ERROR CONF: Debe configurar la Firma Electrónica en el menú 'Empresa' antes de enviar.");
            }

            String signedXmlPath = firmaService.firmarXML(xmlPath, p12Path, p12Pass);
            byte[] signedXmlBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(signedXmlPath));

            // 4. Enviar a Recepción SRI
            String responseRecepcion = sriService.enviarComprobante(signedXmlBytes);
            String estadoRecepcion = sriService.extraerEstado(responseRecepcion);

            if ("DEVUELTA".equals(estadoRecepcion)) {
                String mensaje = sriService.extraerMensaje(responseRecepcion);
                if (mensaje.contains("PROCESAMIENTO") || responseRecepcion.contains("PROCESAMIENTO")) {
                    retencion.setEstadoSri("EN_PROCESO");
                    String fullMsg = "El comprobante está siendo procesado por el SRI. Intente consultar nuevamente en unos minutos. "
                            + mensaje;
                    retencion.setMensajeSri(fullMsg.length() > 250 ? fullMsg.substring(0, 250) : fullMsg);
                } else {
                    retencion.setEstadoSri("DEVUELTA");
                    retencion.setMensajeSri(
                            mensaje != null && mensaje.length() > 250 ? mensaje.substring(0, 250) : mensaje);
                }
                return retencionRepositorio.save(retencion);
            }

            // 5. Solicitar Autorización (si fue RECIBIDA)
            String responseAuth = sriService.autorizarComprobante(retencion.getClaveAcceso());
            String estadoAuth = sriService.extraerEstado(responseAuth);

            // Safety check
            if (estadoAuth == null || estadoAuth.trim().isEmpty()) {
                estadoAuth = "ERROR_RESPUESTA";
            }

            retencion.setEstadoSri(estadoAuth); // AUTORIZADO, NO AUTORIZADO, EN PROCESO

            if ("AUTORIZADO".equals(estadoAuth)) {
                retencion.setFechaAutorizacion(LocalDateTime.now());
                retencion.setMensajeSri("Autorizado correctamente");
            } else {
                String msg = sriService.extraerMensaje(responseAuth);
                if (responseAuth.contains("PROCESAMIENTO") || (msg != null && msg.contains("PROCESAMIENTO"))) {
                    retencion.setEstadoSri("EN_PROCESO");
                }
                String finalMsg = msg != null ? msg : responseAuth;
                retencion.setMensajeSri(finalMsg.length() > 250 ? finalMsg.substring(0, 250) : finalMsg);
            }

            return retencionRepositorio.save(retencion);

        } catch (Exception e) {
            e.printStackTrace();
            retencion.setEstadoSri("ERROR");
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Error desconocido";
            retencion.setMensajeSri(errorMsg.length() > 250 ? errorMsg.substring(0, 250) : errorMsg);
            return retencionRepositorio.save(retencion);
        }
    }

    public String generarXMLRetencion(ComprobanteRetencion retencion) throws Exception {
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document doc = builder.newDocument();

        org.w3c.dom.Element root = doc.createElement("comprobanteRetencion");
        root.setAttribute("id", "comprobante");
        root.setAttribute("version", "1.0.0");
        doc.appendChild(root);

        // InfoTributaria
        org.w3c.dom.Element infoTrib = doc.createElement("infoTributaria");
        root.appendChild(infoTrib);
        infoTrib.appendChild(add(doc, "ambiente", retencion.getEmpresa().getAmbiente()));
        infoTrib.appendChild(add(doc, "tipoEmision", "1"));
        infoTrib.appendChild(add(doc, "razonSocial", retencion.getEmpresa().getRazonSocial()));
        addOptional(doc, infoTrib, "nombreComercial", retencion.getEmpresa().getNombreComercial());
        infoTrib.appendChild(add(doc, "ruc", retencion.getEmpresa().getRuc()));
        infoTrib.appendChild(add(doc, "claveAcceso", retencion.getClaveAcceso()));
        infoTrib.appendChild(add(doc, "codDoc", "07")); // 07 Retencion
        infoTrib.appendChild(add(doc, "estab", retencion.getEmpresa().getEstablecimiento()));
        infoTrib.appendChild(add(doc, "ptoEmi", retencion.getEmpresa().getPuntoEmision()));
        infoTrib.appendChild(add(doc, "secuencial", retencion.getSecuencial()));
        infoTrib.appendChild(add(doc, "dirMatriz", retencion.getEmpresa().getDirEstablecimiento()));

        // InfoCompRetencion
        org.w3c.dom.Element infoRet = doc.createElement("infoCompRetencion");
        root.appendChild(infoRet);
        infoRet.appendChild(add(doc, "fechaEmision",
                retencion.getFechaEmision().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        addOptional(doc, infoRet, "dirEstablecimiento", retencion.getEmpresa().getDirEstablecimiento());
        addOptional(doc, infoRet, "contribuyenteEspecial", retencion.getEmpresa().getContribuyenteEspecial());
        infoRet.appendChild(add(doc, "obligadoContabilidad",
                retencion.getEmpresa().getObligadoContabilidad() != null
                        ? retencion.getEmpresa().getObligadoContabilidad()
                        : "NO"));

        infoRet.appendChild(
                add(doc, "tipoIdentificacionSujetoRetenido", getTipoIdentificacion(retencion.getProveedor().getRuc())));
        infoRet.appendChild(add(doc, "razonSocialSujetoRetenido", retencion.getProveedor().getRazonSocial()));
        infoRet.appendChild(add(doc, "identificacionSujetoRetenido", retencion.getProveedor().getRuc()));
        infoRet.appendChild(add(doc, "periodoFiscal", retencion.getPeriodoFiscal()));

        // Impuestos (Detalles)
        org.w3c.dom.Element impuestos = doc.createElement("impuestos");
        root.appendChild(impuestos);

        for (DetalleRetencion det : retencion.getImpuestos()) {
            org.w3c.dom.Element imp = doc.createElement("impuesto");
            imp.appendChild(add(doc, "codigo", det.getCodigo())); // 1=RENTA, 2=IVA
            imp.appendChild(add(doc, "codigoRetencion", det.getCodigoRetencion()));
            imp.appendChild(
                    add(doc, "baseImponible", String.format(java.util.Locale.US, "%.2f", det.getBaseImponible())));
            imp.appendChild(add(doc, "porcentajeRetener",
                    String.format(java.util.Locale.US, "%.2f", det.getPorcentajeRetener())));
            imp.appendChild(
                    add(doc, "valorRetenido", String.format(java.util.Locale.US, "%.2f", det.getValorRetenido())));
            imp.appendChild(add(doc, "codDocSustento", det.getCodDocSustento()));

            // Fix on the fly for XML generation (Ensure 15 digits)
            String rawNum = det.getNumDocSustento() != null ? det.getNumDocSustento() : "0";
            if (!rawNum.matches("\\d+"))
                rawNum = rawNum.replaceAll("\\D", "");
            String paddedNum = String.format("%15s", rawNum).replace(' ', '0');
            if (paddedNum.length() > 15)
                paddedNum = paddedNum.substring(paddedNum.length() - 15);

            imp.appendChild(add(doc, "numDocSustento", paddedNum));
            imp.appendChild(add(doc, "fechaEmisionDocSustento", det.getFechaEmisionDocSustento()));
            impuestos.appendChild(imp);
        }

        // Additional Info
        org.w3c.dom.Element infoAdicional = doc.createElement("infoAdicional");
        boolean hasInfo = false;
        if (retencion.getProveedor().getEmail() != null) {
            infoAdicional.appendChild(crearCampoAdicional(doc, "Email", retencion.getProveedor().getEmail()));
            hasInfo = true;
        }
        if (hasInfo)
            root.appendChild(infoAdicional);

        // Save File
        File carpeta = new File(FOLDER_SRI);
        if (!carpeta.exists())
            carpeta.mkdirs();
        String ruta = FOLDER_SRI + "\\retencion_" + retencion.getSecuencial() + ".xml";

        javax.xml.transform.Transformer transformer = javax.xml.transform.TransformerFactory.newInstance()
                .newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.transform(new javax.xml.transform.dom.DOMSource(doc),
                new javax.xml.transform.stream.StreamResult(new File(ruta)));

        return ruta;
    }

    private String generarSecuencial() {
        long count = retencionRepositorio.count() + 1;
        return String.format("%09d", count);
    }

    // Helpers (Reused from Factura logic)
    private org.w3c.dom.Element add(org.w3c.dom.Document doc, String tag, Object value) {
        org.w3c.dom.Element e = doc.createElement(tag);
        e.appendChild(doc.createTextNode(value != null ? String.valueOf(value) : ""));
        return e;
    }

    private void addOptional(org.w3c.dom.Document doc, org.w3c.dom.Element parent, String tagName, Object value) {
        if (value != null && !String.valueOf(value).trim().isEmpty()) {
            parent.appendChild(add(doc, tagName, value));
        }
    }

    private org.w3c.dom.Element crearCampoAdicional(org.w3c.dom.Document doc, String nombre, String valor) {
        org.w3c.dom.Element campo = doc.createElement("campoAdicional");
        campo.setAttribute("nombre", nombre);
        campo.appendChild(doc.createTextNode(valor));
        return campo;
    }

    private String getTipoIdentificacion(String identificacion) {
        if (identificacion == null)
            return "07";
        if (identificacion.length() == 10)
            return "05";
        if (identificacion.length() == 13)
            return "04";
        return "06";
    }

    public List<ComprobanteRetencion> listarRetenciones() {
        return retencionRepositorio.findAll();
    }
}
