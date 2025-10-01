package com.example.backend.repository;

import com.example.backend.entity.Notification;
import com.example.backend.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByLoanBorrowerOwnerUsernameAndSentFalse(String username);
    List<Notification> findByLoanBorrowerOwnerUsername(String username);
    Optional<Notification> findByLoanAndDueDate(Loan loan, LocalDate dueDate);
    List<Notification> findByLoanAndSentDate(Loan loan, LocalDate sentDate);
    List<Notification> findByDueDateLessThanEqualAndSentFalse(LocalDate date);
}