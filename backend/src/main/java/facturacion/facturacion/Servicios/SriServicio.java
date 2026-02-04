package facturacion.facturacion.Servicios;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SriServicio {

    private static final String URL_RECEPCION = "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/RecepcionComprobantesOffline";
    private static final String URL_AUTORIZACION = "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline";

    public String enviarComprobante(byte[] xmlFirmadoBytes) {
        String xmlBase64 = Base64.getEncoder().encodeToString(xmlFirmadoBytes);
        String soapEnvelope = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ec=\"http://ec.gob.sri.ws.recepcion\">"
                +
                "   <soapenv:Header/>" +
                "   <soapenv:Body>" +
                "      <ec:validarComprobante>" +
                "         <xml>" + xmlBase64 + "</xml>" +
                "      </ec:validarComprobante>" +
                "   </soapenv:Body>" +
                "</soapenv:Envelope>";

        return enviarSoap(URL_RECEPCION, soapEnvelope, "validarComprobante");
    }

    public String autorizarComprobante(String claveAcceso) {
        String soapEnvelope = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ec=\"http://ec.gob.sri.ws.autorizacion\">"
                +
                "   <soapenv:Header/>" +
                "   <soapenv:Body>" +
                "      <ec:autorizacionComprobante>" +
                "         <claveAccesoComprobante>" + claveAcceso + "</claveAccesoComprobante>" +
                "      </ec:autorizacionComprobante>" +
                "   </soapenv:Body>" +
                "</soapenv:Envelope>";

        return enviarSoap(URL_AUTORIZACION, soapEnvelope, "autorizacionComprobante");
    }

    private String enviarSoap(String urlEndpoint, String soapXml, String accion) {
        try {
            URL url = new URL(urlEndpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");

            // Enviar Request
            try (OutputStream os = conn.getOutputStream()) {
                os.write(soapXml.getBytes(StandardCharsets.UTF_8));
            }

            // Leer Response
            int responseCode = conn.getResponseCode();
            StringBuilder response = new StringBuilder();

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader((responseCode == 200) ? conn.getInputStream() : conn.getErrorStream(),
                            StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            // DEBUG EXTREMO: Imprimir toda la respuesta para ver errores ocultos
            System.out.println("================================");
            System.out.println("SRI RAW RESPONSE (" + accion + "):");
            System.out.println(response.toString());
            System.out.println("================================");

            return response.toString();

        } catch (Exception e) {
            System.err.println("Error en SOAP " + accion + ": " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }

    // Método auxiliar para extraer estado de la respuesta XML
    public String extraerEstado(String xmlResponse) {
        if (xmlResponse == null || xmlResponse.isEmpty())
            return "ERROR_CONEXION";

        try {
            // PRIORIDAD 1: Chequear si está EN PROCESAMIENTO (independiente del tag estado)
            if (xmlResponse.contains("CLAVE DE ACCESO EN PROCESAMIENTO")) {
                return "EN_PROCESO";
            }

            // Intentar primero con el tag estándar <estado>
            Pattern pattern = Pattern.compile("<estado>(.*?)</estado>");
            Matcher matcher = pattern.matcher(xmlResponse);
            if (matcher.find()) {
                return matcher.group(1);
            }

            // Si no hay estado pero hay mensajes de error del SRI (ej: Clave no encontrada)
            if (xmlResponse.contains("<mensaje>")) {
                if (xmlResponse.contains("CLAVE DE ACCESO NO REGISTRADA"))
                    return "NO_REGISTRADA";
                if (xmlResponse.contains("IDENTIFICADOR DE COMPROBANTE NO VALIDO"))
                    return "ERROR_IDENTIFICADOR";
            }

            // Si es un error de conexión SOAP (no es XML válido del SRI)
            if (xmlResponse.startsWith("ERROR:")) {
                return "ERROR_COMUNICACION";
            }

            return "DESCONOCIDO";
        } catch (Exception e) {
            return "ERROR_PARSING";
        }
    }

    // Método auxiliar para extraer mensajes de error (Información adicional)
    public String extraerMensaje(String xmlResponse) {
        if (xmlResponse == null || xmlResponse.isEmpty())
            return "No se recibió respuesta del SRI";

        try {
            // Caso especial: Errores SOAP o de transporte
            if (xmlResponse.startsWith("ERROR:"))
                return xmlResponse;

            // Intentar capturar mensaje y tipo
            Pattern pMensaje = Pattern.compile("<mensaje>(.*?)</mensaje>");
            Pattern pInfo = Pattern.compile("<informacionAdicional>(.*?)</informacionAdicional>");

            Matcher mMsg = pMensaje.matcher(xmlResponse);
            Matcher mInfo = pInfo.matcher(xmlResponse);

            boolean esErrorEstructura = false;
            StringBuilder sbMsg = new StringBuilder();
            StringBuilder sbInfo = new StringBuilder();

            while (mMsg.find()) {
                String msg = mMsg.group(1).replaceAll("<[^>]+>", "").trim(); // Strip tags
                if (msg.contains("ARCHIVO NO CUMPLE ESTRUCTURA XML")) {
                    esErrorEstructura = true;
                }
                sbMsg.append(msg).append("; ");
            }
            while (mInfo.find()) {
                String info = mInfo.group(1).replaceAll("<[^>]+>", "").trim(); // Strip tags
                sbInfo.append(info).append("; ");
            }

            if (sbMsg.length() == 0 && !xmlResponse.contains("<estado>")) {
                if (xmlResponse.contains("faultstring")) {
                    Pattern pFault = Pattern.compile("<faultstring>(.*?)</faultstring>");
                    Matcher mFault = pFault.matcher(xmlResponse);
                    if (mFault.find())
                        return "Error SRI (SOAP): " + mFault.group(1);
                }
                return "Respuesta SRI (Sin estado): " + xmlResponse.substring(0, Math.min(xmlResponse.length(), 200));
            }

            // Construir mensaje final con Detalle Técnico COMPLETO
            String resultado = sbMsg.toString();
            if (sbInfo.length() > 0) {
                resultado += " Detalle: " + sbInfo.toString();
            }

            // Limpieza final
            resultado = resultado.replace(";;", ";").trim();
            if (resultado.endsWith(";"))
                resultado = resultado.substring(0, resultado.length() - 1);

            // Si no se encontraron mensajes de error específicos
            if (resultado.isEmpty()) {
                // Intentar obtener el estado como fallback
                Pattern pEstado = Pattern.compile("<estado>(.*?)</estado>");
                Matcher mEstado = pEstado.matcher(xmlResponse);
                if (mEstado.find()) {
                    return "Estado: " + mEstado.group(1); // "Estado: AUTORIZADO" o "Estado: EN PROCESO"
                }
                return "SRI: Error desconocido o Respuesta sin Mensajes (ver logs)";
            }

            return resultado;

        } catch (Exception e) {
            return "Error parseando respuesta SRI";
        }
    }
}
