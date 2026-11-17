package org.example.achat.payment.api;

import org.example.achat.payment.api.dto.SettlementResponse;
import org.example.achat.payment.application.SettlementApplicationService;
import org.example.achat.payment.application.SettlementNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SettlementApiContractTest {

    private final SettlementApplicationService service = mock(SettlementApplicationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(
                new SettlementController(service),
                new InternalSettlementController(service),
                new RecoveryRateController(service))
                .setControllerAdvice(new SettlementExceptionHandler())
                .build();
    }

    @Test
    void listsAndGetsSettlementUsingLegacyFieldNames() throws Exception {
        SettlementResponse settlement = new SettlementResponse(
                7L, 15.5f, 4.5f, false, LocalDate.parse("2026-04-03"), 22L);
        when(service.findAll()).thenReturn(List.of(settlement));
        when(service.findById(7L)).thenReturn(settlement);
        when(service.findByInvoice(22L)).thenReturn(List.of(settlement));

        mockMvc.perform(get("/reglements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idReglement").value(7))
                .andExpect(jsonPath("$[0].factureId").value(22))
                .andExpect(jsonPath("$[0].dateReglement").value("2026-04-03"));

        mockMvc.perform(get("/reglements/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantPaye").value(15.5));

        mockMvc.perform(get("/reglements/facture/22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].factureId").value(22));
    }

    @Test
    void validatesNewScalarInvoiceReferenceAndRetainsRevenueContract() throws Exception {
        mockMvc.perform(post("/reglements")
                        .contentType("application/json")
                        .content("""
                                {"montantPaye":1,"montantRestant":0,"payee":true,"dateReglement":"2026-04-03"}
                                """))
                .andExpect(status().isBadRequest());

        when(service.getRevenue(LocalDate.parse("2026-04-01"), LocalDate.parse("2026-04-30")))
                .thenReturn(15.5f);
        mockMvc.perform(get("/reglements/revenue")
                        .param("startDate", "2026-04-01")
                        .param("endDate", "2026-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(15.5));
    }

    @Test
    void returnsNotFoundAndValidatesDateOrder() throws Exception {
        when(service.findById(99L)).thenThrow(new SettlementNotFoundException(99L));
        mockMvc.perform(get("/reglements/99"))
                .andExpect(status().isNotFound());

        when(service.getRevenue(LocalDate.parse("2026-05-01"), LocalDate.parse("2026-04-01")))
                .thenThrow(new IllegalArgumentException("Start date must be before or equal to end date"));
        mockMvc.perform(get("/reglements/revenue")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-04-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void preservesLegacyRecoveryRatePath() throws Exception {
        when(service.getRecoveryRate(LocalDate.parse("2026-04-01"), LocalDate.parse("2026-04-30")))
                .thenReturn(75.0f);
        mockMvc.perform(get("/facture/pourcentageRecouvrement/2026-04-01/2026-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(75.0));
    }
}
