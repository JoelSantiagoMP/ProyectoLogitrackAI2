package com.example.logitrack.service;

import com.example.logitrack.model.EstadoOrdenCompra;
import com.example.logitrack.model.OrdenCompra;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class OrdenPdfService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String MARCA_AGUA = "BORRADOR";

    public byte[] generar(OrdenCompra orden) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Document documento = new Document(PageSize.A4, 50, 50, 60, 50);
            PdfWriter writer = PdfWriter.getInstance(documento, salida);
            documento.open();

            if (orden.getEstado() == EstadoOrdenCompra.BORRADOR) {
                dibujarMarcaAguaDiagonal(writer);
            }

            Font titulo = new Font(Font.HELVETICA, 16, Font.BOLD, Color.DARK_GRAY);
            Font etiqueta = new Font(Font.HELVETICA, 11, Font.BOLD, Color.BLACK);
            Font valor = new Font(Font.HELVETICA, 11, Font.NORMAL, Color.BLACK);

            documento.add(new Paragraph("Orden de compra", titulo));
            documento.add(new Paragraph(" "));
            agregarLinea(documento, etiqueta, valor, "Número de orden", String.valueOf(orden.getId()));
            agregarLinea(documento, etiqueta, valor, "Fecha de creación",
                    orden.getFechaCreacion() != null ? orden.getFechaCreacion().format(FECHA) : "");
            agregarLinea(documento, etiqueta, valor, "Proveedor",
                    orden.getProveedor() != null ? orden.getProveedor().getNombre() : "");
            agregarLinea(documento, etiqueta, valor, "Producto",
                    orden.getProducto() != null ? orden.getProducto().getNombre() : "");
            agregarLinea(documento, etiqueta, valor, "Cantidad", String.valueOf(orden.getCantidad()));
            agregarLinea(documento, etiqueta, valor, "Precio unitario", String.valueOf(orden.getPrecioUnitario()));
            agregarLinea(documento, etiqueta, valor, "Total", String.valueOf(orden.getTotal()));
            agregarLinea(documento, etiqueta, valor, "Bodega destino",
                    orden.getBodegaDestino() != null ? orden.getBodegaDestino().getNombre() : "");
            agregarLinea(documento, etiqueta, valor, "Estado",
                    orden.getEstado() != null ? orden.getEstado().name() : "");

            documento.close();
            return salida.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF de la orden " + orden.getId(), e);
        }
    }

    private void agregarLinea(Document documento, Font etiqueta, Font valor, String nombre, String contenido)
            throws com.lowagie.text.DocumentException {
        Paragraph linea = new Paragraph();
        linea.add(new Phrase(nombre + ": ", etiqueta));
        linea.add(new Phrase(contenido, valor));
        linea.setSpacingAfter(6);
        documento.add(linea);
    }

    /**
     * Marca de agua en el fondo: texto BORRADOR, diagonal (~45°), gris claro y semitransparente.
     */
    private void dibujarMarcaAguaDiagonal(PdfWriter writer) {
        try {
            PdfContentByte fondo = writer.getDirectContentUnder();
            PdfGState transparencia = new PdfGState();
            transparencia.setFillOpacity(0.18f);
            fondo.saveState();
            fondo.setGState(transparencia);
            fondo.beginText();
            fondo.setColorFill(new Color(160, 160, 160));
            fondo.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED), 72);
            float x = PageSize.A4.getWidth() / 2;
            float y = PageSize.A4.getHeight() / 2;
            fondo.showTextAligned(Element.ALIGN_CENTER, MARCA_AGUA, x, y, 45);
            fondo.endText();
            fondo.restoreState();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo dibujar la marca de agua BORRADOR", e);
        }
    }
}
