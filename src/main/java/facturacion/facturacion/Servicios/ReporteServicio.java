package facturacion.facturacion.Servicios;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import facturacion.facturacion.Entidades.Cliente;
import facturacion.facturacion.Entidades.Factura;
import facturacion.facturacion.Entidades.Producto;
import facturacion.facturacion.Repositorios.ClienteRepositorio;
import facturacion.facturacion.Repositorios.FacturaRepositorio;
import facturacion.facturacion.Repositorios.ProductoRepositorio;

@Service
public class ReporteServicio {

    @Autowired
    private ClienteRepositorio clienteRepositorio;

    @Autowired
    private ProductoRepositorio productoRepositorio;

    @Autowired
    private FacturaRepositorio facturaRepositorio;

    public byte[] generarReporteClientesPdf() throws DocumentException {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, out);
        document.open();

        Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        fontTitle.setSize(18);

        Paragraph paragraph = new Paragraph("Reporte de Clientes", fontTitle);
        paragraph.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(paragraph);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100f);
        table.setWidths(new int[] { 3, 3, 3, 3 });
        table.setSpacingBefore(10);

        PdfPCell cell = new PdfPCell();
        cell.setPhrase(new Phrase("Nombre"));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Identificación"));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Correo"));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Teléfono"));
        table.addCell(cell);

        List<Cliente> clientes = clienteRepositorio.findAll();

        for (Cliente cliente : clientes) {
            table.addCell(cliente.getClienteNombre());
            table.addCell(String.valueOf(cliente.getClienteId()));
            table.addCell(cliente.getClienteEmail());
            table.addCell(cliente.getClienteTelefono());
        }

        document.add(table);
        document.close();

        return out.toByteArray();
    }

    public byte[] generarReporteProductosExcel() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Productos");

            Row headerRow = sheet.createRow(0);
            String[] columns = { "ID", "Nombre", "Precio Unitario", "Stock", "Código" };

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
            }

            List<Producto> productos = productoRepositorio.findAll();
            int rowIdx = 1;

            for (Producto producto : productos) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(producto.getProductoId());
                row.createCell(1).setCellValue(producto.getProductoNombre());
                row.createCell(2).setCellValue(producto.getProductoPrecio());
                row.createCell(3).setCellValue(producto.getProductoStock());
                row.createCell(4).setCellValue(producto.getProductoSerial());
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generarReporteVentasPdf() throws DocumentException {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, out);
        document.open();

        Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        fontTitle.setSize(18);

        Paragraph paragraph = new Paragraph("Reporte de Ventas (Facturas)", fontTitle);
        paragraph.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(paragraph);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100f);
        table.setSpacingBefore(10);

        // Headers
        table.addCell("ID");
        table.addCell("Cliente");
        table.addCell("Fecha");
        table.addCell("Total");
        table.addCell("Estado SRI");

        List<Factura> facturas = facturaRepositorio.findAll();

        for (Factura factura : facturas) {
            table.addCell(String.valueOf(factura.getFacturaId()));
            table.addCell(factura.getCliente() != null ? factura.getCliente().getClienteNombre() : "N/A");
            table.addCell(factura.getFechaEmision() != null ? factura.getFechaEmision().toString() : "");
            table.addCell(String.valueOf(factura.getTotalFactura()));

            String estadoStr = "Pendiente";
            if (factura.getEstado() != null) {
                if (factura.getEstado() == 1)
                    estadoStr = "Registrada";
                if (factura.getEstado() == 2)
                    estadoStr = "Enviada";
                if (factura.getEstado() == 3)
                    estadoStr = "Autorizada";
            }
            table.addCell(estadoStr);
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }
}
