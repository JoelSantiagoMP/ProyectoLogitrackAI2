package com.example.logitrack.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("LogiTrack API — Torre de control IQ")
                        .description("""
                                API REST de LogiTrack S.A. Extiende el reto anterior con KPIs de inventario, \
                                productos en riesgo, órdenes de compra, panel operativo (n8n/MCP) y PDF de órdenes.

                                **Autenticación:** use `POST /auth/login` y pegue el token en **Authorize** (Bearer JWT).

                                **Roles IQ:** AGENTE consulta KPIs/riesgo, crea BORRADOR y publica resumen. \
                                ADMIN además aprueba, recibe o cancela órdenes y registra movimientos.

                                Rutas documentadas bajo `/api/*`. Alias sin prefijo (`/kpis`, `/ordenes`, etc.) \
                                siguen activos en runtime pero no se listan aquí para evitar duplicados.""")
                        .version("1.0.0")
                        .contact(new Contact().name("LogiTrack S.A.")))
                .tags(List.of(
                        new Tag().name(IqOpenApiDocs.TAG_AUTH)
                                .description("Login JWT reutilizado del reto anterior."),
                        new Tag().name(IqOpenApiDocs.TAG_KPIS)
                                .description("Indicadores, stock, riesgo y bodegas críticas (ADMIN o AGENTE)."),
                        new Tag().name(IqOpenApiDocs.TAG_ORDENES)
                                .description("Órdenes de compra y PDF. Crear BORRADOR: ADMIN/AGENTE. Cambiar estado: solo ADMIN."),
                        new Tag().name(IqOpenApiDocs.TAG_PANEL)
                                .description("Resumen estructurado publicado por n8n (ADMIN o AGENTE)."),
                        new Tag().name(IqOpenApiDocs.TAG_PROVEEDORES)
                                .description("Proveedores precargados para órdenes automáticas.")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Token obtenido en POST /auth/login")));
    }
}
