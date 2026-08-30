package com.example.logitrack.controller;

import com.example.logitrack.config.IqOpenApiDocs;
import com.example.logitrack.dto.CambioEstadoOrdenRequest;
import com.example.logitrack.dto.OrdenCompraRequest;
import com.example.logitrack.model.EstadoOrdenCompra;
import com.example.logitrack.model.OrdenCompra;
import com.example.logitrack.service.OrdenCompraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/ordenes", "/ordenes"})
@Tag(name = IqOpenApiDocs.TAG_ORDENES)
@SecurityRequirement(name = "bearerAuth")
public class OrdenCompraController {

    private final OrdenCompraService ordenCompraService;

    public OrdenCompraController(OrdenCompraService ordenCompraService) {
        this.ordenCompraService = ordenCompraService;
    }

    @Operation(summary = "Listar órdenes de compra",
            description = "Filtro opcional por estado. " + IqOpenApiDocs.ROLE_ADMIN_AGENTE)
    @IqOpenApiDocs.SecuredAdminOrAgente
    @GetMapping
    public ResponseEntity<List<OrdenCompra>> listar(
            @Parameter(description = "BORRADOR, APROBADA, RECIBIDA o CANCELADA")
            @RequestParam(required = false) EstadoOrdenCompra estado) {
        return ResponseEntity.ok(ordenCompraService.listar(estado));
    }

    @Operation(summary = "Obtener una orden por ID", description = IqOpenApiDocs.ROLE_ADMIN_AGENTE)
    @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    @IqOpenApiDocs.SecuredAdminOrAgente
    @GetMapping("/{id}")
    public ResponseEntity<OrdenCompra> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ordenCompraService.obtenerPorId(id));
    }

    @Operation(summary = "Crear orden en BORRADOR",
            description = """
                    Crea una orden con estado inicial BORRADOR. El **total** se calcula en el servidor \
                    (precio del producto × cantidad). Usado por MCP/n8n (`crear_orden_borrador`).

                    """ + IqOpenApiDocs.ROLE_ADMIN_AGENTE)
    @ApiResponse(responseCode = "201", description = "Orden creada en BORRADOR")
    @ApiResponse(responseCode = "400", description = "Cantidad inválida o datos inconsistentes")
    @IqOpenApiDocs.SecuredAdminOrAgente
    @PostMapping
    public ResponseEntity<OrdenCompra> crear(@Valid @RequestBody OrdenCompraRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        return new ResponseEntity<>(ordenCompraService.crear(request, username), HttpStatus.CREATED);
    }

    @Operation(summary = "Generar PDF de la orden",
            description = """
                    Genera y persiste el PDF. Si la orden está en BORRADOR incluye marca de agua diagonal. \
                    Reemplaza un PDF previo si existía.

                    """ + IqOpenApiDocs.ROLE_ADMIN_AGENTE)
    @ApiResponse(responseCode = "200", description = "PDF generado (application/pdf)")
    @ApiResponse(responseCode = "404", description = "Orden no encontrada")
    @IqOpenApiDocs.SecuredAdminOrAgente
    @PostMapping("/{id}/pdf")
    public ResponseEntity<byte[]> generarPdf(@PathVariable Long id) {
        return pdfResponse(id, ordenCompraService.generarPdf(id));
    }

    @Operation(summary = "Descargar PDF guardado",
            description = """
                    Devuelve el PDF previamente generado. **404** si aún no se ha generado o fue invalidado \
                    al cambiar el estado de la orden.

                    """ + IqOpenApiDocs.ROLE_ADMIN_AGENTE)
    @ApiResponse(responseCode = "200", description = "PDF (application/pdf)")
    @ApiResponse(responseCode = "404", description = "PDF no generado o invalidado")
    @IqOpenApiDocs.SecuredAdminOrAgente
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> obtenerPdf(@PathVariable Long id) {
        return pdfResponse(id, ordenCompraService.obtenerPdf(id));
    }

    @Operation(summary = "Cambiar estado de la orden",
            description = """
                    Transiciones permitidas: BORRADOR→APROBADA|CANCELADA; APROBADA→RECIBIDA|CANCELADA. \
                    Al pasar a RECIBIDA se crea automáticamente un movimiento ENTRADA (misma transacción). \
                    El PDF guardado se elimina en cualquier cambio de estado.

                    Cuerpo exacto: `{ "estado": "APROBADA" }`.

                    """ + IqOpenApiDocs.ROLE_ADMIN)
    @ApiResponse(responseCode = "400", description = "Transición no permitida")
    @ApiResponse(responseCode = "403", description = "AGENTE u otro rol no autorizado")
    @IqOpenApiDocs.SecuredAdmin
    @PatchMapping("/{id}/estado")
    public ResponseEntity<OrdenCompra> cambiarEstado(@PathVariable Long id,
            @Valid @RequestBody CambioEstadoOrdenRequest request,
            Authentication authentication) {
        boolean esAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        return ResponseEntity.ok(ordenCompraService.cambiarEstado(id, request, authentication.getName(), esAdmin));
    }

    private ResponseEntity<byte[]> pdfResponse(Long id, byte[] pdf) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"orden-" + id + ".pdf\"")
                .body(pdf);
    }
}
