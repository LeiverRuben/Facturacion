package facturacion.facturacion.Servicios;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
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

    // Endpoints de Pruebas Offline
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
                "         <claveAcceso>" + claveAcceso + "</claveAcceso>" +
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

            return response.toString();

        } catch (Exception e) {
            System.err.println("Error en SOAP " + accion + ": " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }

    // Método auxiliar para extraer estado de la respuesta XML (Parsing básico con
    // Regex para evitar complejidad DOM)
    public String extraerEstado(String xmlResponse) {
        try {
            Pattern pattern = Pattern.compile("<estado>(.*?)</estado>");
            Matcher matcher = pattern.matcher(xmlResponse);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return "DESCONOCIDO";
        } catch (Exception e) {
            return "ERROR_PARSING";
        }
    }

    // Método auxiliar para extraer mensajes de error (Información adicional)
    public String extraerMensaje(String xmlResponse) {
        try {
            // Intentar capturar mensaje y tipo
            Pattern pIdentificador = Pattern.compile("<identificador>(.*?)</identificador>");
            Pattern pMensaje = Pattern.compile("<mensaje>(.*?)</mensaje>");
            Pattern pInfo = Pattern.compile("<informacionAdicional>(.*?)</informacionAdicional>");

            Matcher mId = pIdentificador.matcher(xmlResponse);
            Matcher mMsg = pMensaje.matcher(xmlResponse);
            Matcher mInfo = pInfo.matcher(xmlResponse);

            boolean esErrorEstructura = false;
            StringBuilder sbMsg = new StringBuilder();
            StringBuilder sbInfo = new StringBuilder();

            while (mMsg.find()) {
                String msg = mMsg.group(1);
                if (msg.contains("ARCHIVO NO CUMPLE ESTRUCTURA XML")) {
                    esErrorEstructura = true;
                }
                sbMsg.append(msg).append("; ");
            }
            while (mInfo.find()) {
                sbInfo.append(mInfo.group(1)).append("; ");
            }

            // Si es un "falso" error de estructura (tiene detalle de datos), mostramos solo
            // el detalle
            if (esErrorEstructura && sbInfo.length() > 0) {
                return "Error de Datos: " + sbInfo.toString();
            }

            // Si no, devolvemos todo
            String resultado = sbMsg.toString() + (sbInfo.length() > 0 ? " Detalle: " + sbInfo.toString() : "");
            return resultado.isEmpty() ? "Respuesta XML sin mensajes claros de error." : resultado;
        } catch (Exception e) {
            return "Error parseando respuesta SRI";
        }
    }
}
