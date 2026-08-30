package com.example.logitrack.controller;

import com.example.logitrack.config.IqOpenApiDocs;
import com.example.logitrack.dto.KpisResponse;
import com.example.logitrack.service.IndicadoresInventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = IqOpenApiDocs.TAG_KPIS)
@SecurityRequirement(name = "bearerAuth")
public class KpiController {

    private final IndicadoresInventarioService indicadoresInventarioService;

    public KpiController(IndicadoresInventarioService indicadoresInventarioService) {
        this.indicadoresInventarioService = indicadoresInventarioService;
    }

    @Operation(
            summary = "Obtener KPIs de inventario",
            description = """
                    Devuelve los cuatro indicadores del dashboard, ocupación por bodega, movimientos del día \
                    anterior (America/Bogota) y marca de tiempo `calculadoEn`.

                    """ + IqOpenApiDocs.ROLE_ADMIN_AGENTE)
    @ApiResponse(responseCode = "200", description = "KPIs calculados",
            content = @Content(schema = @Schema(implementation = KpisResponse.class)))
    @IqOpenApiDocs.SecuredAdminOrAgente
    @GetMapping({"/api/kpis", "/kpis"})
    public ResponseEntity<KpisResponse> obtenerKpis() {
        return ResponseEntity.ok(indicadoresInventarioService.obtenerKpis());
    }
}
