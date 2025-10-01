package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EMI {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String borrowerName; // e.g., "Vinoth Axis"
    private BigDecimal totalAmount; // e.g., 60000
    private BigDecimal givenInCash; // e.g., 57000
    private LocalDate givenDate; // e.g., 29/9/25
    private Integer tenure; // e.g., 12 months
    private BigDecimal emiAmount; // Manual entry
    private LocalDate startDate; // e.g., Oct 1
    private boolean completed;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}