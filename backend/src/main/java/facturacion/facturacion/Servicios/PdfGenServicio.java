package facturacion.facturacion.Servicios;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import facturacion.facturacion.Entidades.ComprobanteRetencion;
import facturacion.facturacion.Entidades.DetalleRetencion;
import facturacion.facturacion.Entidades.Factura;
import facturacion.facturacion.Entidades.DetalleFactura;
import facturacion.facturacion.Entidades.GuiaDeRemision;
import facturacion.facturacion.Entidades.DestinatarioGuia;
import facturacion.facturacion.Entidades.DetalleGuia;
import facturacion.facturacion.Entidades.LiquidacionDeCompra;
import facturacion.facturacion.Entidades.DetalleLiquidacion;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

import com.lowagie.text.pdf.Barcode128; // Importar Barcode
import com.lowagie.text.pdf.PdfContentByte; // Importar ContentByte
import com.lowagie.text.Image; // Importar Image

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

                        // ... (resto del metodo generarPdfRetencion sin cambios por ahora si no usa
                        // writer)
                        // Para simplificar, solo cambiamos generarPdfFactura donde usaremos el writer

                        // 1. Encabezado (Empresa)
                        agregarEncabezadoEmpresa(document, retencion.getEmpresa());

                        // ... rest of logic for retencion ... (Just matching context to be safe or
                        // skipping to Factura)
                        // Actually I can't easily skip lines in ReplaceFileContent safely if I don't
                        // see them.
                        // I will target the specific block in generarPdfFactura separately or use
                        // Imports only here.

                        // Let's just do imports first.

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
                                agregarCelda(tablaInfo, "Fecha Autorización:",
                                                retencion.getFechaAutorizacion().format(formatter));
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
                                                new PdfPCell(new Phrase(nombreImpuesto,
                                                                FontFactory.getFont(FontFactory.HELVETICA, 10))));
                                tablaDetalles.addCell(new PdfPCell(
                                                new Phrase("$ " + String.format("%.2f", det.getBaseImponible()),
                                                                FontFactory.getFont(FontFactory.HELVETICA, 10))));
                                tablaDetalles.addCell(new PdfPCell(
                                                new Phrase(det.getCodigoRetencion(),
                                                                FontFactory.getFont(FontFactory.HELVETICA, 10))));
                                tablaDetalles.addCell(new PdfPCell(
                                                new Phrase(det.getPorcentajeRetener() + " %",
                                                                FontFactory.getFont(FontFactory.HELVETICA, 10))));
                                tablaDetalles.addCell(new PdfPCell(
                                                new Phrase("$ " + String.format("%.2f", det.getValorRetenido()),
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
        // GENERAR PDF DE FACTURA (REDISEÑO RIDE)
        // ======================================
        public byte[] generarPdfFactura(Factura factura) throws IOException {
                // Validación de Estado SRI (Bloquear si no está autorizado, excepto Consumidor
                // Final)
                boolean esConsumidorFinal = factura.getCliente() != null &&
                                ("9999999999999".equals(factura.getCliente().getIdentificacion()) ||
                                                "Consumidor Final".equalsIgnoreCase(
                                                                factura.getCliente().getClienteNombre()));

                boolean tieneRespuestaSri = factura.getEstadoSri() != null
                                && !factura.getEstadoSri().equals("PENDIENTE");

                // Si no es consumidor final y no tiene respuesta del SRI (sigue PENDIENTE),
                // lanzamos excepción
                if (!esConsumidorFinal && !tieneRespuestaSri) {
                        throw new IOException("La factura aún no es validada o revisada por el SRI");
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Document document = new Document(PageSize.A4, 30, 30, 30, 30); // Márgenes ajustados

                try {
                        PdfWriter writer = PdfWriter.getInstance(document, baos);
                        document.open();

                        // ---------------------------------------------------------
                        // 1. ENCABEZADO SUPERIOR (2 COLUMNAS)
                        // ---------------------------------------------------------
                        PdfPTable headerTable = new PdfPTable(2);
                        headerTable.setWidthPercentage(100);
                        headerTable.setWidths(new float[] { 50f, 50f }); // 50% y 50%
                        headerTable.setSpacingAfter(10);

                        // --- COLUMNA IZQUIERDA: Info Empresa ---
                        PdfPCell cellEmpresa = new PdfPCell();
                        cellEmpresa.setBorder(Rectangle.NO_BORDER);

                        // Logo (Placeholder o Texto Grande)
                        Paragraph nombreEmpresa = new Paragraph(factura.getEmpresa().getRazonSocial(),
                                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK));
                        cellEmpresa.addElement(nombreEmpresa);

                        cellEmpresa.addElement(new Paragraph(factura.getEmpresa().getNombreComercial(),
                                        FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY)));

                        addSpacer(cellEmpresa);

                        // Dirección
                        Paragraph dirMatriz = new Paragraph("Dirección Matriz:\n" + factura.getEmpresa().getDirMatriz(),
                                        FontFactory.getFont(FontFactory.HELVETICA, 8));
                        cellEmpresa.addElement(dirMatriz);

                        Paragraph dirSucursal = new Paragraph(
                                        "Dirección Sucursal:\n" + factura.getEmpresa().getDirEstablecimiento(),
                                        FontFactory.getFont(FontFactory.HELVETICA, 8));
                        cellEmpresa.addElement(dirSucursal);

                        addSpacer(cellEmpresa);

                        Paragraph obligado = new Paragraph(
                                        "OBLIGADO A LLEVAR CONTABLIDAD: "
                                                        + factura.getEmpresa().getObligadoContabilidad(),
                                        FontFactory.getFont(FontFactory.HELVETICA, 8));
                        cellEmpresa.addElement(obligado);

                        headerTable.addCell(cellEmpresa);

                        // --- COLUMNA DERECHA: RUC y Datos Tributarios (Box) ---
                        PdfPCell cellRuc = new PdfPCell();
                        cellRuc.setBorder(Rectangle.BOX); // Borde alrededor
                        cellRuc.setBorderWidth(1f);
                        cellRuc.setPadding(10);

                        // RUC
                        cellRuc.addElement(new Paragraph("R.U.C.: " + factura.getEmpresa().getRuc(),
                                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));

                        // FACTURA No.
                        String numeroFactura = factura.getEmpresa().getEstablecimiento() + "-"
                                        + factura.getEmpresa().getPuntoEmision() + "-"
                                        + factura.getSecuencial();

                        cellRuc.addElement(new Paragraph("FACTURA",
                                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
                        cellRuc.addElement(new Paragraph("No. " + numeroFactura,
                                        FontFactory.getFont(FontFactory.HELVETICA, 10)));

                        addSpacer(cellRuc);

                        // Número de Autorización
                        cellRuc.addElement(new Paragraph("NÚMERO DE AUTORIZACIÓN",
                                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
                        String authNum = (factura.getEstadoSri() != null && factura.getEstadoSri().equals("AUTORIZADO"))
                                        ? factura.getClaveAcceso() // Normalmente es la misma clave en online
                                        : "PENDIENTE";
                        cellRuc.addElement(new Paragraph(authNum, FontFactory.getFont(FontFactory.HELVETICA, 8)));

                        addSpacer(cellRuc);

                        // Fecha Autorización
                        cellRuc.addElement(new Paragraph("FECHA Y HORA DE AUTORIZACIÓN",
                                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
                        String fechaAuth = (factura.getFechaAutorizacion() != null)
                                        ? factura.getFechaAutorizacion().format(formatter)
                                        : "PENDIENTE";
                        cellRuc.addElement(new Paragraph(fechaAuth, FontFactory.getFont(FontFactory.HELVETICA, 8)));

                        addSpacer(cellRuc);

                        // Ambiente y Emisión
                        cellRuc.addElement(new Paragraph("AMBIENTE: "
                                        + (factura.getEmpresa().getAmbiente() == 1 ? "PRUEBAS" : "PRODUCCIÓN"),
                                        FontFactory.getFont(FontFactory.HELVETICA, 8)));
                        cellRuc.addElement(new Paragraph("EMISIÓN: NORMAL",
                                        FontFactory.getFont(FontFactory.HELVETICA, 8)));

                        addSpacer(cellRuc);

                        // Clave de Acceso y Barcode
                        cellRuc.addElement(new Paragraph("CLAVE DE ACCESO",
                                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));

                        try {
                                PdfContentByte cb = writer.getDirectContent();
                                Barcode128 code128 = new Barcode128();
                                code128.setCode(factura.getClaveAcceso());
                                code128.setCodeType(Barcode128.CODE128);
                                code128.setBarHeight(30f); // Altura barras
                                code128.setFont(null); // Sin texto abajo (lo ponemos nosotros si queremos, o null)

                                Image code128Image = code128.createImageWithBarcode(cb, null, null);
                                code128Image.scalePercent(100);
                                cellRuc.addElement(code128Image);

                                cellRuc.addElement(new Paragraph(factura.getClaveAcceso(),
                                                FontFactory.getFont(FontFactory.HELVETICA, 8)));
                        } catch (Exception e) {
                                cellRuc.addElement(new Paragraph(factura.getClaveAcceso(),
                                                FontFactory.getFont(FontFactory.HELVETICA, 8)));
                        }

                        headerTable.addCell(cellRuc);
                        document.add(headerTable);

                        // ---------------------------------------------------------
                        // 2. INFO CLIENTE (Franja Horizontal)
                        // ---------------------------------------------------------
                        PdfPTable clientTable = new PdfPTable(2);
                        clientTable.setWidthPercentage(100);
                        clientTable.setWidths(new float[] { 60f, 40f });
                        clientTable.setSpacingAfter(10);

                        // Borde superior e inferior solo para estética simple
                        PdfPCell cellClientLeft = new PdfPCell();
                        cellClientLeft.setBorder(Rectangle.TOP | Rectangle.BOTTOM | Rectangle.LEFT);
                        cellClientLeft.setPadding(5);

                        String nombreCliente = (factura.getCliente() != null)
                                        ? factura.getCliente().getClienteNombre() + " "
                                                        + factura.getCliente().getClienteApellido()
                                        : "Consumidor Final";
                        String identCliente = (factura.getCliente() != null
                                        && factura.getCliente().getIdentificacion() != null)
                                                        ? factura.getCliente().getIdentificacion()
                                                        : "9999999999999";
                        String fechaEmision = factura.getFechaEmision()
                                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                        cellClientLeft.addElement(new Paragraph("Razón Social / Nombres y Apellidos: " + nombreCliente,
                                        FontFactory.getFont(FontFactory.HELVETICA, 9)));

                        String direccionCliente = (factura.getCliente() != null)
                                        ? factura.getCliente().getClienteDireccion()
                                        : "";
                        cellClientLeft.addElement(new Paragraph("Dirección: " + direccionCliente,
                                        FontFactory.getFont(FontFactory.HELVETICA, 9)));

                        cellClientLeft.addElement(new Paragraph("Fecha de Emisión: " + fechaEmision,
                                        FontFactory.getFont(FontFactory.HELVETICA, 9)));

                        PdfPCell cellClientRight = new PdfPCell();
                        cellClientRight.setBorder(Rectangle.TOP | Rectangle.BOTTOM | Rectangle.RIGHT);
                        cellClientRight.setPadding(5);
                        cellClientRight.addElement(new Paragraph("Identificación: " + identCliente,
                                        FontFactory.getFont(FontFactory.HELVETICA, 9)));

                        // Guia remisión podría ir aquí si existiera

                        clientTable.addCell(cellClientLeft);
                        clientTable.addCell(cellClientRight);
                        document.add(clientTable);

                        // ---------------------------------------------------------
                        // 3. DETALLES (Tabla Estilizada)
                        // ---------------------------------------------------------
                        PdfPTable tablaDetalles = new PdfPTable(5); // Cod, Cant, Desc, P.Unit, Total
                        tablaDetalles.setWidthPercentage(100);
                        tablaDetalles.setWidths(new float[] { 1.5f, 1f, 4.5f, 1.5f, 1.5f });
                        tablaDetalles.setSpacingAfter(10);

                        // Headers
                        addHeaderCell(tablaDetalles, "Cod. Principal");
                        addHeaderCell(tablaDetalles, "Cant.");
                        addHeaderCell(tablaDetalles, "Descripción");
                        addHeaderCell(tablaDetalles, "Precio Unitario");
                        addHeaderCell(tablaDetalles, "Precio Total");

                        if (factura.getDetalles() != null) {
                                for (DetalleFactura det : factura.getDetalles()) {
                                        String cod = (det.getProducto() != null)
                                                        ? String.valueOf(det.getProducto().getProductoId())
                                                        : "-";
                                        String desc = (det.getProducto() != null)
                                                        ? det.getProducto().getProductoNombre()
                                                        : "Producto Eliminado";

                                        addDetailCell(tablaDetalles, cod, Element.ALIGN_LEFT);
                                        addDetailCell(tablaDetalles, String.valueOf(det.getCantidad()),
                                                        Element.ALIGN_CENTER);
                                        addDetailCell(tablaDetalles, desc, Element.ALIGN_LEFT);
                                        addDetailCell(tablaDetalles, String.format("%.2f", det.getPrecioUnitario()),
                                                        Element.ALIGN_RIGHT);
                                        addDetailCell(tablaDetalles, String.format("%.2f", det.getSubtotal()),
                                                        Element.ALIGN_RIGHT);
                                }
                        }
                        document.add(tablaDetalles);

                        // ---------------------------------------------------------
                        // 4. INFO ADICIONAL Y TOTALES
                        // ---------------------------------------------------------
                        PdfPTable footerTable = new PdfPTable(2);
                        footerTable.setWidthPercentage(100);
                        footerTable.setWidths(new float[] { 60f, 40f });

                        // --- Info Adicional (Izquierda) ---
                        PdfPCell cellInfoAd = new PdfPCell();
                        cellInfoAd.setBorder(Rectangle.BOX);
                        cellInfoAd.setPadding(5);

                        Paragraph titleInfo = new Paragraph("INFORMACIÓN ADICIONAL",
                                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9));
                        titleInfo.setAlignment(Element.ALIGN_CENTER);
                        cellInfoAd.addElement(titleInfo);

                        if (factura.getCliente() != null) {
                                addInfoLine(cellInfoAd, "Teléfono:", factura.getCliente().getClienteTelefono());
                                addInfoLine(cellInfoAd, "Email:", factura.getCliente().getClienteEmail());
                        }
                        if (factura.getPagos() != null && !factura.getPagos().isEmpty()) {
                                int maxPlazo = factura.getPagos().stream()
                                                .mapToInt(p -> p.getPlazo() != null ? p.getPlazo() : 0)
                                                .max().orElse(0);
                                if (maxPlazo > 0) {
                                        addInfoLine(cellInfoAd, "Plazo:", maxPlazo + " días");
                                }
                        }

                        footerTable.addCell(cellInfoAd);

                        // --- Totales (Derecha) ---
                        PdfPCell cellTotals = new PdfPCell();
                        cellTotals.setBorder(Rectangle.NO_BORDER);

                        PdfPTable tTotalsInner = new PdfPTable(2);
                        tTotalsInner.setWidthPercentage(100);
                        tTotalsInner.setWidths(new float[] { 60f, 40f });

                        addTotalRow(tTotalsInner, "SUBTOTAL 12%", factura.getSubtotal12());
                        addTotalRow(tTotalsInner, "SUBTOTAL 0%", factura.getSubtotal0());
                        addTotalRow(tTotalsInner, "SUBTOTAL Exento de IVA", factura.getSubtotalExento());
                        addTotalRow(tTotalsInner, "SUBTOTAL NO OBJETO IVA", factura.getSubtotalNoObjeto());
                        addTotalRow(tTotalsInner, "DESCUENTO", factura.getTotalDescuento());
                        addTotalRow(tTotalsInner, "IVA 12%", factura.getTotalIva());
                        addTotalRow(tTotalsInner, "VALOR TOTAL", factura.getTotalFactura());

                        cellTotals.addElement(tTotalsInner);
                        footerTable.addCell(cellTotals);

                        // ---------------------------------------------------------
                        // 4.1. FORMAS DE PAGO
                        // ---------------------------------------------------------
                        if (factura.getPagos() != null && !factura.getPagos().isEmpty()) {
                                PdfPTable tablaPagos = new PdfPTable(2);
                                tablaPagos.setWidthPercentage(60); // Mas pequeño, a la izquierda
                                tablaPagos.setHorizontalAlignment(Element.ALIGN_LEFT);
                                tablaPagos.setWidths(new float[] { 70f, 30f });
                                tablaPagos.setSpacingAfter(10);

                                // Header Pagos
                                PdfPCell cellHeaderPago = new PdfPCell(new Phrase("Forma de Pago",
                                                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
                                cellHeaderPago.setBorder(Rectangle.BOX);
                                cellHeaderPago.setBackgroundColor(Color.LIGHT_GRAY);
                                tablaPagos.addCell(cellHeaderPago);

                                PdfPCell cellHeaderValor = new PdfPCell(new Phrase("Valor",
                                                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
                                cellHeaderValor.setBorder(Rectangle.BOX);
                                cellHeaderValor.setBackgroundColor(Color.LIGHT_GRAY);
                                tablaPagos.addCell(cellHeaderValor);

                                for (facturacion.facturacion.Entidades.FacturaPago pago : factura.getPagos()) {
                                        String nombrePago = (pago.getFormaPago() != null)
                                                        ? pago.getFormaPago().getNombre()
                                                        : "Otros";

                                        PdfPCell cNombre = new PdfPCell(new Phrase(nombrePago,
                                                        FontFactory.getFont(FontFactory.HELVETICA, 8)));
                                        cNombre.setBorder(Rectangle.BOX);
                                        tablaPagos.addCell(cNombre);

                                        PdfPCell cValor = new PdfPCell(
                                                        new Phrase(String.format("%.2f", pago.getTotal()),
                                                                        FontFactory.getFont(FontFactory.HELVETICA, 8)));
                                        cValor.setHorizontalAlignment(Element.ALIGN_RIGHT);
                                        cValor.setBorder(Rectangle.BOX);
                                        tablaPagos.addCell(cValor);
                                }
                                document.add(tablaPagos);
                        }

                        document.add(footerTable);

                        // ---------------------------------------------------------
                        // 5. BLOQUE DE FIRMA (Visual)
                        // ---------------------------------------------------------
                        agregarBloqueFirma(document, factura);

                        document.close();
                } catch (Exception e) {
                        throw new IOException("Error creating PDF", e);
                }

                return baos.toByteArray();
        }

        // ======================================
        // UTILIDADES PRIVADAS NUEVAS
        // ======================================

        private void addSpacer(PdfPCell cell) {
                cell.addElement(new Paragraph(" "));
        }

        // Antiguo método de encabezado (Se mantiene por compatibilidad si es necesario
        // para retenciones,
        // pero idealmente deberíamos actualizarlo también. Por ahora lo dejamos simple
        // para retenciones
        // o lo sustituimos arriba si 'generarPdfRetencion' lo usa.
        // NOTA: generarPdfRetencion llama a 'agregarEncabezadoEmpresa'. Lo dejaremos
        // como estaba o
        // crearemos uno nuevo. Para este refactor reemplazo todo el bloque final, así
        // que debo incluir
        // agregarEncabezadoEmpresa si 'generarPdfRetencion' no fue editado.
        // Revisando el archivo, 'generarPdfRetencion' usa 'agregarEncabezadoEmpresa'.
        // Debo mantenerlo.

        private void agregarEncabezadoEmpresa(Document document, facturacion.facturacion.Entidades.Empresa empresa)
                        throws DocumentException {
                // Mantenemos este simple para Retenciones, o podríamos mejorarlo después.
                Paragraph nombreEmpresa = new Paragraph(empresa.getRazonSocial(),
                                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK));
                nombreEmpresa.setAlignment(Element.ALIGN_CENTER);
                document.add(nombreEmpresa);
                // ... (resto simplificado si no se usa en factura)
        }

        private void addHeaderCell(PdfPTable table, String text) {
                PdfPCell cell = new PdfPCell(
                                new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setBackgroundColor(Color.DARK_GRAY);
                cell.setPadding(4);
                table.addCell(cell);
        }

        private void addDetailCell(PdfPTable table, String text, int align) {
                PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 8)));
                cell.setHorizontalAlignment(align);
                cell.setPadding(3);
                table.addCell(cell);
        }

        private void addInfoLine(PdfPCell cell, String label, String value) {
                if (value == null)
                        return;
                Paragraph p = new Paragraph(label + " " + value, FontFactory.getFont(FontFactory.HELVETICA, 8));
                cell.addElement(p);
        }

        private void addTotalRow(PdfPTable table, String label, Double value) {
                PdfPCell cLabel = new PdfPCell(new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA, 8)));
                cLabel.setBorder(Rectangle.BOX);
                cLabel.setPadding(3);
                table.addCell(cLabel);

                PdfPCell cValue = new PdfPCell(new Phrase("$ " + String.format("%.2f", value != null ? value : 0.0),
                                FontFactory.getFont(FontFactory.HELVETICA, 8)));
                cValue.setBorder(Rectangle.BOX);
                cValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
                cValue.setPadding(3);
                table.addCell(cValue);
        }

        private void agregarCelda(PdfPTable table, String label, String value) {
                // Método legacy para mantener compatibilidad con generarPdfRetencion
                PdfPCell cellLabel = new PdfPCell(
                                new Phrase(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
                cellLabel.setBorder(Rectangle.NO_BORDER);
                table.addCell(cellLabel);

                PdfPCell cellValue = new PdfPCell(
                                new Phrase(value != null ? value : "", FontFactory.getFont(FontFactory.HELVETICA, 10)));
                cellValue.setBorder(Rectangle.NO_BORDER);
                table.addCell(cellValue);
        }

        private void agregarCeldaHeader(PdfPTable table, String header) {
                // Legacy
                PdfPCell cell = new PdfPCell(
                                new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
                cell.setBackgroundColor(Color.DARK_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
        }

        private void agregarBloqueFirma(Document document, Factura factura) throws DocumentException {
                PdfPTable tablaFirma = new PdfPTable(1);
                tablaFirma.setWidthPercentage(50); // Ajustado para no ocupar todo el ancho
                tablaFirma.setHorizontalAlignment(Element.ALIGN_LEFT);
                tablaFirma.setSpacingBefore(30);

                PdfPCell celda = new PdfPCell();
                celda.setBorder(PdfPCell.NO_BORDER);
                celda.setHorizontalAlignment(Element.ALIGN_CENTER);

                celda.addElement(new Paragraph("________________________________________",
                                FontFactory.getFont(FontFactory.HELVETICA, 8)));
                celda.addElement(new Paragraph("FIRMADO ELECTRÓNICAMENTE POR",
                                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));

                String nombreFirma = factura.getEmpresa().getRazonSocial().toUpperCase();
                celda.addElement(new Paragraph(nombreFirma,
                                FontFactory.getFont(FontFactory.HELVETICA, 7)));

                tablaFirma.addCell(celda);
                document.add(tablaFirma);
        }

        // ======================================
        // GENERAR PDF DE GUÍA DE REMISIÓN
        // ======================================
        public byte[] generarPdfGuiaRemision(GuiaDeRemision guia) throws IOException {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Document document = new Document(PageSize.A4, 30, 30, 30, 30);

                try {
                        PdfWriter.getInstance(document, baos);
                        document.open();

                        // 1. Encabezado
                        agregarEncabezadoEmpresa(document, guia.getEmpresa());

                        // Título
                        Paragraph titulo = new Paragraph("GUÍA DE REMISIÓN",
                                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16));
                        titulo.setAlignment(Element.ALIGN_CENTER);
                        titulo.setSpacingAfter(10);
                        document.add(titulo);

                        // Info Guía
                        PdfPTable tablaInfo = new PdfPTable(2);
                        tablaInfo.setWidthPercentage(100);
                        tablaInfo.setSpacingAfter(15);
                        agregarCelda(tablaInfo, "No. Comprobante:", guia.getSecuencial());
                        agregarCelda(tablaInfo, "Estado SRI:", guia.getEstadoSri());
                        agregarCelda(tablaInfo, "Fecha Emisión:", guia.getFechaEmision().format(formatter));
                        agregarCelda(tablaInfo, "Clave Acceso:", guia.getClaveAcceso());
                        document.add(tablaInfo);

                        // 2. Info Transportista
                        Paragraph subTrans = new Paragraph("INFORMACIÓN DEL TRANSPORTISTA",
                                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
                        subTrans.setSpacingAfter(5);
                        document.add(subTrans);

                        PdfPTable tablaTrans = new PdfPTable(2);
                        tablaTrans.setWidthPercentage(100);
                        tablaTrans.setSpacingAfter(15);
                        agregarCelda(tablaTrans, "Razón Social:", guia.getTransportistaRazonSocial());
                        agregarCelda(tablaTrans, "RUC/CI:", guia.getTransportistaIdentificacion());
                        agregarCelda(tablaTrans, "Placa:", guia.getPlaca());
                        agregarCelda(tablaTrans, "Fecha Inicio:", guia.getFechaIniTransporte().toString());
                        agregarCelda(tablaTrans, "Fecha Fin:", guia.getFechaFinTransporte().toString());
                        agregarCelda(tablaTrans, "Punto Partida:", guia.getDirPartida());
                        document.add(tablaTrans);

                        // 3. Destinatarios
                        if (guia.getDestinatarios() != null) {
                                for (DestinatarioGuia dest : guia.getDestinatarios()) {
                                        Paragraph subDest = new Paragraph(
                                                        "DESTINATARIO: " + dest.getRazonSocialDestinatario(),
                                                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11));
                                        subDest.setSpacingBefore(10);
                                        subDest.setSpacingAfter(5);
                                        document.add(subDest);

                                        PdfPTable tablaDest = new PdfPTable(2);
                                        tablaDest.setWidthPercentage(100);
                                        tablaDest.setSpacingAfter(10);
                                        agregarCelda(tablaDest, "RUC/CI:", dest.getIdentificacionDestinatario());
                                        agregarCelda(tablaDest, "Dirección Llegada:", dest.getDirDestinatario());
                                        agregarCelda(tablaDest, "Motivo:", dest.getMotivoTraslado());
                                        agregarCelda(tablaDest, "Ruta:", dest.getRuta());
                                        document.add(tablaDest);

                                        // Detalles
                                        PdfPTable tablaDetalles = new PdfPTable(3);
                                        tablaDetalles.setWidthPercentage(100);
                                        tablaDetalles.setWidths(new float[] { 2f, 6f, 2f });

                                        agregarCeldaHeader(tablaDetalles, "Código");
                                        agregarCeldaHeader(tablaDetalles, "Descripción");
                                        agregarCeldaHeader(tablaDetalles, "Cantidad");

                                        if (dest.getDetalles() != null) {
                                                for (DetalleGuia det : dest.getDetalles()) {
                                                        tablaDetalles.addCell(new PdfPCell(new Phrase(
                                                                        det.getCodigoInterno(), FontFactory.getFont(
                                                                                        FontFactory.HELVETICA, 9))));
                                                        tablaDetalles.addCell(new PdfPCell(new Phrase(
                                                                        det.getDescripcion(), FontFactory.getFont(
                                                                                        FontFactory.HELVETICA, 9))));
                                                        tablaDetalles.addCell(new PdfPCell(new Phrase(
                                                                        String.valueOf(det.getCantidad()),
                                                                        FontFactory.getFont(FontFactory.HELVETICA,
                                                                                        9))));
                                                }
                                        }
                                        document.add(tablaDetalles);
                                }
                        }

                        document.close();
                } catch (Exception e) {
                        throw new IOException("Error creating PDF Guia", e);
                }
                return baos.toByteArray();
        }

        // ======================================
        // GENERAR PDF DE LIQUIDACIÓN DE COMPRA
        // ======================================
        public byte[] generarPdfLiquidacionCompra(LiquidacionDeCompra liquidacion) throws IOException {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Document document = new Document(PageSize.A4, 30, 30, 30, 30);

                try {
                        PdfWriter.getInstance(document, baos);
                        document.open();

                        // 1. Encabezado
                        agregarEncabezadoEmpresa(document, liquidacion.getEmpresa());

                        // Título
                        Paragraph titulo = new Paragraph("LIQUIDACIÓN DE COMPRA",
                                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16));
                        titulo.setAlignment(Element.ALIGN_CENTER);
                        titulo.setSpacingAfter(10);
                        document.add(titulo);

                        // Info SRI
                        PdfPTable tablaInfo = new PdfPTable(2);
                        tablaInfo.setWidthPercentage(100);
                        tablaInfo.setSpacingAfter(15);
                        agregarCelda(tablaInfo, "No. Comprobante:", liquidacion.getSecuencial());
                        agregarCelda(tablaInfo, "Estado SRI:", liquidacion.getEstadoSri());
                        agregarCelda(tablaInfo, "Fecha Emisión:", liquidacion.getFechaEmision().format(formatter));
                        if (liquidacion.getFechaAutorizacion() != null) {
                                agregarCelda(tablaInfo, "Fecha Autorización:",
                                                liquidacion.getFechaAutorizacion().format(formatter));
                        }
                        agregarCelda(tablaInfo, "Clave Acceso:", liquidacion.getClaveAcceso());
                        document.add(tablaInfo);

                        // 2. Info Proveedor
                        Paragraph subProv = new Paragraph("INFORMACIÓN DEL PROVEEDOR",
                                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
                        subProv.setSpacingAfter(5);
                        document.add(subProv);

                        PdfPTable tablaProv = new PdfPTable(2);
                        tablaProv.setWidthPercentage(100);
                        tablaProv.setSpacingAfter(15);
                        if (liquidacion.getProveedor() != null) {
                                agregarCelda(tablaProv, "Razón Social:", liquidacion.getProveedor().getRazonSocial());
                                agregarCelda(tablaProv, "RUC/CI:", liquidacion.getProveedor().getRuc());
                                agregarCelda(tablaProv, "Dirección:", liquidacion.getProveedor().getDireccion());
                                agregarCelda(tablaProv, "Email:", liquidacion.getProveedor().getEmail());
                        }
                        document.add(tablaProv);

                        // 3. Detalles
                        PdfPTable tablaDetalles = new PdfPTable(4);
                        tablaDetalles.setWidthPercentage(100);
                        tablaDetalles.setWidths(new float[] { 1.5f, 4.5f, 2f, 2f });
                        tablaDetalles.setSpacingAfter(10);

                        agregarCeldaHeader(tablaDetalles, "Cant");
                        agregarCeldaHeader(tablaDetalles, "Descripción");
                        agregarCeldaHeader(tablaDetalles, "P. Unitario");
                        agregarCeldaHeader(tablaDetalles, "Total");

                        if (liquidacion.getDetalles() != null) {
                                for (DetalleLiquidacion det : liquidacion.getDetalles()) {
                                        tablaDetalles.addCell(new PdfPCell(new Phrase(String.valueOf(det.getCantidad()),
                                                        FontFactory.getFont(FontFactory.HELVETICA, 9))));
                                        tablaDetalles.addCell(new PdfPCell(new Phrase(det.getDescripcion(),
                                                        FontFactory.getFont(FontFactory.HELVETICA, 9))));
                                        tablaDetalles.addCell(new PdfPCell(new Phrase(
                                                        "$ " + String.format("%.2f", det.getPrecioUnitario()),
                                                        FontFactory.getFont(FontFactory.HELVETICA, 9))));
                                        tablaDetalles.addCell(new PdfPCell(new Phrase(
                                                        "$ " + String.format("%.2f", det.getPrecioTotalSinImpuesto()),
                                                        FontFactory.getFont(FontFactory.HELVETICA, 9))));
                                }
                        }
                        document.add(tablaDetalles);

                        // 4. Totales
                        PdfPTable tablaTotales = new PdfPTable(2);
                        tablaTotales.setWidthPercentage(40);
                        tablaTotales.setHorizontalAlignment(Element.ALIGN_RIGHT);

                        agregarCelda(tablaTotales, "Subtotal 12%:",
                                        "$ " + String.format("%.2f", liquidacion.getSubtotal12()));
                        agregarCelda(tablaTotales, "Subtotal 0%:",
                                        "$ " + String.format("%.2f", liquidacion.getSubtotal0()));
                        agregarCelda(tablaTotales, "IVA 12%:", "$ " + String.format("%.2f", liquidacion.getTotalIva()));
                        agregarCelda(tablaTotales, "TOTAL:", "$ " + String.format("%.2f", liquidacion.getTotal()));

                        document.add(tablaTotales);

                        document.close();
                } catch (Exception e) {
                        throw new IOException("Error creating PDF Liquidacion", e);
                }
                return baos.toByteArray();
        }
}
