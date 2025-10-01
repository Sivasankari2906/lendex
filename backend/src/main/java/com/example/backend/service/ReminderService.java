package com.example.backend.service;

import com.example.backend.entity.Loan;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class ReminderService {
    private final LoanService loanService;

    public ReminderService(LoanService loanService) {
        this.loanService = loanService;
    }

    // run every day at 08:00 server time to find due loans
    @Scheduled(cron = "0 0 8 * * *")
    public void checkDueLoans() {
        LocalDate today = LocalDate.now();
        List<Loan> due = loanService.getDueLoans(today);
        // For demo: we simply log the reminders. In production, integrate email/FCM/push.
        if (!due.isEmpty()) {
            due.forEach(loan -> {
                System.out.println("[REMINDER] Loan id=" + loan.getId() + " borrower=" + loan.getBorrower().getName()
                        + " due on " + loan.getNextDueDate());
            });
        }
    }
}
