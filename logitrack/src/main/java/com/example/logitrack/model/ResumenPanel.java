package com.example.logitrack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "resumen_panel", uniqueConstraints = {
        @UniqueConstraint(name = "uk_resumen_panel_fecha", columnNames = "fecha")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumenPanel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La fecha del resumen es obligatoria")
    @Column(nullable = false)
    private LocalDate fecha;

    @NotBlank(message = "El contenido del resumen es obligatorio")
    @Column(name = "contenido_json", nullable = false, columnDefinition = "TEXT")
    private String contenidoJson;

    @NotNull(message = "El autor es obligatorio")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;
}
