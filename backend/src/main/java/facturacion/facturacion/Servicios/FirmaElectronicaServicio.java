package facturacion.facturacion.Servicios;

import org.apache.xml.security.Init;

import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.File;
import java.io.FileInputStream;

import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Service
public class FirmaElectronicaServicio {

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        Init.init();
    }

    private static final String XADES_1_3_2_NS = "http://uri.etsi.org/01903/v1.3.2#";

    public String firmarXML(String xmlPath, String p12Path, String password) {
        try {
            // 1. Validar existencia del archivo P12
            File p12File = new File(p12Path);
            if (!p12File.exists()) {
                throw new RuntimeException("Archivo de firma no encontrado en: " + p12Path);
            }

            // Si es un directorio, buscar el primer archivo .p12 o .pfx
            if (p12File.isDirectory()) {
                File[] files = p12File.listFiles(
                        (dir, name) -> name.toLowerCase().endsWith(".p12") || name.toLowerCase().endsWith(".pfx"));
                if (files != null && files.length > 0) {
                    p12File = files[0];
                    System.out.println("INFO: Usando archivo de firma encontrado: " + p12File.getAbsolutePath());
                } else {
                    throw new RuntimeException("No se encontraron archivos .p12 / .pfx en el directorio: " + p12Path);
                }
            }

            // 2. Cargar KeyStore
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(p12File)) {
                ks.load(fis, password.toCharArray());
            }

            String alias = ks.aliases().nextElement();
            PrivateKey privateKey = (PrivateKey) ks.getKey(alias, password.toCharArray());
            X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

            // 3. Cargar documento XML
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().parse(new File(xmlPath));

            // 4. Crear firma XML
            // Usamos RSA_SHA1 por compatibilidad SRI común, aunque SHA256 es preferible si
            // el SRI lo soporta completamente ahora.
            // Para asegurar compatibilidad histórica usamos RSA_SHA1 en el algoritmo de
            // firma.
            String baseURI = new File(xmlPath).toURI().toString();
            XMLSignature firma = new XMLSignature(doc, baseURI, XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1);

            Element root = doc.getDocumentElement();
            // IMPORTANTE PARA SRI: Identificar el atributo 'id' como ID real para que la
            // referencia # funcione
            if (root.hasAttribute("id")) {
                root.setIdAttribute("id", true);
            } else {
                // Si por alguna razón no tiene id, se lo ponemos (fallback)
                root.setAttribute("id", "comprobante");
                root.setIdAttribute("id", true);
            }

            root.appendChild(firma.getElement());

            // 5. Transformaciones (Enveloped Signature)
            Transforms transforms = new Transforms(doc);
            transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
            // Canonicalization Exclusive Omit Comments es estándar para evitar problemas de
            // namespaces
            transforms.addTransform(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);

            // CAMBIO CLAVE: Referenciar explícitamente al ID del comprobante en lugar de
            // todo el documento ("")
            firma.addDocument("#comprobante", transforms, Constants.ALGO_ID_DIGEST_SHA1);

            // 6. Añadir KeyInfo (Certificado y Clave Pública)
            firma.addKeyInfo(cert);
            firma.addKeyInfo(cert.getPublicKey());

            // 7. Construir estructura XAdES (QualifyingProperties)
            Element qualifyingProperties = createQualifyingProperties(doc, cert, firma.getId());

            // Añadir el Object con las propiedades XAdES
            firma.appendObject(new org.apache.xml.security.signature.ObjectContainer(doc));
            // Esta librería de Apache es antigua, a veces appendObject es complejo.
            // Simplificación: añadimos el objeto manualmente al elemento Signature
            Element objectElement = doc.createElementNS(Constants.SignatureSpecNS, "ds:Object");
            objectElement.appendChild(qualifyingProperties);
            firma.getElement().appendChild(objectElement);

            // Referenciar el objeto en la firma para que sea firmado (SignedProperties)
            // IMPORTANTE: El ID de los SignedProperties debe ser referenciado
            String signedPropertiesId = "SignedProperties-" + UUID.randomUUID().toString();
            // Buscar el nodo SignedProperties recien creado para ponerle ID
            Element signedProps = (Element) qualifyingProperties
                    .getElementsByTagNameNS(XADES_1_3_2_NS, "SignedProperties").item(0);
            signedProps.setAttribute("Id", signedPropertiesId);
            signedProps.setIdAttribute("Id", true);

            // Añadir referencia a SignedProperties (Type es obligatorio para XAdES)
            // Usamos SHA1 para el digest de la referencia también
            firma.addDocument("#" + signedPropertiesId, null, Constants.ALGO_ID_DIGEST_SHA1, null,
                    "http://uri.etsi.org/01903#SignedProperties");

            // 8. Firmar
            firma.sign(privateKey);

            // 9. Guardar XML firmado
            String xmlFirmado = xmlPath.replace(".xml", "_firmado.xml");
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(new File(xmlFirmado)));

            return xmlFirmado;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error grave firmando XML (XAdES): " + e.getMessage(), e);
        }
    }

    private Element createQualifyingProperties(Document doc, X509Certificate cert, String signatureId)
            throws Exception {
        Element qualifyingProperties = doc.createElementNS(XADES_1_3_2_NS, "etsi:QualifyingProperties");
        // qualifyingProperties.setAttribute("Target", "#" + signatureId); // Target
        // apunta a la Signature ID, a veces es null si no se asigna ID a la firma

        Element signedProperties = doc.createElementNS(XADES_1_3_2_NS, "etsi:SignedProperties");
        qualifyingProperties.appendChild(signedProperties);

        Element signedSignatureProperties = doc.createElementNS(XADES_1_3_2_NS, "etsi:SignedSignatureProperties");
        signedProperties.appendChild(signedSignatureProperties);

        // SigningTime
        Element signingTime = doc.createElementNS(XADES_1_3_2_NS, "etsi:SigningTime");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        signingTime.setTextContent(sdf.format(new Date())); // Hora local, SRI acepta local o UTC
        signedSignatureProperties.appendChild(signingTime);

        // SigningCertificate
        Element signingCertificate = doc.createElementNS(XADES_1_3_2_NS, "etsi:SigningCertificate");
        signedSignatureProperties.appendChild(signingCertificate);

        Element certTag = doc.createElementNS(XADES_1_3_2_NS, "etsi:Cert");
        signingCertificate.appendChild(certTag);

        Element certDigest = doc.createElementNS(XADES_1_3_2_NS, "etsi:CertDigest");
        certTag.appendChild(certDigest);

        // DigestMethod
        Element digestMethod = doc.createElementNS(Constants.SignatureSpecNS, "ds:DigestMethod");
        digestMethod.setAttribute("Algorithm", Constants.ALGO_ID_DIGEST_SHA1);
        certDigest.appendChild(digestMethod);

        // DigestValue - Hash del certificado
        Element digestValue = doc.createElementNS(Constants.SignatureSpecNS, "ds:DigestValue");
        digestValue.setTextContent(calculateCertDigest(cert));
        certDigest.appendChild(digestValue);

        // IssuerSerial
        Element issuerSerial = doc.createElementNS(XADES_1_3_2_NS, "etsi:IssuerSerial");
        certTag.appendChild(issuerSerial);

        Element x509IssuerName = doc.createElementNS(Constants.SignatureSpecNS, "ds:X509IssuerName");
        // FIX: SRI / XAdES often requires RFC 2253 format for Issuer Name in
        // IssuerSerial to match certificate bytes
        x509IssuerName
                .setTextContent(cert.getIssuerX500Principal().getName(javax.security.auth.x500.X500Principal.RFC2253));
        issuerSerial.appendChild(x509IssuerName);

        Element x509SerialNumber = doc.createElementNS(Constants.SignatureSpecNS, "ds:X509SerialNumber");
        x509SerialNumber.setTextContent(cert.getSerialNumber().toString());
        issuerSerial.appendChild(x509SerialNumber);

        return qualifyingProperties;
    }

    private String calculateCertDigest(X509Certificate cert) throws Exception {
        // Calcular SHA-1 del certificado DER
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(cert.getEncoded());
        return java.util.Base64.getEncoder().encodeToString(digest);
    }

    public String verificarFirma(String p12Path, String password) {
        try {
            File p12File = new File(p12Path);
            if (!p12File.exists())
                return "ERROR: Archivo no encontrado en: " + p12Path;

            // Support for directories (auto-find .p12)
            if (p12File.isDirectory()) {
                File[] files = p12File.listFiles(
                        (dir, name) -> name.toLowerCase().endsWith(".p12") || name.toLowerCase().endsWith(".pfx"));
                if (files != null && files.length > 0) {
                    p12File = files[0];
                } else {
                    return "ERROR: No se encontró ningún archivo .p12 o .pfx en la carpeta: " + p12Path;
                }
            }

            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(p12File)) {
                ks.load(fis, password.toCharArray());
            } catch (java.io.IOException e) {
                if (e.getMessage() != null && e.getMessage().contains("password")) {
                    return "ERROR: La contraseña es INCORRECTA.";
                }
                return "ERROR: No se pudo abrir el archivo (Corrupto o no es un .p12 válido).";
            } catch (Exception e) {
                return "ERROR: Contraseña incorrecta o archivo corrupto.";
            }

            String alias = ks.aliases().nextElement();
            X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            Date now = new Date();
            long daysLeft = (cert.getNotAfter().getTime() - now.getTime()) / (1000 * 60 * 60 * 24);

            return String.format("OK: Certificado válido.\nEmisor: %s\nExpira: %s (%d días restantes)\nSujeto: %s",
                    cert.getIssuerX500Principal().getName(),
                    sdf.format(cert.getNotAfter()),
                    daysLeft,
                    cert.getSubjectX500Principal().getName());

        } catch (Exception e) {
            return "ERROR General: " + e.getMessage();
        }
    }
}