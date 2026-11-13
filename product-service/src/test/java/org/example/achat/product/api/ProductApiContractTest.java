package org.example.achat.product.api;

import org.example.achat.product.api.dto.ProductCategoryResponse;
import org.example.achat.product.api.dto.ProductResponse;
import org.example.achat.product.application.ProductApplicationService;
import org.example.achat.product.application.ProductCategoryApplicationService;
import org.example.achat.product.application.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ProductApiContractTest {

    private final ProductApplicationService productService = mock(ProductApplicationService.class);
    private final ProductCategoryApplicationService categoryService =
            mock(ProductCategoryApplicationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = standaloneSetup(
                new ProductController(productService),
                new ProductCategoryController(categoryService))
                .setControllerAdvice(new ProductExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void listsProductsWithLegacyFieldNamesAndNoClaimedStockAssignment() throws Exception {
        when(productService.findAll()).thenReturn(List.of(
                new ProductResponse(7L, "P-7", "Paper", 2.5f, null, 3L)));

        mockMvc.perform(get("/produits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idProduit").value(7))
                .andExpect(jsonPath("$[0].codeProduit").value("P-7"))
                .andExpect(jsonPath("$[0].prix").value(2.5))
                .andExpect(content().string(containsString("\"stockId\":null")))
                .andExpect(jsonPath("$[0].categorieProduitId").value(3));
    }

    @Test
    void createsProductAndPreservesLegacyCreatedStatus() throws Exception {
        when(productService.create(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ProductResponse(8L, "P-8", "Pen", 1.25f, null, null));

        mockMvc.perform(post("/produits")
                        .contentType("application/json")
                        .content("""
                                {"codeProduit":"P-8","libelleProduit":"Pen","prix":1.25}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idProduit").value(8))
                .andExpect(content().string(containsString("\"stockId\":null")));
    }

    @Test
    void rejectsBlankProductCode() throws Exception {
        mockMvc.perform(post("/produits")
                        .contentType("application/json")
                        .content("""
                                {"codeProduit":" ","libelleProduit":"Pen","prix":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"));
    }

    @Test
    void rejectsNegativeProductPrice() throws Exception {
        mockMvc.perform(post("/produits")
                        .contentType("application/json")
                        .content("""
                                {"codeProduit":"P-9","libelleProduit":"Pen","prix":-0.01}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsLegacyNotFoundStatusAndDetailForProduct() throws Exception {
        when(productService.findById(99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/produits/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Produit not found with id: 99"));
    }

    @Test
    void updatesProductByPathIdAndDeletesWithNoContent() throws Exception {
        when(productService.update(org.mockito.ArgumentMatchers.eq(4L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ProductResponse(4L, "P-4", "Updated", 3.0f, null, null));

        mockMvc.perform(put("/produits/4")
                        .contentType("application/json")
                        .content("""
                                {"codeProduit":"P-4","libelleProduit":"Updated","prix":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idProduit").value(4));

        mockMvc.perform(delete("/produits/4")).andExpect(status().isNoContent());
        verify(productService).delete(4L);
    }

    @Test
    void listsAndCreatesCategoriesWithLegacyJsonNames() throws Exception {
        when(categoryService.findAll()).thenReturn(
                List.of(new ProductCategoryResponse(2L, "CAT-2", "Office")));
        when(categoryService.findById(2L))
                .thenReturn(new ProductCategoryResponse(2L, "CAT-2", "Office"));
        when(categoryService.create(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ProductCategoryResponse(3L, "CAT-3", "Hardware"));

        mockMvc.perform(get("/categories-produits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idCategorieProduit").value(2))
                .andExpect(jsonPath("$[0].codeCategorie").value("CAT-2"));

        mockMvc.perform(get("/categories-produits/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libelleCategorie").value("Office"));

        mockMvc.perform(post("/categories-produits")
                        .contentType("application/json")
                        .content("""
                                {"codeCategorie":"CAT-3","libelleCategorie":"Hardware"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCategorieProduit").value(3));
    }

    @Test
    void rejectsBlankCategoryLabelAndMapsReferenceConflictsToConflict() throws Exception {
        mockMvc.perform(post("/categories-produits")
                        .contentType("application/json")
                        .content("""
                                {"codeCategorie":"CAT-3","libelleCategorie":" "}
                                """))
                .andExpect(status().isBadRequest());

        doThrow(new DataIntegrityViolationException("referenced category"))
                .when(categoryService).delete(6L);
        mockMvc.perform(delete("/categories-produits/6"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void updatesAndDeletesCategory() throws Exception {
        when(categoryService.update(
                org.mockito.ArgumentMatchers.eq(5L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ProductCategoryResponse(5L, "CAT-5", "Updated"));

        mockMvc.perform(put("/categories-produits/5")
                        .contentType("application/json")
                        .content("""
                                {"codeCategorie":"CAT-5","libelleCategorie":"Updated"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libelleCategorie").value("Updated"));

        mockMvc.perform(delete("/categories-produits/5")).andExpect(status().isNoContent());
        verify(categoryService).delete(5L);
    }
}
