package com.example.logitrack.controller;

import com.example.logitrack.config.IqOpenApiDocs;
import com.example.logitrack.dto.PanelResumenRequest;
import com.example.logitrack.model.ResumenPanel;
import com.example.logitrack.service.PanelResumenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/panel", "/panel"})
@Tag(name = IqOpenApiDocs.TAG_PANEL)
@SecurityRequirement(name = "bearerAuth")
public class PanelResumenController {

    private final PanelResumenService panelResumenService;

    public PanelResumenController(PanelResumenService panelResumenService) {
        this.panelResumenService = panelResumenService;
    }

    @Operation(summary = "Publicar resumen del panel",
            description = """
                    Valida y publica el resumen estructurado del flujo n8n. Solo admite el contrato estricto \
                    (sin propiedades adicionales). Un resumen válido por fecha; reemplazo auditado.

                    """ + IqOpenApiDocs.ROLE_ADMIN_AGENTE)
    @ApiResponse(responseCode = "200", description = "Resumen publicado")
    @ApiResponse(responseCode = "400", description = "JSON inválido; se conserva el resumen anterior")
    @IqOpenApiDocs.SecuredAdminOrAgente
    @PostMapping("/resumen")
    public ResponseEntity<ResumenPanel> publicar(@Valid @RequestBody PanelResumenRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(panelResumenService.publicar(request, authentication.getName()));
    }

    @Operation(summary = "Obtener último resumen válido",
            description = """
                    Devuelve el contenido del último resumen publicado. **404** si aún no existe.

                    """ + IqOpenApiDocs.ROLE_ADMIN_AGENTE)
    @ApiResponse(responseCode = "200", description = "Resumen del panel",
            content = @Content(schema = @Schema(implementation = PanelResumenRequest.class)))
    @ApiResponse(responseCode = "404", description = "No hay resumen publicado")
    @IqOpenApiDocs.SecuredAdminOrAgente
    @GetMapping("/resumen")
    public ResponseEntity<PanelResumenRequest> obtenerUltimo() {
        return ResponseEntity.ok(panelResumenService.obtenerUltimoContenido());
    }
}
