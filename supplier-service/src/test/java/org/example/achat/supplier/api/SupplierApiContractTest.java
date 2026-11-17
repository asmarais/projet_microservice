package org.example.achat.supplier.api;

import org.example.achat.supplier.api.dto.ActivitySectorResponse;
import org.example.achat.supplier.api.dto.SupplierResponse;
import org.example.achat.supplier.application.SupplierApplicationService;
import org.example.achat.supplier.application.SupplierNotFoundException;
import org.example.achat.supplier.domain.SupplierCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SupplierApiContractTest {

    private final SupplierApplicationService service = mock(SupplierApplicationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(
                new SupplierController(service),
                new ActivitySectorController(service),
                new SupplierReferenceController(service))
                .setControllerAdvice(new SupplierExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void listsSuppliersWithLegacyResponseFields() throws Exception {
        when(service.findAllSuppliers()).thenReturn(List.of(
                new SupplierResponse(7L, "S-7", "Supplier", SupplierCategory.ORDINAIRE, 12L, Set.of(3L))));

        mockMvc.perform(get("/fournisseurs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idFournisseur").value(7))
                .andExpect(jsonPath("$[0].categorieFournisseur").value("ORDINAIRE"))
                .andExpect(jsonPath("$[0].detailFournisseurId").value(12))
                .andExpect(jsonPath("$[0].secteurActiviteIds[0]").value(3));
    }

    @Test
    void createsSupplierWithLegacyStatusAndJsonNames() throws Exception {
        when(service.createSupplier(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new SupplierResponse(8L, "S-8", "New", SupplierCategory.CONVENTIONNE, null, Set.of()));

        mockMvc.perform(post("/fournisseurs")
                        .contentType("application/json")
                        .content("""
                                {"code":"S-8","libelle":"New","categorieFournisseur":"CONVENTIONNE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idFournisseur").value(8))
                .andExpect(jsonPath("$.secteurActiviteIds").isArray());
    }

    @Test
    void validatesRequiredSupplierAndSectorFields() throws Exception {
        mockMvc.perform(post("/fournisseurs")
                        .contentType("application/json")
                        .content("""
                                {"code":" ","libelle":"New","categorieFournisseur":"ORDINAIRE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"));

        mockMvc.perform(post("/secteurs-activite")
                        .contentType("application/json")
                        .content("""
                                {"codeSecteurActivite":"A","libelleSecteurActivite":" "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundAndSupportsUpdateDeleteSectorRoutes() throws Exception {
        when(service.findSupplier(99L)).thenThrow(new SupplierNotFoundException(99L));

        mockMvc.perform(get("/fournisseurs/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Fournisseur not found with id: 99"));

        when(service.updateSector(org.mockito.ArgumentMatchers.eq(4L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ActivitySectorResponse(4L, "A-4", "Updated"));

        mockMvc.perform(put("/secteurs-activite/4")
                        .contentType("application/json")
                        .content("""
                                {"codeSecteurActivite":"A-4","libelleSecteurActivite":"Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSecteurActivite").value(4));

        mockMvc.perform(delete("/secteurs-activite/4")).andExpect(status().isNoContent());
    }

    @Test
    void internalReferenceReturnsSupplierLookupShape() throws Exception {
        when(service.findReference(5L))
                .thenReturn(new org.example.achat.supplier.api.dto.SupplierReferenceResponse(5L, "S-5", "Known"));

        mockMvc.perform(get("/internal/v1/suppliers/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplierId").value(5))
                .andExpect(jsonPath("$.label").value("Known"));
    }
}
