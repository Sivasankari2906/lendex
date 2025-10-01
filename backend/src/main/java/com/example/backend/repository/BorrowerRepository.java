package com.example.backend.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.Borrower;
import com.example.backend.entity.User;

import java.util.List;

public interface BorrowerRepository extends JpaRepository<Borrower, Long> {
    List<Borrower> findByOwner(User owner);
}
