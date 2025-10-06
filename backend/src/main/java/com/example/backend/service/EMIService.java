package com.example.backend.service;

import com.example.backend.entity.EMI;
import com.example.backend.entity.EMIPayment;
import com.example.backend.entity.User;
import com.example.backend.repository.EMIRepository;
import com.example.backend.repository.EMIPaymentRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class EMIService {
    private final EMIRepository emiRepo;
    private final UserRepository userRepo;
    private final EMIPaymentRepository emiPaymentRepo;

    public EMIService(EMIRepository emiRepo, UserRepository userRepo, EMIPaymentRepository emiPaymentRepo) {
        this.emiRepo = emiRepo;
        this.userRepo = userRepo;
        this.emiPaymentRepo = emiPaymentRepo;
    }

    public List<EMI> getEmisForUser(String username) {
        User user = userRepo.findByUsername(username).orElseThrow();
        return emiRepo.findByUser(user);
    }

    @Transactional
    public EMI createEmi(String borrowerName, BigDecimal totalAmount, BigDecimal givenInCash, 
                        LocalDate givenDate, Integer tenure, BigDecimal emiAmount, LocalDate startDate, String remarks, String username) {
        User user = userRepo.findByUsername(username).orElseThrow();

        EMI emiEntity = EMI.builder()
                .borrowerName(borrowerName)
                .totalAmount(totalAmount)
                .givenInCash(givenInCash)
                .givenDate(givenDate)
                .tenure(tenure)
                .emiAmount(emiAmount)
                .startDate(startDate)
                .completed(false)
                .remarks(remarks)
                .user(user)
                .build();

        return emiRepo.save(emiEntity);
    }

    @Transactional
    public void deleteEmi(Long emiId, String username) {
        EMI emi = emiRepo.findById(emiId).orElseThrow();
        if (!emi.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("not-owner");
        }
        emiRepo.delete(emi);
    }

    @Transactional
    public EMIPayment recordEmiPayment(Long emiId, BigDecimal amount, LocalDate monthDate, String note, String remarks, String username) {
        EMI emi = emiRepo.findById(emiId).orElseThrow();
        if (!emi.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("not-owner");
        }
        
        // Validate EMI payment amount
        validateEmiPaymentAmount(emi, amount, monthDate);
        
        String emiMonth = monthDate.getYear() + "-" + String.format("%02d", monthDate.getMonthValue());
        
        EMIPayment payment = EMIPayment.builder()
            .amount(amount)
            .date(monthDate)
            .paymentDate(LocalDate.now())
            .emiMonth(emiMonth)
            .note(note)
            .remarks(remarks)
            .emi(emi)
            .build();
        
        EMIPayment savedPayment = emiPaymentRepo.save(payment);
        
        // Check if EMI should be closed
        checkAndCloseEmi(emi);
        
        return savedPayment;
    }
    
    private void validateEmiPaymentAmount(EMI emi, BigDecimal amount, LocalDate monthDate) {
        double monthlyEmi = emi.getEmiAmount().doubleValue();
        
        // Get existing payments for this month
        double totalPaidThisMonth = emiPaymentRepo.findByEmiOrderByDateDesc(emi).stream()
            .filter(payment -> 
                payment.getDate().getYear() == monthDate.getYear() &&
                payment.getDate().getMonth() == monthDate.getMonth())
            .mapToDouble(payment -> payment.getAmount().doubleValue())
            .sum();
        
        double remainingForMonth = monthlyEmi - totalPaidThisMonth;
        
        // Allow overpayment but log a warning
        if (amount.doubleValue() > remainingForMonth && remainingForMonth > 0) {
            System.out.println(String.format("Warning: EMI payment of %.2f exceeds remaining EMI of %.2f for month %s", 
                amount.doubleValue(), remainingForMonth, monthDate.toString().substring(0, 7)));
        }
    }

    public List<EMIPayment> getEmiPayments(Long emiId, String username) {
        EMI emi = emiRepo.findById(emiId).orElseThrow();
        if (!emi.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("not-owner");
        }
        return emiPaymentRepo.findByEmiOrderByDateDesc(emi);
    }

    @Transactional
    public EMI closeEmi(Long emiId, String username) {
        EMI emi = emiRepo.findById(emiId).orElseThrow();
        if (!emi.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("not-owner");
        }
        emi.setCompleted(true);
        return emiRepo.save(emi);
    }

    @Transactional
    public EMI updateEmi(Long emiId, String borrowerName, BigDecimal totalAmount, BigDecimal givenInCash,
                        LocalDate givenDate, Integer tenure, BigDecimal emiAmount, LocalDate startDate, String remarks, String username) {
        EMI emi = emiRepo.findById(emiId).orElseThrow();
        if (!emi.getUser().getUsername().equals(username)) {
            throw new AccessDeniedException("not-owner");
        }
        
        emi.setBorrowerName(borrowerName);
        emi.setTotalAmount(totalAmount);
        emi.setGivenInCash(givenInCash);
        emi.setGivenDate(givenDate);
        emi.setTenure(tenure);
        emi.setEmiAmount(emiAmount);
        emi.setStartDate(startDate);
        emi.setRemarks(remarks);
        
        return emiRepo.save(emi);
    }

    private void checkAndCloseEmi(EMI emi) {
        // Check if all months are paid
        LocalDate currentMonth = emi.getStartDate();
        List<EMIPayment> payments = emiPaymentRepo.findByEmiOrderByDateDesc(emi);
        int paidMonths = 0;
        
        for (int i = 0; i < emi.getTenure(); i++) {
            String monthKey = currentMonth.getYear() + "-" + String.format("%02d", currentMonth.getMonthValue());
            boolean monthPaid = payments.stream()
                .anyMatch(p -> p.getEmiMonth().equals(monthKey) && 
                         p.getAmount().compareTo(emi.getEmiAmount()) >= 0);
            if (monthPaid) paidMonths++;
            currentMonth = currentMonth.plusMonths(1);
        }
        
        if (paidMonths >= emi.getTenure() && !emi.isCompleted()) {
            emi.setCompleted(true);
            emiRepo.save(emi);
        }
    }
}