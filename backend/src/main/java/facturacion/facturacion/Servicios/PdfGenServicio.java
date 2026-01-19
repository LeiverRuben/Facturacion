package facturacion.facturacion.Servicios;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import facturacion.facturacion.Entidades.ComprobanteRetencion;
import facturacion.facturacion.Entidades.DetalleRetencion;
import facturacion.facturacion.Entidades.Factura;
import facturacion.facturacion.Entidades.DetalleFactura;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGenServicio {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ======================================
    // GENERAR PDF DE RETENCIÓN
    // ======================================
    public byte[] generarPdfRetencion(ComprobanteRetencion retencion) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // 1. Encabezado (Empresa)
            agregarEncabezadoEmpresa(document, retencion.getEmpresa());

            // 2. Título Documento
            Paragraph titulo = new Paragraph("COMPROBANTE DE RETENCIÓN",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16));
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(20);
            document.add(titulo);

            // 3. Info del Comprobante
            PdfPTable tablaInfo = new PdfPTable(2);
            tablaInfo.setWidthPercentage(100);
            tablaInfo.setSpacingAfter(20);

            agregarCelda(tablaInfo, "No. Comprobante:", retencion.getSecuencial());
            agregarCelda(tablaInfo, "Estado SRI:", retencion.getEstadoSri());
            agregarCelda(tablaInfo, "Fecha Emisión:", retencion.getFechaEmision().format(formatter));
            if (retencion.getFechaAutorizacion() != null) {
                agregarCelda(tablaInfo, "Fecha Autorización:", retencion.getFechaAutorizacion().format(formatter));
            }
            agregarCelda(tablaInfo, "Clave Acceso:", retencion.getClaveAcceso());
            document.add(tablaInfo);

            // 4. Info del Proveedor (Sujeto Retenido)
            Paragraph subtituloProv = new Paragraph("INFORMACIÓN DEL SUJETO RETENIDO",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            subtituloProv.setSpacingAfter(10);
            document.add(subtituloProv);

            PdfPTable tablaProv = new PdfPTable(2);
            tablaProv.setWidthPercentage(100);
            tablaProv.setSpacingAfter(20);
            agregarCelda(tablaProv, "Razón Social:", retencion.getProveedor().getRazonSocial());
            agregarCelda(tablaProv, "RUC:", retencion.getProveedor().getRuc());
            agregarCelda(tablaProv, "Email:", retencion.getProveedor().getEmail());
            agregarCelda(tablaProv, "Periodo Fiscal:", retencion.getPeriodoFiscal());
            document.add(tablaProv);

            // 5. Detalles de Retención
            Paragraph subtituloDet = new Paragraph("DETALLE DE LA RETENCIÓN",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            subtituloDet.setSpacingAfter(10);
            document.add(subtituloDet);

            PdfPTable tablaDetalles = new PdfPTable(5);
            tablaDetalles.setWidthPercentage(100);
            tablaDetalles.setWidths(new float[] { 2, 2, 2, 2, 2 });

            // Cabeceras
            agregarCeldaHeader(tablaDetalles, "Impuesto");
            agregarCeldaHeader(tablaDetalles, "Base Imponible");
            agregarCeldaHeader(tablaDetalles, "Cod. Retención");
            agregarCeldaHeader(tablaDetalles, "% Porcentaje");
            agregarCeldaHeader(tablaDetalles, "Valor Retenido");

            for (DetalleRetencion det : retencion.getImpuestos()) {
                String nombreImpuesto = det.getCodigo().equals("1") ? "RENTA"
                        : (det.getCodigo().equals("2") ? "IVA" : "ISD");

                tablaDetalles.addCell(
                        new PdfPCell(new Phrase(nombreImpuesto, FontFactory.getFont(FontFactory.HELVETICA, 10))));
                tablaDetalles.addCell(new PdfPCell(new Phrase("$ " + String.format("%.2f", det.getBaseImponible()),
                        FontFactory.getFont(FontFactory.HELVETICA, 10))));
                tablaDetalles.addCell(new PdfPCell(
                        new Phrase(det.getCodigoRetencion(), FontFactory.getFont(FontFactory.HELVETICA, 10))));
                tablaDetalles.addCell(new PdfPCell(
                        new Phrase(det.getPorcentajeRetener() + " %", FontFactory.getFont(FontFactory.HELVETICA, 10))));
                tablaDetalles.addCell(new PdfPCell(new Phrase("$ " + String.format("%.2f", det.getValorRetenido()),
                        FontFactory.getFont(FontFactory.HELVETICA, 10))));
            }
            document.add(tablaDetalles);

            document.close();
        } catch (Exception e) {
            throw new IOException("Error creating PDF", e);
        }

        return baos.toByteArray();
    }

    // ======================================
    // GENERAR PDF DE FACTURA
    // ======================================
    public byte[] generarPdfFactura(Factura factura) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // 1. Encabezado (Empresa)
            agregarEncabezadoEmpresa(document, factura.getEmpresa());

            // 2. Título Documento
            Paragraph titulo = new Paragraph("FACTURA", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16));
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(20);
            document.add(titulo);

            // 3. Info Factura -> Cliente
            PdfPTable tablaInfo = new PdfPTable(2);
            tablaInfo.setWidthPercentage(100);
            tablaInfo.setSpacingAfter(20);

            agregarCelda(tablaInfo, "No. Factura:", factura.getSecuencial());
            agregarCelda(tablaInfo, "Fecha Emisión:", factura.getFechaEmision().format(formatter));
            if (factura.getCliente() != null) {
                agregarCelda(tablaInfo, "Cliente:",
                        factura.getCliente().getClienteNombre() + " " + factura.getCliente().getClienteApellido());
                agregarCelda(tablaInfo, "RUC/CI:", ""); // Cliente entity might need CI field check or use generics
            }
            agregarCelda(tablaInfo, "Estado SRI:", factura.getEstadoSri());
            agregarCelda(tablaInfo, "Clave Acceso:", factura.getClaveAcceso());
            document.add(tablaInfo);

            // 4. Detalles
            PdfPTable tablaDetalles = new PdfPTable(4);
            tablaDetalles.setWidthPercentage(100);
            tablaDetalles.setWidths(new float[] { 4, 1, 2, 2 });

            agregarCeldaHeader(tablaDetalles, "Producto / Descripción");
            agregarCeldaHeader(tablaDetalles, "Cant.");
            agregarCeldaHeader(tablaDetalles, "P. Unitario");
            agregarCeldaHeader(tablaDetalles, "Subtotal");

            if (factura.getDetalles() != null) {
                for (DetalleFactura det : factura.getDetalles()) {
                    // Safety check for product
                    String desc = (det.getProducto() != null) ? det.getProducto().getProductoNombre()
                            : "Producto Eliminado";

                    tablaDetalles
                            .addCell(new PdfPCell(new Phrase(desc, FontFactory.getFont(FontFactory.HELVETICA, 10))));
                    tablaDetalles.addCell(new PdfPCell(new Phrase(String.valueOf(det.getCantidad()),
                            FontFactory.getFont(FontFactory.HELVETICA, 10))));
                    tablaDetalles.addCell(new PdfPCell(new Phrase("$ " + String.format("%.2f", det.getPrecioUnitario()),
                            FontFactory.getFont(FontFactory.HELVETICA, 10))));
                    tablaDetalles.addCell(new PdfPCell(new Phrase("$ " + String.format("%.2f", det.getSubtotal()),
                            FontFactory.getFont(FontFactory.HELVETICA, 10))));
                }
            }
            document.add(tablaDetalles);

            // 5. Totales
            PdfPTable tablaTotales = new PdfPTable(2);
            tablaTotales.setWidthPercentage(40);
            tablaTotales.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tablaTotales.setSpacingBefore(10);

            agregarCeldaTotal(tablaTotales, "Subtotal 12%:", factura.getSubtotal12());
            agregarCeldaTotal(tablaTotales, "Subtotal 0%:", factura.getSubtotal0());
            agregarCeldaTotal(tablaTotales, "IVA 12%:", factura.getTotalIva());
            agregarCeldaTotal(tablaTotales, "TOTAL:", factura.getTotalFactura());

            document.add(tablaTotales);

            document.close();
        } catch (Exception e) {
            throw new IOException("Error creating PDF", e);
        }

        return baos.toByteArray();
    }

    // ======================================
    // UTILIDADES PRIVADAS
    // ======================================

    private void agregarEncabezadoEmpresa(Document document, facturacion.facturacion.Entidades.Empresa empresa)
            throws DocumentException {
        Paragraph nombreEmpresa = new Paragraph(empresa.getRazonSocial(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK));
        nombreEmpresa.setAlignment(Element.ALIGN_CENTER);
        document.add(nombreEmpresa);

        Paragraph ruc = new Paragraph("RUC: " + empresa.getRuc(), FontFactory.getFont(FontFactory.HELVETICA, 12));
        ruc.setAlignment(Element.ALIGN_CENTER);
        document.add(ruc);

        Paragraph dir = new Paragraph(empresa.getDirMatriz(), FontFactory.getFont(FontFactory.HELVETICA, 10));
        dir.setAlignment(Element.ALIGN_CENTER);
        dir.setSpacingAfter(20);
        document.add(dir);
    }

    private void agregarCelda(PdfPTable table, String label, String value) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        cellLabel.setBorder(Rectangle.NO_BORDER);
        table.addCell(cellLabel);

        PdfPCell cellValue = new PdfPCell(
                new Phrase(value != null ? value : "", FontFactory.getFont(FontFactory.HELVETICA, 10)));
        cellValue.setBorder(Rectangle.NO_BORDER);
        table.addCell(cellValue);
    }

    private void agregarCeldaHeader(PdfPTable table, String header) {
        PdfPCell cell = new PdfPCell(
                new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
        cell.setBackgroundColor(Color.DARK_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void agregarCeldaTotal(PdfPTable table, String label, Double value) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        cellLabel.setBorder(Rectangle.BOTTOM);
        table.addCell(cellLabel);

        PdfPCell cellValue = new PdfPCell(new Phrase("$ " + String.format("%.2f", value != null ? value : 0.0),
                FontFactory.getFont(FontFactory.HELVETICA, 10)));
        cellValue.setBorder(Rectangle.BOTTOM);
        cellValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cellValue);
    }
}
