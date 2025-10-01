package com.example.backend.service;

import com.example.backend.entity.Borrower;
import com.example.backend.entity.EMI;
import com.example.backend.entity.EMIPayment;
import com.example.backend.entity.Loan;
import com.example.backend.entity.Payment;
import com.example.backend.entity.User;
import com.example.backend.repository.BorrowerRepository;
import com.example.backend.repository.EMIRepository;
import com.example.backend.repository.EMIPaymentRepository;
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
    private final EMIRepository emiRepo;
    private final EMIPaymentRepository emiPaymentRepo;

    public LoanService(LoanRepository loanRepo, UserRepository userRepo, BorrowerRepository borrowerRepo, PaymentRepository paymentRepo, EMIRepository emiRepo, EMIPaymentRepository emiPaymentRepo) {
        this.loanRepo = loanRepo;
        this.userRepo = userRepo;
        this.borrowerRepo = borrowerRepo;
        this.paymentRepo = paymentRepo;
        this.emiRepo = emiRepo;
        this.emiPaymentRepo = emiPaymentRepo;
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
    
    public List<Map<String, Object>> getBorrowersWithLoanCount(String username) {
        User user = userRepo.findByUsername(username).orElseThrow();
        List<Borrower> borrowers = borrowerRepo.findByOwner(user);
        return borrowers.stream().map(borrower -> {
            Map<String, Object> borrowerData = new HashMap<>();
            borrowerData.put("id", borrower.getId());
            borrowerData.put("name", borrower.getName());
            borrowerData.put("phone", borrower.getPhone());
            borrowerData.put("loanCount", borrower.getLoans().size());
            return borrowerData;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public Borrower addBorrowerForUser(Borrower b, String username) {
        User user = userRepo.findByUsername(username).orElseThrow();
        b.setOwner(user);
        Borrower savedBorrower = borrowerRepo.save(b);
        return savedBorrower;
    }


    @Transactional
    public Payment recordPayment(Long loanId, BigDecimal amount, LocalDate monthDate, String note, String username) {
        Loan loan = loanRepo.findById(loanId).orElseThrow();
        if (!loan.getBorrower().getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("not-owner");
        }
        
        // Extract actual payment date from note if present, otherwise use today
        LocalDate actualPaymentDate = LocalDate.now();
        if (note.contains("Recorded on ")) {
            try {
                String dateStr = note.substring(note.indexOf("Recorded on ") + 12, note.indexOf("Recorded on ") + 22);
                actualPaymentDate = LocalDate.parse(dateStr);
            } catch (Exception e) {
                // Use default if parsing fails
            }
        }
        
        // Format interest month as "YYYY-MM"
        String interestMonth = monthDate.getYear() + "-" + String.format("%02d", monthDate.getMonthValue());
        
        Payment p = Payment.builder()
            .amount(amount)
            .date(monthDate) // Month assignment date (for filtering)
            .paymentDate(actualPaymentDate) // Actual payment date (for display)
            .interestMonth(interestMonth)
            .note(note)
            .loan(loan)
            .build();
        paymentRepo.save(p);

        // DO NOT modify principal - payments only track interest
        // Principal remains unchanged and can only be modified by user editing the loan
        
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
        // Keep remainingPrincipal same as principal - only user can change principal amount
        loan.setRemainingPrincipal(principal);
        loan.setMonthlyInterestRate(monthlyInterestRate);
        return loanRepo.save(loan);
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

    public List<EMI> getOverdueEmisForUser(String username, LocalDate date) {
        User user = userRepo.findByUsername(username).orElseThrow();
        List<EMI> allEmis = emiRepo.findByUser(user);
        
        return allEmis.stream().filter(emi -> {
            return isEmiOverdue(emi, date);
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

    private boolean isEmiOverdue(EMI emi, LocalDate date) {
        if (emi.isCompleted()) return false;
        
        LocalDate currentMonth = emi.getStartDate();
        
        for (int i = 0; i < emi.getTenure(); i++) {
            if (currentMonth.isAfter(date)) break;
            
            final LocalDate monthToCheck = currentMonth;
            double totalPaid = emiPaymentRepo.findByEmiOrderByDateDesc(emi).stream()
                .filter(payment -> 
                    payment.getDate().getYear() == monthToCheck.getYear() &&
                    payment.getDate().getMonth() == monthToCheck.getMonth())
                .mapToDouble(payment -> payment.getAmount().doubleValue())
                .sum();
            
            if (totalPaid < emi.getEmiAmount().doubleValue()) {
                if (currentMonth.plusMonths(1).isBefore(date)) {
                    return true;
                }
            }
            
            currentMonth = currentMonth.plusMonths(1);
        }
        
        return false;
    }
}
