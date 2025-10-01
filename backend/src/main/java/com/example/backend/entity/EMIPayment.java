package com.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
public class EMIPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;
    private LocalDate date; // Month assignment date
    private LocalDate paymentDate; // Actual payment date
    private String note;
    private String emiMonth; // Format: "2024-06"

    @ManyToOne
    @JoinColumn(name = "emi_id")
    @JsonBackReference
    private EMI emi;
}