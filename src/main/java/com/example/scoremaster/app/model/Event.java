package com.example.scoremaster.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    @NotBlank
    private LocalDate startDate;

    @Column(nullable = false)
    @NotBlank
    private LocalDate endDate;

    @Column(nullable = false)
    @NotBlank
    private String location;

    @Column(nullable = false)
    @NotBlank
    private String organizer;
}
