package com.example.logitrack.service;

import com.example.logitrack.model.Bodega;
import com.example.logitrack.model.EstadoOrdenCompra;
import com.example.logitrack.model.OrdenCompra;
import com.example.logitrack.model.Producto;
import com.example.logitrack.model.Proveedor;
import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrdenPdfServiceTest {

    private final OrdenPdfService servicio = new OrdenPdfService();

    @Test
    void pdfBorrador_incluyeMarcaDeAguaDiagonal() throws Exception {
        byte[] pdf = servicio.generar(ordenConEstado(EstadoOrdenCompra.BORRADOR));
        String pagina = contenidoDePagina(pdf);

        assertTrue(new String(pdf, StandardCharsets.ISO_8859_1).startsWith("%PDF"));
        assertTrue(pagina.contains("BORRADOR"));
        assertTrue(pagina.contains("45"), "la marca de agua debe ir rotada en diagonal");
        assertTrue(pagina.contains("/ca") || pagina.toLowerCase().contains("ca"),
                "la marca de agua debe ser semitransparente");
    }

    @Test
    void pdfAprobada_noIncluyeMarcaDeAgua() throws Exception {
        byte[] pdf = servicio.generar(ordenConEstado(EstadoOrdenCompra.APROBADA));
        String pagina = contenidoDePagina(pdf);

        assertTrue(pagina.contains("APROBADA"));
        assertFalse(pagina.contains("BORRADOR"));
        assertFalse(pagina.contains(" 45 "));
    }

    private String contenidoDePagina(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            return new String(reader.getPageContent(1), StandardCharsets.ISO_8859_1);
        } finally {
            reader.close();
        }
    }

    private OrdenCompra ordenConEstado(EstadoOrdenCompra estado) {
        Proveedor proveedor = Proveedor.builder().nombre("Proveedor PDF").diasEntrega(5).build();
        proveedor.setId(1L);
        Producto producto = new Producto();
        producto.setId(2L);
        producto.setNombre("Producto PDF");
        producto.setPrecio(100.0);
        Bodega bodega = Bodega.builder().nombre("Bodega PDF").capacidad(100).build();
        bodega.setId(3L);
        return OrdenCompra.builder()
                .id(14L)
                .producto(producto)
                .proveedor(proveedor)
                .bodegaDestino(bodega)
                .cantidad(4)
                .precioUnitario(100.0)
                .total(400.0)
                .fechaCreacion(LocalDateTime.of(2026, 8, 24, 10, 0))
                .estado(estado)
                .build();
    }
}
