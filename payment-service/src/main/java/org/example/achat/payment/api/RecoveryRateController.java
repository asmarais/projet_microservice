package org.example.achat.payment.api;

import org.example.achat.payment.application.SettlementApplicationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/facture")
public class RecoveryRateController {

    private final SettlementApplicationService service;

    public RecoveryRateController(SettlementApplicationService service) {
        this.service = service;
    }

    @GetMapping("/pourcentageRecouvrement/{startDate}/{endDate}")
    public float recoveryRate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return service.getRecoveryRate(startDate, endDate);
    }
}
