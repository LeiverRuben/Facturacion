package facturacion.facturacion.Servicios;

import facturacion.facturacion.Dto.GuiaRemisionDTO;
import facturacion.facturacion.Entidades.*;
import facturacion.facturacion.Repositorios.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GuiaRemisionServicio {

    private final GuiaDeRemisionRepositorio guiaRepositorio;
    private final EmpresaRepositorio empresaRepositorio;
    private final FirmaElectronicaServicio firmaService;
    private final SriServicio sriService;

    private static final String FOLDER_SRI = "C:\\facturaSRI";

    @Transactional
    public GuiaDeRemision crearGuia(GuiaRemisionDTO dto) {
        Empresa empresa = empresaRepositorio.findAll().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("No hay empresa configurada."));

        GuiaDeRemision guia = new GuiaDeRemision();
        guia.setEmpresa(empresa);
        guia.setSecuencial(generarSecuencial());
        guia.setFechaEmision(LocalDateTime.now());
        guia.setEstado(1);
        guia.setEstadoSri("PENDIENTE");

        // Info Transportista (Snapshot)
        guia.setTransportistaIdentificacion(dto.getTransportistaIdentificacion());
        guia.setTransportistaRazonSocial(dto.getTransportistaRazonSocial());
        guia.setPlaca(dto.getPlaca());
        guia.setDirPartida(dto.getDirPartida());
        guia.setFechaIniTransporte(dto.getFechaIniTransporte());
        guia.setFechaFinTransporte(dto.getFechaFinTransporte());

        guia = guiaRepositorio.save(guia);

        List<DestinatarioGuia> destinatarios = new ArrayList<>();
        if (dto.getDestinatarios() != null) {
            for (GuiaRemisionDTO.DestinatarioDTO destDTO : dto.getDestinatarios()) {
                DestinatarioGuia dest = new DestinatarioGuia();
                dest.setGuiaDeRemision(guia);
                dest.setIdentificacionDestinatario(destDTO.getIdentificacionDestinatario());
                dest.setRazonSocialDestinatario(destDTO.getRazonSocialDestinatario());
                dest.setDirDestinatario(destDTO.getDirDestinatario());
                dest.setMotivoTraslado(destDTO.getMotivoTraslado());
                dest.setRuta(destDTO.getRuta());

                // Doc Sustento
                dest.setCodDocSustento(destDTO.getCodDocSustento());
                dest.setNumDocSustento(destDTO.getNumDocSustento());
                dest.setNumAutDocSustento(destDTO.getNumAutDocSustento());

                List<DetalleGuia> detalles = new ArrayList<>();
                if (destDTO.getDetalles() != null) {
                    for (GuiaRemisionDTO.DetalleGuiaDTO detDTO : destDTO.getDetalles()) {
                        DetalleGuia det = new DetalleGuia();
                        det.setDestinatario(dest);
                        det.setCodigoInterno(detDTO.getCodigoInterno());
                        det.setDescripcion(detDTO.getDescripcion());
                        det.setCantidad(detDTO.getCantidad());
                        detalles.add(det);
                    }
                }
                dest.setDetalles(detalles);
                destinatarios.add(dest);
            }
        }
        guia.setDestinatarios(destinatarios);

        return guiaRepositorio.save(guia);
    }

    private String generarSecuencial() {
        long count = guiaRepositorio.count() + 1;
        return String.format("%09d", count);
    }

    @Transactional
    public GuiaDeRemision enviarSri(Long id) {
        GuiaDeRemision guia = guiaRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Guía no encontrada"));

        if ("AUTORIZADO".equals(guia.getEstadoSri())) {
            throw new RuntimeException("La guía ya está AUTORIZADA");
        }

        try {
            if (guia.getClaveAcceso() == null || guia.getClaveAcceso().isEmpty()) {
                guia.setClaveAcceso(generarClaveAcceso(guia));
                guiaRepositorio.save(guia);
            }

            String xmlPath = generarXMLGuia(guia);

            // Firma
            String p12Path = guia.getEmpresa().getRutaFirma();
            String p12Pass = guia.getEmpresa().getClaveFirma();
            if (p12Path == null || p12Pass == null || "PENDIENTE".equals(p12Path)) {
                throw new RuntimeException("ERROR CONF: Configure firma electrónica en Empresa.");
            }

            String signedXmlPath = firmaService.firmarXML(xmlPath, p12Path, p12Pass);
            byte[] signedXmlBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(signedXmlPath));

            // Envio
            String responseRecepcion = sriService.enviarComprobante(signedXmlBytes);
            String estadoRecepcion = sriService.extraerEstado(responseRecepcion);

            if ("DEVUELTA".equals(estadoRecepcion)) {
                String mensaje = sriService.extraerMensaje(responseRecepcion);
                if (mensaje.contains("PROCESAMIENTO") || responseRecepcion.contains("PROCESAMIENTO")) {
                    guia.setEstadoSri("EN_PROCESO");
                    String fullMsg = "En procesamiento SRI... " + mensaje;
                    guia.setMensajeSri(fullMsg.length() > 250 ? fullMsg.substring(0, 250) : fullMsg);
                } else {
                    guia.setEstadoSri("DEVUELTA");
                    guia.setMensajeSri(mensaje != null && mensaje.length() > 250 ? mensaje.substring(0, 250) : mensaje);
                }
                return guiaRepositorio.save(guia);
            }

            // Autorizacion
            String responseAuth = sriService.autorizarComprobante(guia.getClaveAcceso());
            String estadoAuth = sriService.extraerEstado(responseAuth);

            if (estadoAuth == null || estadoAuth.trim().isEmpty()) {
                estadoAuth = "ERROR_RESPUESTA";
            }

            guia.setEstadoSri(estadoAuth);
            if ("AUTORIZADO".equals(estadoAuth)) {
                guia.setFechaAutorizacion(LocalDateTime.now());
                guia.setMensajeSri("Autorizado Ok");
            } else {
                String msg = sriService.extraerMensaje(responseAuth);
                if (responseAuth.contains("PROCESAMIENTO") || (msg != null && msg.contains("PROCESAMIENTO"))) {
                    guia.setEstadoSri("EN_PROCESO");
                }
                String finalMsg = msg != null ? msg : responseAuth;
                guia.setMensajeSri(finalMsg.length() > 250 ? finalMsg.substring(0, 250) : finalMsg);
            }

            return guiaRepositorio.save(guia);

        } catch (Exception e) {
            e.printStackTrace();
            guia.setEstadoSri("ERROR");
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Error desconocido";
            guia.setMensajeSri(errorMsg.length() > 250 ? errorMsg.substring(0, 250) : errorMsg);
            return guiaRepositorio.save(guia);
        }
    }

    public String generarClaveAcceso(GuiaDeRemision guia) {
        String fecha = guia.getFechaEmision().toLocalDate().format(DateTimeFormatter.ofPattern("ddMMyyyy"));
        String tipoComprobante = "06"; // 06 = Guía de Remisión
        String ruc = guia.getEmpresa().getRuc();
        String ambiente = String.valueOf(guia.getEmpresa().getAmbiente());
        String estab = guia.getEmpresa().getEstablecimiento();
        String ptoEmi = guia.getEmpresa().getPuntoEmision();
        String secuencial = guia.getSecuencial();
        String codigoNumerico = String.format("%08d", (int) (Math.random() * 99999999));
        String tipoEmision = "1";

        String claveSinDV = fecha + tipoComprobante + ruc + ambiente + estab + ptoEmi + secuencial + codigoNumerico
                + tipoEmision;
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

    public String generarXMLGuia(GuiaDeRemision guia) throws Exception {
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document doc = builder.newDocument();

        org.w3c.dom.Element root = doc.createElement("guiaRemision");
        root.setAttribute("id", "comprobante");
        root.setAttribute("version", "1.1.0"); // Versión actual
        doc.appendChild(root);

        // InfoTributaria
        org.w3c.dom.Element infoTrib = doc.createElement("infoTributaria");
        root.appendChild(infoTrib);
        infoTrib.appendChild(add(doc, "ambiente", guia.getEmpresa().getAmbiente()));
        infoTrib.appendChild(add(doc, "tipoEmision", "1"));
        infoTrib.appendChild(add(doc, "razonSocial", guia.getEmpresa().getRazonSocial()));
        addOptional(doc, infoTrib, "nombreComercial", guia.getEmpresa().getNombreComercial());
        infoTrib.appendChild(add(doc, "ruc", guia.getEmpresa().getRuc()));
        infoTrib.appendChild(add(doc, "claveAcceso", guia.getClaveAcceso()));
        infoTrib.appendChild(add(doc, "codDoc", "06"));
        infoTrib.appendChild(add(doc, "estab", guia.getEmpresa().getEstablecimiento()));
        infoTrib.appendChild(add(doc, "ptoEmi", guia.getEmpresa().getPuntoEmision()));
        infoTrib.appendChild(add(doc, "secuencial", guia.getSecuencial()));
        infoTrib.appendChild(add(doc, "dirMatriz", guia.getEmpresa().getDirEstablecimiento()));

        // InfoGuiaRemision
        org.w3c.dom.Element infoGuia = doc.createElement("infoGuiaRemision");
        root.appendChild(infoGuia);

        String dirEst = guia.getEmpresa().getDirEstablecimiento();
        if (dirEst == null || dirEst.trim().isEmpty()) {
            dirEst = guia.getEmpresa().getDirMatriz();
        }
        if (dirEst != null && !dirEst.trim().isEmpty()) {
            infoGuia.appendChild(add(doc, "dirEstablecimiento", dirEst));
        }
        infoGuia.appendChild(add(doc, "dirPartida", guia.getDirPartida()));
        infoGuia.appendChild(add(doc, "razonSocialTransportista", guia.getTransportistaRazonSocial()));

        infoGuia.appendChild(add(doc, "tipoIdentificacionTransportista",
                getTipoIdentificacion(guia.getTransportistaIdentificacion())));
        infoGuia.appendChild(add(doc, "rucTransportista", guia.getTransportistaIdentificacion()));
        infoGuia.appendChild(add(doc, "placa", guia.getPlaca()));
        infoGuia.appendChild(add(doc, "fechaIniTransporte",
                guia.getFechaIniTransporte().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        infoGuia.appendChild(add(doc, "fechaFinTransporte",
                guia.getFechaFinTransporte().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));

        infoGuia.appendChild(add(doc, "obligadoContabilidad",
                guia.getEmpresa().getObligadoContabilidad() != null ? guia.getEmpresa().getObligadoContabilidad()
                        : "NO"));

        // Destinatarios
        org.w3c.dom.Element destinatarios = doc.createElement("destinatarios");
        root.appendChild(destinatarios);

        for (DestinatarioGuia dest : guia.getDestinatarios()) {
            org.w3c.dom.Element d = doc.createElement("destinatario");
            d.appendChild(add(doc, "identificacionDestinatario", dest.getIdentificacionDestinatario()));
            d.appendChild(add(doc, "razonSocialDestinatario", dest.getRazonSocialDestinatario()));
            d.appendChild(add(doc, "dirDestinatario", dest.getDirDestinatario()));
            d.appendChild(add(doc, "motivoTraslado", dest.getMotivoTraslado()));

            // Doc Sustento si existe
            if (dest.getNumDocSustento() != null && !dest.getNumDocSustento().isEmpty()) {
                d.appendChild(
                        add(doc, "codDocSustento", dest.getCodDocSustento() != null ? dest.getCodDocSustento() : "01"));
                d.appendChild(add(doc, "numDocSustento", formatDocNum(dest.getNumDocSustento())));
                if (dest.getNumAutDocSustento() != null) {
                    d.appendChild(add(doc, "numAutDocSustento", dest.getNumAutDocSustento()));
                }
                // Fecha Emision Doc Sustento? Se requiere si hay doc sustento
                // Asumimos HOY si falta, o deberíamos guardarlo.
                // Por simplicidad, usaremos la fecha de creación de la guía si es nulo, PERO el
                // SRI valida esto.
                // MODIFICACIÓN: En un caso real se debe guardar fecha sustento.
                // Pongo fecha guía por ahora para evitar NullPointer
                d.appendChild(add(doc, "fechaEmisionDocSustento",
                        guia.getFechaEmision().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
            }

            d.appendChild(add(doc, "ruta", dest.getRuta()));

            org.w3c.dom.Element detalles = doc.createElement("detalles");
            d.appendChild(detalles);

            for (DetalleGuia det : dest.getDetalles()) {
                org.w3c.dom.Element dl = doc.createElement("detalle");
                dl.appendChild(add(doc, "codigoInterno", det.getCodigoInterno()));
                dl.appendChild(add(doc, "descripcion", det.getDescripcion()));
                dl.appendChild(add(doc, "cantidad", String.format(java.util.Locale.US, "%.2f", det.getCantidad())));
                detalles.appendChild(dl);
            }

            destinatarios.appendChild(d);
        }

        // Save
        File carpeta = new File(FOLDER_SRI);
        if (!carpeta.exists())
            carpeta.mkdirs();
        String ruta = FOLDER_SRI + "\\guia_" + guia.getSecuencial() + ".xml";

        javax.xml.transform.Transformer transformer = javax.xml.transform.TransformerFactory.newInstance()
                .newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.transform(new javax.xml.transform.dom.DOMSource(doc),
                new javax.xml.transform.stream.StreamResult(new File(ruta)));

        return ruta;
    }

    private String getTipoIdentificacion(String id) {
        if (id == null)
            return "07";
        if (id.length() == 10)
            return "05";
        if (id.length() == 13)
            return "04";
        return "06";
    }

    private String formatDocNum(String num) {
        String rawNum = num.replace("-", "");
        if (!rawNum.matches("\\d+"))
            rawNum = rawNum.replaceAll("\\D", "");
        String paddedNum = String.format("%15s", rawNum).replace(' ', '0');
        if (paddedNum.length() > 15)
            paddedNum = paddedNum.substring(paddedNum.length() - 15);
        return paddedNum;
    }

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
}
