package com.example.backend.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.Loan;
import com.example.backend.entity.User;

import java.time.LocalDate;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByNextDueDateLessThanEqualAndRepaidFalse(LocalDate date);
    List<Loan> findByBorrowerOwner(User owner);
    List<Loan> findByBorrowerOwnerAndNextDueDateLessThanEqualAndRepaidFalse(User owner, LocalDate date);
    List<Loan> findByBorrowerOwnerAndRepaidFalse(User owner);
    List<Loan> findByRepaidFalse();
}
