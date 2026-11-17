package org.example.achat.payment.api;

import org.example.achat.payment.application.SettlementApplicationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/internal/v1/settlements")
public class InternalSettlementController {

    private final SettlementApplicationService service;

    public InternalSettlementController(SettlementApplicationService service) {
        this.service = service;
    }

    @GetMapping("/revenue")
    public float revenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return service.getRevenue(startDate, endDate);
    }
}
