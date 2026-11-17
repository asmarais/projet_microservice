package org.example.achat.procurement.api;

import org.example.achat.procurement.api.dto.InvoiceResponse;
import org.example.achat.procurement.application.InvoiceCommandService;
import org.example.achat.procurement.application.InvoiceQueryService;
import org.example.achat.procurement.application.InvoiceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class InvoiceApiContractTest {

    private final InvoiceCommandService commands = mock(InvoiceCommandService.class);
    private final InvoiceQueryService queries = mock(InvoiceQueryService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new InvoiceController(commands, queries))
                .setControllerAdvice(new ProcurementExceptionHandler())
                .build();
    }

    @Test
    void preservesLegacyInvoicePathsAndPayloadFieldNames() throws Exception {
        InvoiceResponse invoice = new InvoiceResponse(
                7L, 3.5f, 27.5f, null, null, false, List.of());
        when(queries.findAll()).thenReturn(List.of(invoice));
        when(queries.findById(7L)).thenReturn(invoice);

        mockMvc.perform(get("/facture/retrieve-all-factures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idFacture").value(7))
                .andExpect(jsonPath("$[0].montantFacture").value(27.5))
                .andExpect(jsonPath("$[0].detailsFacture").isArray());

        mockMvc.perform(get("/facture/retrieve-facture/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivee").value(false));
    }

    @Test
    void acceptsLegacyCreateAndReturnsLegacyStatus() throws Exception {
        when(commands.create(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new InvoiceResponse(8L, 0, 25, null, null, false, List.of()));

        mockMvc.perform(post("/facture/add-facture")
                        .contentType("application/json")
                        .content("""
                                {"montantRemise":0,"montantFacture":25,"archivee":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idFacture").value(8));
    }

    @Test
    void mapsMissingInvoiceToLegacyEmptySuccessBody() throws Exception {
        when(queries.findById(99L)).thenReturn(null);
        mockMvc.perform(get("/facture/retrieve-facture/99"))
                .andExpect(status().isOk());
    }
}
