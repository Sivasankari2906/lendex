package com.example.backend.service;

import com.example.backend.entity.Borrower;
import com.example.backend.entity.Loan;
import com.example.backend.entity.Payment;
import com.example.backend.entity.User;
import com.example.backend.repository.BorrowerRepository;
import com.example.backend.repository.LoanRepository;
import com.example.backend.repository.PaymentRepository;
import com.example.backend.repository.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LoanService {
    private final LoanRepository loanRepo;
    private final UserRepository userRepo;
    private final BorrowerRepository borrowerRepo;
    private final PaymentRepository paymentRepo;

    public LoanService(LoanRepository loanRepo, UserRepository userRepo, BorrowerRepository borrowerRepo, PaymentRepository paymentRepo) {
        this.loanRepo = loanRepo;
        this.userRepo = userRepo;
        this.borrowerRepo = borrowerRepo;
        this.paymentRepo = paymentRepo;
    }

    @Transactional
    public Borrower addBorrower(Borrower b) {
        return borrowerRepo.save(b);
    }

    @Transactional
    public Loan addLoan(Long borrowerId, BigDecimal principal, Double monthlyInterestRate, LocalDate issuedDate, LocalDate trackingStartDate, String username) {
        Borrower borrower = borrowerRepo.findById(borrowerId).orElseThrow();
        if (!borrower.getOwner().getUsername().equals(username)) throw new AccessDeniedException("not-owner");
        Loan loan = Loan.builder()
                .principal(principal)
                .remainingPrincipal(principal)
                .monthlyInterestRate(monthlyInterestRate)
                .issuedDate(issuedDate)
                .trackingStartDate(trackingStartDate)
                .nextDueDate(trackingStartDate.plusMonths(1))
                .repaid(false)
                .borrower(borrower)
                .build();
        borrower.getLoans().add(loan);
        borrowerRepo.save(borrower);
        return loan;
    }

    public List<Loan> getAllLoans() {
        return loanRepo.findAll();
    }

    public List<Loan> getLoansForUser(String username) {
        User user = userRepo.findByUsername(username).orElseThrow();
        return loanRepo.findByBorrowerOwner(user);
    }

    public List<Loan> getDueLoans(LocalDate date) {
        return loanRepo.findByNextDueDateLessThanEqualAndRepaidFalse(date);
    }

    public List<Loan> getDueLoansForUser(String username, LocalDate date) {
        User user = userRepo.findByUsername(username).orElseThrow();
        return loanRepo.findByBorrowerOwnerAndNextDueDateLessThanEqualAndRepaidFalse(user, date);
    }

    public List<Borrower> getBorrowersForUser(String username) {
        User user = userRepo.findByUsername(username).orElseThrow();
        return borrowerRepo.findByOwner(user);
    }

    @Transactional
    public Borrower addBorrowerForUser(Borrower b, String username) {
        User user = userRepo.findByUsername(username).orElseThrow();
        b.setOwner(user);
        Borrower savedBorrower = borrowerRepo.save(b);
        return savedBorrower;
    }


    @Transactional
    public Payment recordPayment(Long loanId, BigDecimal amount, LocalDate date, String note, String username) {
        Loan loan = loanRepo.findById(loanId).orElseThrow();
        if (!loan.getBorrower().getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("not-owner");
        }
        Payment p = Payment.builder().amount(amount).date(date).note(note).loan(loan).build();
        paymentRepo.save(p);

        // Apply payment to interest first (for simplicity we treat payment as reducing principal)
        BigDecimal remaining = loan.getRemainingPrincipal().subtract(amount);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            loan.setRemainingPrincipal(BigDecimal.ZERO);
            loan.setRepaid(true);
        } else {
            loan.setRemainingPrincipal(remaining);
        }

        // advance due date by 1 month if not repaid
        if (!loan.isRepaid()) {
            loan.setNextDueDate(loan.getNextDueDate().plusMonths(1));
        }

        loanRepo.save(loan);
        return p;
    }

    public Loan getLoanForUser(Long loanId, String username) {
        Loan loan = loanRepo.findById(loanId).orElseThrow();
        if (!loan.getBorrower().getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("not-owner");
        }
        return loan;
    }

    public List<Payment> getPaymentsForLoan(Long loanId, String username) {
        Loan loan = loanRepo.findById(loanId).orElseThrow();
        if (!loan.getBorrower().getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("not-owner");
        }
        return paymentRepo.findByLoanOrderByDateDesc(loan);
    }

    @Transactional
    public Loan postponeLoanDueDate(Long loanId, LocalDate newDueDate, String username) {
        Loan loan = loanRepo.findById(loanId).orElseThrow();
        if (!loan.getBorrower().getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("not-owner");
        }
        loan.setNextDueDate(newDueDate);
        return loanRepo.save(loan);
    }

    @Transactional
    public Loan closeLoan(Long loanId, String username) {
        Loan loan = loanRepo.findById(loanId).orElseThrow();
        if (!loan.getBorrower().getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("not-owner");
        }
        loan.setRepaid(true);
        loan.setRemainingPrincipal(BigDecimal.ZERO);
        return loanRepo.save(loan);
    }

    public Map<String, String> getUserProfile(String username) {
        User user = userRepo.findByUsername(username).orElseThrow();
        Map<String, String> profile = new HashMap<>();
        profile.put("username", user.getUsername());
        profile.put("fullName", user.getFullName() != null ? user.getFullName() : "");
        profile.put("email", user.getEmail() != null ? user.getEmail() : "");
        profile.put("phone", user.getPhone() != null ? user.getPhone() : "");
        return profile;
    }

    @Transactional
    public Map<String, String> updateUserProfile(Map<String, String> updates, String username) {
        User user = userRepo.findByUsername(username).orElseThrow();
        if (updates.containsKey("fullName")) {
            user.setFullName(updates.get("fullName"));
        }
        if (updates.containsKey("email")) {
            user.setEmail(updates.get("email"));
        }
        if (updates.containsKey("phone")) {
            user.setPhone(updates.get("phone"));
        }
        userRepo.save(user);
        return getUserProfile(username);
    }

    @Transactional
    public Loan updateLoan(Long loanId, BigDecimal principal, Double monthlyInterestRate, String username) {
        Loan loan = loanRepo.findById(loanId).orElseThrow();
        if (!loan.getBorrower().getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("not-owner");
        }
        loan.setPrincipal(principal);
        loan.setRemainingPrincipal(principal);
        loan.setMonthlyInterestRate(monthlyInterestRate);
        return loanRepo.save(loan);
    }

    @Transactional
    public void deleteLoan(Long loanId, String username) {
        Loan loan = loanRepo.findById(loanId).orElseThrow();
        if (!loan.getBorrower().getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("not-owner");
        }
        loanRepo.delete(loan);
    }

    @Transactional
    public Borrower updateBorrower(Long borrowerId, Borrower updates, String username) {
        Borrower borrower = borrowerRepo.findById(borrowerId).orElseThrow();
        if (!borrower.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("not-owner");
        }
        borrower.setName(updates.getName());
        borrower.setPhone(updates.getPhone());
        return borrowerRepo.save(borrower);
    }

    @Transactional
    public void deleteBorrower(Long borrowerId, String username) {
        Borrower borrower = borrowerRepo.findById(borrowerId).orElseThrow();
        if (!borrower.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("not-owner");
        }
        borrowerRepo.delete(borrower);
    }

    @Transactional
    public void postponeNotification(Long loanId, int days, String username) {
        Loan loan = loanRepo.findById(loanId).orElseThrow();
        if (!loan.getBorrower().getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("not-owner");
        }
        
        // Set notification postponement without changing due date
        LocalDate postponedUntil = LocalDate.now().plusDays(days);
        loan.setNotificationPostponedUntil(postponedUntil);
        loanRepo.save(loan);
    }

    public List<Loan> getDueLoansForUserExcludingPostponed(String username, LocalDate date) {
        User user = userRepo.findByUsername(username).orElseThrow();
        List<Loan> allLoans = loanRepo.findByBorrowerOwnerAndRepaidFalse(user);
        
        return allLoans.stream().filter(loan -> {
            return isLoanOverdue(loan, date);
        }).collect(java.util.stream.Collectors.toList());
    }
    
    private boolean isLoanOverdue(Loan loan, LocalDate date) {
        LocalDate trackingStart = loan.getTrackingStartDate() != null ? 
            loan.getTrackingStartDate() : loan.getIssuedDate();
        
        // Check if notification is postponed
        if (loan.getNotificationPostponedUntil() != null && 
            loan.getNotificationPostponedUntil().isAfter(date)) {
            return false;
        }
        
        // Check if loan has overdue payments
        LocalDate currentMonth = trackingStart;
        double monthlyInterest = loan.getRemainingPrincipal().doubleValue() * loan.getMonthlyInterestRate() / 100;
        
        while (currentMonth.isBefore(date.withDayOfMonth(1))) {
            // Check total payments for this month
            final LocalDate monthToCheck = currentMonth;
            double totalPaid = paymentRepo.findByLoanOrderByDateDesc(loan).stream()
                .filter(payment -> 
                    payment.getDate().getYear() == monthToCheck.getYear() &&
                    payment.getDate().getMonth() == monthToCheck.getMonth())
                .mapToDouble(payment -> payment.getAmount().doubleValue())
                .sum();
            
            if (totalPaid < monthlyInterest) {
                long monthsOverdue = java.time.temporal.ChronoUnit.MONTHS.between(currentMonth, date);
                // If overdue > 1 month, always notify. If < 1 month, only notify after due date
                if (monthsOverdue > 1 || currentMonth.plusMonths(1).isBefore(date)) {
                    return true;
                }
            }
            
            currentMonth = currentMonth.plusMonths(1);
        }
        
        return false;
    }
}
