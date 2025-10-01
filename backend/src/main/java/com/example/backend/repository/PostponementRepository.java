package com.example.backend.repository;

import com.example.backend.entity.Postponement;
import com.example.backend.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PostponementRepository extends JpaRepository<Postponement, Long> {
    Optional<Postponement> findByLoanAndOriginalDueDate(Loan loan, LocalDate originalDueDate);
    List<Postponement> findByPostponedUntilGreaterThanEqual(LocalDate date);
}