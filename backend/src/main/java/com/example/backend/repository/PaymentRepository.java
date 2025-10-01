package com.example.backend.repository;

import com.example.backend.entity.Loan;
import com.example.backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByLoanOrderByDateDesc(Loan loan);
    void deleteByLoan(Loan loan);
}
