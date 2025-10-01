package com.example.backend.controller;

import com.example.backend.entity.EMI;
import com.example.backend.entity.EMIPayment;
import com.example.backend.service.EMIService;
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
public class EMIController {
    private final EMIService emiService;

    public EMIController(EMIService emiService) {
        this.emiService = emiService;
    }

    @GetMapping("/emis")
    public ResponseEntity<List<EMI>> getEmis(Principal principal) {
        return ResponseEntity.ok(emiService.getEmisForUser(principal.getName()));
    }

    @PostMapping("/emis")
    public ResponseEntity<EMI> createEmi(@RequestBody Map<String, String> body, Principal principal) {
        String borrowerName = body.get("borrowerName");
        BigDecimal totalAmount = new BigDecimal(body.get("totalAmount"));
        BigDecimal givenInCash = new BigDecimal(body.get("givenInCash"));
        LocalDate givenDate = LocalDate.parse(body.get("givenDate"));
        Integer tenure = Integer.valueOf(body.get("tenure"));
        BigDecimal emiAmount = new BigDecimal(body.get("emiAmount"));
        LocalDate startDate = LocalDate.parse(body.get("startDate"));
        
        return ResponseEntity.ok(emiService.createEmi(borrowerName, totalAmount, givenInCash, 
                                                     givenDate, tenure, emiAmount, startDate, principal.getName()));
    }

    @DeleteMapping("/emis/{emiId}")
    public ResponseEntity<Void> deleteEmi(@PathVariable Long emiId, Principal principal) {
        emiService.deleteEmi(emiId, principal.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/emis/{emiId}/payments")
    public ResponseEntity<EMIPayment> recordEmiPayment(@PathVariable Long emiId, @RequestBody Map<String,String> body, Principal principal) {
        BigDecimal amount = new BigDecimal(body.get("amount"));
        LocalDate monthDate = LocalDate.parse(body.get("date"));
        String note = body.getOrDefault("note", "");
        return ResponseEntity.ok(emiService.recordEmiPayment(emiId, amount, monthDate, note, principal.getName()));
    }

    @GetMapping("/emis/{emiId}/payments")
    public ResponseEntity<List<EMIPayment>> getEmiPayments(@PathVariable Long emiId, Principal principal) {
        return ResponseEntity.ok(emiService.getEmiPayments(emiId, principal.getName()));
    }

    @PostMapping("/emis/{emiId}/close")
    public ResponseEntity<EMI> closeEmi(@PathVariable Long emiId, Principal principal) {
        return ResponseEntity.ok(emiService.closeEmi(emiId, principal.getName()));
    }

    @PutMapping("/emis/{emiId}")
    public ResponseEntity<EMI> updateEmi(@PathVariable Long emiId, @RequestBody Map<String, String> body, Principal principal) {
        String borrowerName = body.get("borrowerName");
        BigDecimal totalAmount = new BigDecimal(body.get("totalAmount"));
        BigDecimal givenInCash = new BigDecimal(body.get("givenInCash"));
        LocalDate givenDate = LocalDate.parse(body.get("givenDate"));
        Integer tenure = Integer.valueOf(body.get("tenure"));
        BigDecimal emiAmount = new BigDecimal(body.get("emiAmount"));
        LocalDate startDate = LocalDate.parse(body.get("startDate"));
        
        return ResponseEntity.ok(emiService.updateEmi(emiId, borrowerName, totalAmount, givenInCash,
                                                      givenDate, tenure, emiAmount, startDate, principal.getName()));
    }
}