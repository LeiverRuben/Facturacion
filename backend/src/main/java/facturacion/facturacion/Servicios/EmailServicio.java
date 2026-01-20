package facturacion.facturacion.Servicios;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailServicio {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remitente;

    public void enviarFacturaAutorizada(String destinatario, String numeroFactura, byte[] pdfBytes, byte[] xmlBytes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // multipart=true para adjuntos
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject("Comprobante Electrónico | Factura #" + numeroFactura);

            String cuerpo = String.format("""
                    Estimado/a cliente,

                    Su factura electrónica #%s ha sido AUTORIZADA por el SRI.

                    Adjunto encontrará los archivos PDF y XML correspondientes.

                    Gracias por su preferencia.
                    """, numeroFactura);

            helper.setText(cuerpo);

            // Adjuntar PDF
            if (pdfBytes != null) {
                helper.addAttachment("Factura_" + numeroFactura + ".pdf", new ByteArrayResource(pdfBytes));
            }

            // Adjuntar XML
            if (xmlBytes != null) {
                helper.addAttachment("Factura_" + numeroFactura + ".xml", new ByteArrayResource(xmlBytes));
            }

            mailSender.send(message);
            System.out.println("Email enviado exitosamente a: " + destinatario);

        } catch (MessagingException e) {
            System.err.println("Error al enviar email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
