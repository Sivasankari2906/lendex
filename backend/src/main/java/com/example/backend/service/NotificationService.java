package com.example.backend.service;

import com.example.backend.entity.EMI;
import com.example.backend.entity.Loan;
import com.example.backend.entity.Notification;
import com.example.backend.repository.EMIRepository;
import com.example.backend.repository.EMIPaymentRepository;
import com.example.backend.repository.LoanRepository;
import com.example.backend.repository.NotificationRepository;
import com.example.backend.repository.PaymentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepo;
    private final LoanRepository loanRepo;
    private final EMIRepository emiRepo;
    private final PaymentRepository paymentRepo;
    private final EMIPaymentRepository emiPaymentRepo;
    private final SmsService smsService;
    private final EmailService emailService;

    public NotificationService(NotificationRepository notificationRepo, LoanRepository loanRepo, EMIRepository emiRepo, PaymentRepository paymentRepo, EMIPaymentRepository emiPaymentRepo, SmsService smsService, EmailService emailService) {
        this.notificationRepo = notificationRepo;
        this.loanRepo = loanRepo;
        this.emiRepo = emiRepo;
        this.paymentRepo = paymentRepo;
        this.emiPaymentRepo = emiPaymentRepo;
        this.smsService = smsService;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 9 * * *") // Daily at 9 AM
    @Transactional
    public void checkOverduePayments() {
        LocalDate today = LocalDate.now();
        List<Loan> activeLoans = loanRepo.findByRepaidFalse();

        for (Loan loan : activeLoans) {
            checkLoanOverduePayments(loan, today);
        }
        
        // Check overdue EMIs
        List<EMI> activeEmis = emiRepo.findByCompletedFalse();
        for (EMI emi : activeEmis) {
            checkEmiOverduePayments(emi, today);
        }
    }

    private void checkLoanOverduePayments(Loan loan, LocalDate today) {
        LocalDate trackingStart = loan.getTrackingStartDate() != null ? 
            loan.getTrackingStartDate() : loan.getIssuedDate();
        
        LocalDate currentMonth = trackingStart;
        int totalOverdueMonths = 0;
        LocalDate firstOverdueMonth = null;
        
        while (currentMonth.isBefore(today.withDayOfMonth(1))) {
            // Check if payment exists for this month
            final LocalDate monthToCheck = currentMonth;
            boolean hasPayment = paymentRepo.findByLoanOrderByDateDesc(loan).stream()
                .anyMatch(payment -> 
                    payment.getDate().getYear() == monthToCheck.getYear() &&
                    payment.getDate().getMonth() == monthToCheck.getMonth());
            
            if (!hasPayment) {
                if (firstOverdueMonth == null) {
                    firstOverdueMonth = currentMonth;
                }
                totalOverdueMonths++;
            }
            
            currentMonth = currentMonth.plusMonths(1);
        }
        
        if (totalOverdueMonths > 0 && firstOverdueMonth != null) {
            // Check if notification already sent for this overdue period
            String notificationKey = loan.getId() + "-" + firstOverdueMonth.toString();
            if (notificationRepo.findByLoanAndDueDate(loan, firstOverdueMonth).isEmpty()) {
                
                Notification notification = Notification.builder()
                    .loan(loan)
                    .dueDate(firstOverdueMonth)
                    .sentDate(today)
                    .daysPastDue(totalOverdueMonths * 30) // Approximate days
                    .message(String.format("Collect %d months overdue payments from %s starting from %s - Total: ₹%.2f", 
                        totalOverdueMonths, loan.getBorrower().getName(), 
                        firstOverdueMonth.getMonth().name(),
                        totalOverdueMonths * loan.getRemainingPrincipal().doubleValue() * loan.getMonthlyInterestRate() / 100))
                    .sent(true)
                    .build();
                
                notificationRepo.save(notification);
                sendNotification(notification);
            }
        }
    }

    private void checkEmiOverduePayments(EMI emi, LocalDate today) {
        LocalDate currentMonth = emi.getStartDate();
        int totalOverdueMonths = 0;
        LocalDate firstOverdueMonth = null;
        
        for (int i = 0; i < emi.getTenure(); i++) {
            if (currentMonth.isAfter(today)) break;
            
            final LocalDate monthToCheck = currentMonth;
            double totalPaid = emiPaymentRepo.findByEmiOrderByDateDesc(emi).stream()
                .filter(payment -> 
                    payment.getDate().getYear() == monthToCheck.getYear() &&
                    payment.getDate().getMonth() == monthToCheck.getMonth())
                .mapToDouble(payment -> payment.getAmount().doubleValue())
                .sum();
            
            if (totalPaid < emi.getEmiAmount().doubleValue()) {
                if (firstOverdueMonth == null) {
                    firstOverdueMonth = currentMonth;
                }
                totalOverdueMonths++;
            }
            
            currentMonth = currentMonth.plusMonths(1);
        }
        
        if (totalOverdueMonths > 0 && firstOverdueMonth != null) {
            if (notificationRepo.findByEmiAndDueDate(emi, firstOverdueMonth).isEmpty()) {
                
                Notification notification = Notification.builder()
                    .emi(emi)
                    .dueDate(firstOverdueMonth)
                    .sentDate(today)
                    .daysPastDue(totalOverdueMonths * 30)
                    .message(String.format("Collect %d months overdue EMI from %s starting from %s - Total: ₹%.2f", 
                        totalOverdueMonths, emi.getBorrowerName(), 
                        firstOverdueMonth.getMonth().name(),
                        totalOverdueMonths * emi.getEmiAmount().doubleValue()))
                    .sent(true)
                    .build();
                
                notificationRepo.save(notification);
                sendEmiNotification(notification);
            }
        }
    }

    private void sendNotification(Notification notification) {
        String borrowerName = notification.getLoan().getBorrower().getName();
        String message = notification.getMessage();
        
        // Send email notification to lender (app user)
        String lenderEmail = notification.getLoan().getBorrower().getOwner().getUsername();
        String emailSubject = "Lendex - Overdue Payment Reminder";
        String emailMessage = String.format("Reminder: %s\n\nBorrower: %s\n\nPlease follow up for payment collection.", 
            message, borrowerName);
        emailService.sendEmail(lenderEmail, emailSubject, emailMessage);
        
        System.out.println("Loan notification sent for: " + borrowerName);
    }

    private void sendEmiNotification(Notification notification) {
        String borrowerName = notification.getEmi().getBorrowerName();
        String message = notification.getMessage();
        
        // Send email notification to lender (app user)
        String lenderEmail = notification.getEmi().getUser().getUsername();
        String emailSubject = "Lendex - Overdue EMI Reminder";
        String emailMessage = String.format("EMI Reminder: %s\n\nBorrower: %s\n\nPlease follow up for EMI collection.", 
            message, borrowerName);
        emailService.sendEmail(lenderEmail, emailSubject, emailMessage);
        
        System.out.println("EMI notification sent for: " + borrowerName);
    }

    public List<Notification> getNotificationsForUser(String username) {
        List<Notification> loanNotifications = notificationRepo.findByLoanBorrowerOwnerUsername(username);
        List<Notification> emiNotifications = notificationRepo.findByEmiUserUsername(username);
        
        List<Notification> allNotifications = new java.util.ArrayList<>(loanNotifications);
        allNotifications.addAll(emiNotifications);
        
        return allNotifications;
    }
    
    @Transactional
    public void createNotificationForOverduePayment(Loan loan) {
        LocalDate today = LocalDate.now();
        LocalDate trackingStart = loan.getTrackingStartDate() != null ? 
            loan.getTrackingStartDate() : loan.getIssuedDate();
        
        // Check if notification is postponed
        if (loan.getNotificationPostponedUntil() != null && 
            loan.getNotificationPostponedUntil().isAfter(today)) {
            return; // Skip if postponed
        }
        
        // Calculate overdue months and amount
        LocalDate currentMonth = trackingStart;
        int overdueMonths = 0;
        double totalOverdue = 0;
        double monthlyInterest = loan.getPrincipal().doubleValue() * loan.getMonthlyInterestRate() / 100;
        
        while (currentMonth.isBefore(today.withDayOfMonth(1))) {
            final LocalDate monthToCheck = currentMonth;
            double totalPaid = paymentRepo.findByLoanOrderByDateDesc(loan).stream()
                .filter(payment -> 
                    payment.getDate().getYear() == monthToCheck.getYear() &&
                    payment.getDate().getMonth() == monthToCheck.getMonth())
                .mapToDouble(payment -> payment.getAmount().doubleValue())
                .sum();
            
            if (totalPaid < (monthlyInterest - 0.01)) {
                overdueMonths++;
                totalOverdue += (monthlyInterest - totalPaid);
            }
            
            currentMonth = currentMonth.plusMonths(1);
        }
        
        if (overdueMonths > 0) {
            // Check if notification already exists for today
            if (notificationRepo.findByLoanAndSentDate(loan, today).isEmpty()) {
                Notification notification = Notification.builder()
                    .loan(loan)
                    .dueDate(trackingStart)
                    .sentDate(today)
                    .daysPastDue((int) ChronoUnit.DAYS.between(trackingStart, today))
                    .message(String.format("Collect %d months overdue payments from %s - Total: ₹%.0f", 
                        overdueMonths, loan.getBorrower().getName(), totalOverdue))
                    .sent(true)
                    .build();
                
                notificationRepo.save(notification);
                sendNotification(notification);
            }
        }
    }
}