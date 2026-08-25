package com.example.logitrack.iq;

import com.example.logitrack.dto.LoginRequest;
import com.example.logitrack.model.ResumenPanel;
import com.example.logitrack.model.Rol;
import com.example.logitrack.model.Usuario;
import com.example.logitrack.repository.BodegaRepository;
import com.example.logitrack.repository.InventarioBodegaRepository;
import com.example.logitrack.repository.MovimientoRepository;
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

import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class PanelResumenTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ResumenPanelRepository resumenPanelRepository;

    @Autowired
    private OrdenCompraRepository ordenCompraRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private BodegaRepository bodegaRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private InventarioBodegaRepository inventarioBodegaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private LocalDate hoyBogota;

    @BeforeEach
    void prepararDatos() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        movimientoRepository.deleteAll();
        inventarioBodegaRepository.deleteAll();
        ordenCompraRepository.deleteAll();
        resumenPanelRepository.deleteAll();
        productoRepository.deleteAll();
        bodegaRepository.deleteAll();
        proveedorRepository.deleteAll();
        usuarioRepository.deleteAll();

        Usuario admin = usuarioRepository.save(Usuario.builder()
                .username("admin-panel")
                .password(passwordEncoder.encode("admin123"))
                .rol(Rol.ADMIN)
                .build());

        hoyBogota = LocalDate.now(BOGOTA);
        String contenidoValidoAnterior = """
                {"fecha":"%s","narrativa":"Resumen valido previo que debe conservarse.","alertas":[],"accionesSugeridas":[]}
                """.formatted(hoyBogota);

        resumenPanelRepository.save(ResumenPanel.builder()
                .fecha(hoyBogota)
                .contenidoJson(contenidoValidoAnterior)
                .autor(admin)
                .build());
    }

    @Test
    void resumenInvalido_conservaAnterior() throws Exception {
        String token = tokenDe("admin-panel", "admin123");

        String payloadSeveridadInvalida = """
                {
                  "fecha": "%s",
                  "narrativa": "Hay productos en riesgo y una orden pendiente de aprobacion.",
                  "alertas": [
                    {
                      "severidad": "CRITICA",
                      "titulo": "Producto en riesgo",
                      "detalle": "Producto X esta por debajo de su punto de reorden.",
                      "productoId": 12,
                      "ordenId": null,
                      "bodegaId": 3
                    }
                  ],
                  "accionesSugeridas": []
                }
                """.formatted(hoyBogota);

        mockMvc.perform(post("/api/panel/resumen")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadSeveridadInvalida))
                .andExpect(status().isBadRequest());

        String payloadIdInexistente = """
                {
                  "fecha": "%s",
                  "narrativa": "Hay productos en riesgo y una orden pendiente de aprobacion.",
                  "alertas": [
                    {
                      "severidad": "ALTA",
                      "titulo": "Producto en riesgo",
                      "detalle": "Producto X esta por debajo de su punto de reorden.",
                      "productoId": 999999,
                      "ordenId": null,
                      "bodegaId": null
                    }
                  ],
                  "accionesSugeridas": []
                }
                """.formatted(hoyBogota);

        mockMvc.perform(post("/api/panel/resumen")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadIdInexistente))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/panel/resumen")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Resumen valido previo que debe conservarse")));
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
