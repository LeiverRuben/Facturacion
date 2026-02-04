package facturacion.facturacion.Servicios;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailServicio {

    @Autowired
    private facturacion.facturacion.Repositorios.EmpresaRepositorio empresaRepositorio;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:}")
    private String defaultUsername;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.password:}")
    private String defaultPassword;

    public void enviarNotificacionFactura(String destinatario, String numeroFactura, byte[] pdfBytes, byte[] xmlBytes,
            String estadoSri, String mensajeSri) {
        try {
            // 1. Obtener Credenciales de Empresa (ID 1)
            facturacion.facturacion.Entidades.Empresa empresa = empresaRepositorio.findById(1L).orElse(null);

            String finalUsername = (empresa != null && empresa.getCorreoRemitente() != null
                    && !empresa.getCorreoRemitente().isEmpty())
                            ? empresa.getCorreoRemitente()
                            : defaultUsername;
            String finalPassword = (empresa != null && empresa.getClaveCorreo() != null
                    && !empresa.getClaveCorreo().isEmpty())
                            ? empresa.getClaveCorreo()
                            : defaultPassword;

            if (finalUsername == null || finalUsername.isEmpty() || finalPassword == null || finalPassword.isEmpty()) {
                throw new RuntimeException(
                        "Debe configurar el correo emisor en la pantalla de Empresa (Ajustes) o en application.properties.");
            }

            // 2. Configurar Sender
            org.springframework.mail.javamail.JavaMailSenderImpl mailSender = new org.springframework.mail.javamail.JavaMailSenderImpl();
            String host = "smtp.gmail.com";
            if (finalUsername.contains("@outlook") || finalUsername.contains("@hotmail")) {
                host = "smtp.office365.com";
            }
            mailSender.setHost(host);
            mailSender.setPort(587);
            mailSender.setUsername(finalUsername);
            mailSender.setPassword(finalPassword);

            java.util.Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            // 3. Crear Mensaje
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(finalUsername);
            helper.setTo(destinatario);
            helper.setSubject("Comprobante Electrónico | Factura #" + numeroFactura + " - " + estadoSri);

            String cuerpo;
            if ("AUTORIZADO".equals(estadoSri)) {
                cuerpo = String.format("""
                        Estimado/a cliente,

                        Su factura electrónica #%s ha sido PROCESADA por el SRI.
                        Estado: %s.

                        Adjuntamos su comprobante en formato PDF y XML.

                        Atentamente,
                        %s
                        """, numeroFactura, estadoSri,
                        (empresa != null ? empresa.getRazonSocial() : "Sistema de Facturación"));
            } else {
                cuerpo = String.format("""
                        Estimado/a cliente,

                        Su factura electrónica #%s ha sido procesada por el SRI.

                        Estado Actual: %s
                        Detalle: %s

                        Adjuntamos la documentación disponible.

                        Atentamente,
                        %s
                        """, numeroFactura, estadoSri, mensajeSri,
                        (empresa != null ? empresa.getRazonSocial() : "Sistema de Facturación"));
            }

            helper.setText(cuerpo);

            if (pdfBytes != null) {
                helper.addAttachment("Factura_" + numeroFactura + ".pdf", new ByteArrayResource(pdfBytes));
            }
            if (xmlBytes != null) {
                helper.addAttachment("Factura_" + numeroFactura + ".xml", new ByteArrayResource(xmlBytes));
            }

            mailSender.send(message);
            System.out.println(
                    "Email enviado exitosamente a: " + destinatario + " desde " + finalUsername);

        } catch (Exception e) {
            System.err.println("Error al enviar email: " + e.getMessage());
            throw new RuntimeException("Error en envío: " + e.getMessage());
        }
    }
}
