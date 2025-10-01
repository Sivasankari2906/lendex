package com.example.backend.repository;

import com.example.backend.entity.EMI;
import com.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EMIRepository extends JpaRepository<EMI, Long> {
    List<EMI> findByUser(User user);
    List<EMI> findByCompletedFalse();
}