package com.example.logitrack.iq;

import com.example.logitrack.dto.LoginRequest;
import com.example.logitrack.model.Bodega;
import com.example.logitrack.model.EstadoOrdenCompra;
import com.example.logitrack.model.OrdenCompra;
import com.example.logitrack.model.Producto;
import com.example.logitrack.model.Proveedor;
import com.example.logitrack.model.Rol;
import com.example.logitrack.model.Usuario;
import com.example.logitrack.repository.BodegaRepository;
import com.example.logitrack.repository.OrdenCompraRepository;
import com.example.logitrack.repository.ProductoRepository;
import com.example.logitrack.repository.ProveedorRepository;
import com.example.logitrack.repository.ResumenPanelRepository;
import com.example.logitrack.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private PasswordEncoder passwordEncoder;

    private Long ordenCanceladaId;

    @BeforeEach
    void prepararDatos() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        ordenCompraRepository.deleteAll();
        resumenPanelRepository.deleteAll();
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

        Bodega bodega = bodegaRepository.save(Bodega.builder()
                .nombre("Bodega IQ")
                .ubicacion("Bogota")
                .capacidad(500)
                .encargado(admin)
                .build());

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
    void agenteAprueba_retorna403() throws Exception {
        String token = tokenDe("agente-iq", "agente123");

        mockMvc.perform(patch("/api/ordenes/{id}/estado", ordenCanceladaId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"APROBADA\"}"))
                .andExpect(status().isForbidden());
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
