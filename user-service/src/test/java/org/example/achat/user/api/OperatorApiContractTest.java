package org.example.achat.user.api;

import org.example.achat.user.api.dto.OperatorResponse;
import org.example.achat.user.application.OperatorApplicationService;
import org.example.achat.user.application.OperatorNotFoundException;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class OperatorApiContractTest {

    private final OperatorApplicationService service = mock(OperatorApplicationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(
                new OperatorController(service),
                new OperatorReferenceController(service))
                .setControllerAdvice(new OperatorExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createsAndListsOperatorWithoutExposingPassword() throws Exception {
        when(service.create(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new OperatorResponse(3L, "Doe", "Jane", Set.of()));
        when(service.findAll()).thenReturn(List.of(new OperatorResponse(3L, "Doe", "Jane", Set.of())));

        mockMvc.perform(post("/operateurs")
                        .contentType("application/json")
                        .content("""
                                {"nom":"Doe","prenom":"Jane","password":"ignored-secret"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idOperateur").value(3))
                .andExpect(jsonPath("$.factureIds").isArray())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("ignored-secret"))));

        mockMvc.perform(get("/operateurs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].prenom").value("Jane"));
    }

    @Test
    void rejectsBlankOperatorFieldsAndReturnsNotFound() throws Exception {
        mockMvc.perform(post("/operateurs")
                        .contentType("application/json")
                        .content("""
                                {"nom":"Doe","prenom":"Jane","password":" "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"));

        when(service.findById(99L)).thenThrow(new OperatorNotFoundException(99L));
        mockMvc.perform(get("/operateurs/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Operateur not found with id: 99"));

        mockMvc.perform(delete("/operateurs/99")).andExpect(status().isNoContent());
    }
}
