package com.example.backend.controller;

import com.example.backend.entity.Borrower;
import com.example.backend.entity.EMI;
import com.example.backend.entity.Loan;
import com.example.backend.entity.Payment;
import com.example.backend.service.LoanService;
import com.example.backend.service.NotificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class LoanController {
    private final LoanService loanService;
    private final NotificationService notificationService;

    public LoanController(LoanService loanService, NotificationService notificationService) {
        this.loanService = loanService;
        this.notificationService = notificationService;
    }

    @GetMapping("/borrowers")
    public ResponseEntity<List<Map<String, Object>>> getBorrowers(Principal principal) {
        String username = principal.getName();
        return ResponseEntity.ok(loanService.getBorrowersWithLoanCount(username));
    }

    @PostMapping("/borrowers")
    public ResponseEntity<Borrower> createBorrower(@RequestBody Borrower b, Principal principal) {
        String username = principal.getName();
        return ResponseEntity.ok(loanService.addBorrowerForUser(b, username));
    }


    @PostMapping("/borrowers/{id}/loans")
    public ResponseEntity<Loan> addLoan(@PathVariable Long id, @RequestBody Map<String,String> body, Principal principal){
        BigDecimal principal_amount = new BigDecimal(body.get("principal"));
        Double rate = Double.valueOf(body.get("monthlyInterestRate"));
        LocalDate issued = LocalDate.parse(body.getOrDefault("issuedDate", LocalDate.now().toString()));
        LocalDate trackingStart = LocalDate.parse(body.getOrDefault("trackingStartDate", issued.toString()));
        String remarks = body.getOrDefault("remarks", "");
        return ResponseEntity.ok(loanService.addLoan(id, principal_amount, rate, issued, trackingStart, remarks, principal.getName()));
    }

    @GetMapping("/loans")
    public ResponseEntity<List<Loan>> allLoans(Principal principal){
        String username = principal.getName();
        return ResponseEntity.ok(loanService.getLoansForUser(username));
    }

    @GetMapping("/reminders")
    public ResponseEntity<List<Loan>> reminders(Principal principal){
        String username = principal.getName();
        return ResponseEntity.ok(loanService.getDueLoansForUserExcludingPostponed(username, LocalDate.now()));
    }

    @GetMapping("/emi-reminders")
    public ResponseEntity<List<EMI>> getEmiReminders(Principal principal) {
        String username = principal.getName();
        return ResponseEntity.ok(loanService.getOverdueEmisForUser(username, LocalDate.now()));
    }

    @PostMapping("/loans/{loanId}/payments")
    public ResponseEntity<Payment> recordPayment(@PathVariable Long loanId, @RequestBody Map<String,String> body, Principal principal){
        BigDecimal amount = new BigDecimal(body.get("amount"));
        LocalDate userDate = LocalDate.parse(body.getOrDefault("date", LocalDate.now().toString()));
        String note = body.getOrDefault("note", "");
        String remarks = body.getOrDefault("remarks", "");
        return ResponseEntity.ok(loanService.recordPayment(loanId, amount, userDate, note, remarks, principal.getName()));
    }

    @GetMapping("/loans/{loanId}")
    public ResponseEntity<Loan> getLoan(@PathVariable Long loanId, Principal principal){
        return ResponseEntity.ok(loanService.getLoanForUser(loanId, principal.getName()));
    }

    @GetMapping("/loans/{loanId}/payments")
    public ResponseEntity<List<Payment>> getLoanPayments(@PathVariable Long loanId, Principal principal){
        return ResponseEntity.ok(loanService.getPaymentsForLoan(loanId, principal.getName()));
    }

    @PostMapping("/loans/{loanId}/postpone")
    public ResponseEntity<Map<String,String>> postpone(@PathVariable Long loanId, @RequestBody Map<String,String> body, Principal principal){
        int days = Integer.parseInt(body.get("days"));
        loanService.postponeNotification(loanId, days, principal.getName());
        Map<String,String> response = new java.util.HashMap<>();
        response.put("message", "Notification postponed for " + days + " days");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/loans/{loanId}/close")
    public ResponseEntity<Loan> closeLoan(@PathVariable Long loanId, Principal principal){
        return ResponseEntity.ok(loanService.closeLoan(loanId, principal.getName()));
    }

    @GetMapping("/user/profile")
    public ResponseEntity<Map<String,String>> getUserProfile(Principal principal){
        return ResponseEntity.ok(loanService.getUserProfile(principal.getName()));
    }

    @PutMapping("/user/profile")
    public ResponseEntity<Map<String,String>> updateUserProfile(@RequestBody Map<String,String> body, Principal principal){
        return ResponseEntity.ok(loanService.updateUserProfile(body, principal.getName()));
    }

    @PutMapping("/loans/{loanId}")
    public ResponseEntity<Loan> updateLoan(@PathVariable Long loanId, @RequestBody Map<String,String> body, Principal principal){
        BigDecimal principal_amount = new BigDecimal(body.get("principal"));
        Double rate = Double.valueOf(body.get("monthlyInterestRate"));
        return ResponseEntity.ok(loanService.updateLoan(loanId, principal_amount, rate, principal.getName()));
    }



    @PutMapping("/borrowers/{borrowerId}")
    public ResponseEntity<Borrower> updateBorrower(@PathVariable Long borrowerId, @RequestBody Borrower borrower, Principal principal){
        return ResponseEntity.ok(loanService.updateBorrower(borrowerId, borrower, principal.getName()));
    }

    @DeleteMapping("/borrowers/{borrowerId}")
    public ResponseEntity<Void> deleteBorrower(@PathVariable Long borrowerId, Principal principal){
        loanService.deleteBorrower(borrowerId, principal.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<com.example.backend.entity.Notification>> getNotifications(Principal principal){
        return ResponseEntity.ok(notificationService.getNotificationsForUser(principal.getName()));
    }
}
