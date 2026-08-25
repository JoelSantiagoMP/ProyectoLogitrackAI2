package com.example.logitrack.iq;

import com.example.logitrack.dto.LoginRequest;
import com.example.logitrack.model.Bodega;
import com.example.logitrack.model.EstadoOrdenCompra;
import com.example.logitrack.model.Movimiento;
import com.example.logitrack.model.OrdenCompra;
import com.example.logitrack.model.Producto;
import com.example.logitrack.model.Proveedor;
import com.example.logitrack.model.Rol;
import com.example.logitrack.model.TipoMovimiento;
import com.example.logitrack.model.Usuario;
import com.example.logitrack.repository.AuditoriaRepository;
import com.example.logitrack.repository.BodegaRepository;
import com.example.logitrack.repository.InventarioBodegaRepository;
import com.example.logitrack.repository.MovimientoRepository;
import com.example.logitrack.repository.OrdenCompraRepository;
import com.example.logitrack.repository.ProductoRepository;
import com.example.logitrack.repository.ProveedorRepository;
import com.example.logitrack.repository.ResumenPanelRepository;
import com.example.logitrack.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class OrdenCompraEstadoTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private BodegaRepository bodegaRepository;

    @Autowired
    private OrdenCompraRepository ordenCompraRepository;

    @Autowired
    private ResumenPanelRepository resumenPanelRepository;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private InventarioBodegaRepository inventarioBodegaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long ordenCanceladaId;
    private Long ordenBorradorId;
    private Long productoId;
    private Long proveedorId;
    private Long bodegaId;

    @BeforeEach
    void prepararDatos() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        movimientoRepository.deleteAll();
        inventarioBodegaRepository.deleteAll();
        ordenCompraRepository.deleteAll();
        resumenPanelRepository.deleteAll();
        auditoriaRepository.deleteAll();
        productoRepository.deleteAll();
        bodegaRepository.deleteAll();
        proveedorRepository.deleteAll();
        usuarioRepository.deleteAll();

        Usuario admin = usuarioRepository.save(Usuario.builder()
                .username("admin-iq")
                .password(passwordEncoder.encode("admin123"))
                .rol(Rol.ADMIN)
                .build());
        usuarioRepository.save(Usuario.builder()
                .username("agente-iq")
                .password(passwordEncoder.encode("agente123"))
                .rol(Rol.AGENTE)
                .build());

        Proveedor proveedor = proveedorRepository.save(Proveedor.builder()
                .nombre("Proveedor IQ")
                .contacto("proveedor@iq.test")
                .diasEntrega(7)
                .build());

        Producto producto = new Producto();
        producto.setNombre("Producto IQ transiciones");
        producto.setCategoria("insumos");
        producto.setPrecio(1000.0);
        producto.setProveedorPrincipal(proveedor);
        producto = productoRepository.save(producto);
        productoId = producto.getId();
        proveedorId = proveedor.getId();

        Bodega bodega = bodegaRepository.save(Bodega.builder()
                .nombre("Bodega IQ")
                .ubicacion("Bogota")
                .capacidad(500)
                .encargado("Carlos Ramírez")
                .build());
        bodegaId = bodega.getId();

        OrdenCompra orden = OrdenCompra.builder()
                .producto(producto)
                .proveedor(proveedor)
                .bodegaDestino(bodega)
                .cantidad(10)
                .precioUnitario(1000.0)
                .total(10000.0)
                .estado(EstadoOrdenCompra.CANCELADA)
                .creadoPor(admin)
                .build();
        ordenCanceladaId = ordenCompraRepository.save(orden).getId();

        OrdenCompra borrador = OrdenCompra.builder()
                .producto(producto)
                .proveedor(proveedor)
                .bodegaDestino(bodega)
                .cantidad(8)
                .precioUnitario(1000.0)
                .total(8000.0)
                .estado(EstadoOrdenCompra.BORRADOR)
                .creadoPor(admin)
                .build();
        ordenBorradorId = ordenCompraRepository.save(borrador).getId();
    }

    @Test
    void ordenCancelada_noSeAprueba_retorna400() throws Exception {
        String token = tokenDe("admin-iq", "admin123");

        mockMvc.perform(patch("/api/ordenes/{id}/estado", ordenCanceladaId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"APROBADA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ordenCantidadInvalida_retorna400() throws Exception {
        String token = tokenDe("agente-iq", "agente123");

        mockMvc.perform(post("/api/ordenes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productoId":%d,"proveedorId":%d,"bodegaDestinoId":%d,"precioUnitario":1000.0,"cantidad":0}
                                """.formatted(productoId, proveedorId, bodegaId)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/ordenes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productoId":%d,"proveedorId":%d,"bodegaDestinoId":%d,"precioUnitario":1000.0,"cantidad":-3}
                                """.formatted(productoId, proveedorId, bodegaId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recepcion_creaMovimientoEntrada() throws Exception {
        String token = tokenDe("admin-iq", "admin123");
        Usuario admin = usuarioRepository.findByUsername("admin-iq").orElseThrow();

        OrdenCompra aprobada = ordenCompraRepository.save(OrdenCompra.builder()
                .producto(productoRepository.findById(productoId).orElseThrow())
                .proveedor(proveedorRepository.findById(proveedorId).orElseThrow())
                .bodegaDestino(bodegaRepository.findById(bodegaId).orElseThrow())
                .cantidad(12)
                .precioUnitario(500.0)
                .total(6000.0)
                .estado(EstadoOrdenCompra.APROBADA)
                .creadoPor(admin)
                .build());

        long entradasAntes = movimientoRepository.findByTipoMovimiento(TipoMovimiento.ENTRADA).size();

        mockMvc.perform(patch("/api/ordenes/{id}/estado", aprobada.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"RECIBIDA\"}"))
                .andExpect(status().isOk());

        assertEquals(EstadoOrdenCompra.RECIBIDA,
                ordenCompraRepository.findById(aprobada.getId()).orElseThrow().getEstado());

        long entradasDespues = movimientoRepository.findByTipoMovimiento(TipoMovimiento.ENTRADA).size();
        assertEquals(entradasAntes + 1, entradasDespues);

        Movimiento entrada = movimientoRepository.findByTipoMovimiento(TipoMovimiento.ENTRADA).stream()
                .filter(m -> m.getBodegaDestino() != null && bodegaId.equals(m.getBodegaDestino().getId()))
                .filter(m -> m.getDetalles() != null && m.getDetalles().stream()
                        .anyMatch(d -> productoId.equals(d.getProducto().getId()) && d.getCantidad() == 12))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se registró la ENTRADA de recepción"));
        assertEquals(TipoMovimiento.ENTRADA, entrada.getTipoMovimiento());
    }

    @Test
    void agenteRegistraMovimiento_retorna403() throws Exception {
        String token = tokenDe("agente-iq", "agente123");

        mockMvc.perform(post("/api/movimientos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoMovimiento":"ENTRADA","bodegaDestinoId":%d,"detalles":[{"productoId":%d,"cantidad":1}]}
                                """.formatted(bodegaId, productoId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void agenteAprueba_retorna403() throws Exception {
        String token = tokenDe("agente-iq", "agente123");

        mockMvc.perform(patch("/api/ordenes/{id}/estado", ordenCanceladaId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"APROBADA\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void pdfBorrador_watermarkYSeInvalidaAlCambiarEstado() throws Exception {
        String token = tokenDe("admin-iq", "admin123");

        mockMvc.perform(get("/api/ordenes/{id}/pdf", ordenBorradorId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        byte[] pdf = mockMvc.perform(post("/api/ordenes/{id}/pdf", ordenBorradorId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        String contenido = contenidoDePagina(pdf);
        assertTrue(new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1).startsWith("%PDF"));
        assertTrue(contenido.contains("BORRADOR"));
        assertTrue(contenido.contains("45"));
        assertNotNull(ordenCompraRepository.findById(ordenBorradorId).orElseThrow().getPdf());
        assertNotNull(ordenCompraRepository.findById(ordenBorradorId).orElseThrow().getFechaGeneracionPdf());

        mockMvc.perform(get("/api/ordenes/{id}/pdf", ordenBorradorId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        mockMvc.perform(patch("/api/ordenes/{id}/estado", ordenBorradorId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"APROBADA\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/ordenes/{id}/pdf", ordenBorradorId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        OrdenCompra actualizada = ordenCompraRepository.findById(ordenBorradorId).orElseThrow();
        assertNull(actualizada.getPdf());
        assertNull(actualizada.getFechaGeneracionPdf());
    }

    private String contenidoDePagina(byte[] pdf) throws Exception {
        PdfReader reader = new PdfReader(pdf);
        try {
            return new String(reader.getPageContent(1), java.nio.charset.StandardCharsets.ISO_8859_1);
        } finally {
            reader.close();
        }
    }

    private String tokenDe(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).get("accessToken").asText();
    }
}
