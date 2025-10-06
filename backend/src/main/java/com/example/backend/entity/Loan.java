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
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // principal amount lent
    private BigDecimal principal;

    // monthly interest rate in percentage (e.g., 2.5)
    private Double monthlyInterestRate;

    // date when loan was issued
    private LocalDate issuedDate;

    // date from which to start tracking payments
    private LocalDate trackingStartDate;

    // next due date for interest (monthly)
    private LocalDate nextDueDate;

    // notification postponement until date
    private LocalDate notificationPostponedUntil;

    // amount remaining to be repaid (principal only)
    private BigDecimal remainingPrincipal;

    // if fully repaid, true
    private boolean repaid;

    // remarks or notes about the loan
    private String remarks;

    @ManyToOne
    @JoinColumn(name = "borrower_id")
    private Borrower borrower;
}
