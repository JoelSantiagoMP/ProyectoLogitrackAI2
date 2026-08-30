package com.example.logitrack.config;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Respuestas HTTP comunes documentadas en Swagger para endpoints IQ.
 */
public final class IqOpenApiDocs {

    public static final String TAG_KPIS = "IQ - KPIs e inventario";
    public static final String TAG_ORDENES = "IQ - Órdenes de compra";
    public static final String TAG_PANEL = "IQ - Panel operativo";
    public static final String TAG_PROVEEDORES = "IQ - Proveedores";
    public static final String TAG_AUTH = "Autenticación";

    public static final String ROLE_ADMIN_AGENTE =
            "Requiere JWT Bearer con rol **ADMIN** o **AGENTE** (botón Authorize en Swagger).";
    public static final String ROLE_ADMIN =
            "Requiere JWT Bearer con rol **ADMIN** únicamente.";
    public static final String ROLE_AUTHENTICATED =
            "Requiere JWT Bearer (cualquier usuario autenticado).";

    private IqOpenApiDocs() {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operación exitosa"),
            @ApiResponse(responseCode = "401", description = "Sesión no válida o token ausente"),
            @ApiResponse(responseCode = "403", description = "Acción prohibida para el rol del usuario")
    })
    public @interface SecuredAdminOrAgente {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operación exitosa"),
            @ApiResponse(responseCode = "400", description = "Validación o transición de estado inválida"),
            @ApiResponse(responseCode = "401", description = "Sesión no válida o token ausente"),
            @ApiResponse(responseCode = "403", description = "Solo ADMIN puede ejecutar esta acción"),
            @ApiResponse(responseCode = "404", description = "Recurso no encontrado")
    })
    public @interface SecuredAdmin {
    }
}
