package com.example.logitrack.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/auth/**", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/ordenes/**", "/ordenes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/movimientos", "/api/movimientos/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/proveedores", "/proveedores")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/proveedores", "/proveedores")
                        .authenticated()
                        .requestMatchers("/api/kpis", "/kpis", "/api/proveedores", "/proveedores",
                                "/api/ordenes", "/api/ordenes/**", "/ordenes", "/ordenes/**",
                                "/api/panel/**", "/panel/**")
                        .hasAnyRole("ADMIN", "AGENTE")
                        .requestMatchers(HttpMethod.GET, "/api/productos/riesgo", "/productos/riesgo",
                                "/api/productos/*/stock", "/productos/*/stock",
                                "/api/bodegas/criticas", "/bodegas/criticas")
                        .hasAnyRole("ADMIN", "AGENTE")
                        .requestMatchers("/api/bodegas/**", "/api/productos/**", "/bodegas/**", "/productos/**")
                        .authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setCharacterEncoding("UTF-8");
                            response.setContentType("application/json;charset=UTF-8");
                            Map<String, Object> body = Map.of(
                                    "status", 403,
                                    "error", "Forbidden",
                                    "message", "No tiene permiso para esta acción");
                            try {
                                response.getWriter().write(JSON.writeValueAsString(body));
                            } catch (Exception ignored) {
                                response.getWriter().write(
                                        "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"No tiene permiso para esta accion\"}");
                            }
                        }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Incluimos las URLs habituales de Live Server y entornos de desarrollo locales
        config.setAllowedOrigins(List.of(
                "http://localhost:5500",
                "http://127.0.0.1:5500",
                "http://localhost:5173",
                "http://localhost:3000",
                "http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}