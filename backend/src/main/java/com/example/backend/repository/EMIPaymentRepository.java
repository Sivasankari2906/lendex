package com.example.backend.repository;

import com.example.backend.entity.EMI;
import com.example.backend.entity.EMIPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EMIPaymentRepository extends JpaRepository<EMIPayment, Long> {
    List<EMIPayment> findByEmiOrderByDateDesc(EMI emi);
}