package org.example.achat.product.api;

import jakarta.validation.Valid;
import org.example.achat.product.api.dto.ProductCategoryRequest;
import org.example.achat.product.api.dto.ProductCategoryResponse;
import org.example.achat.product.application.ProductCategoryApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories-produits")
public class ProductCategoryController {

    private final ProductCategoryApplicationService categoryService;

    public ProductCategoryController(ProductCategoryApplicationService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<ProductCategoryResponse> getCategories() {
        return categoryService.findAll();
    }

    @GetMapping("/{categoryId}")
    public ProductCategoryResponse getCategory(@PathVariable Long categoryId) {
        return categoryService.findById(categoryId);
    }

    @PostMapping
    public ResponseEntity<ProductCategoryResponse> createCategory(
            @Valid @RequestBody ProductCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @PutMapping("/{categoryId}")
    public ProductCategoryResponse updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody ProductCategoryRequest request) {
        return categoryService.update(categoryId, request);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.delete(categoryId);
        return ResponseEntity.noContent().build();
    }
}
