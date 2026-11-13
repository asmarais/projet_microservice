package org.example.achat.product.application;

import org.example.achat.product.api.dto.ProductCategoryRequest;
import org.example.achat.product.api.dto.ProductCategoryResponse;
import org.example.achat.product.domain.ProductCategory;
import org.example.achat.product.infrastructure.persistence.ProductCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductCategoryApplicationService {

    private final ProductCategoryRepository categoryRepository;

    public ProductCategoryApplicationService(ProductCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductCategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductCategoryResponse findById(Long categoryId) {
        return toResponse(findCategory(categoryId));
    }

    @Transactional
    public ProductCategoryResponse create(ProductCategoryRequest request) {
        ProductCategory category = new ProductCategory();
        category.setCodeCategorie(request.codeCategorie());
        category.setLibelleCategorie(request.libelleCategorie());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public ProductCategoryResponse update(Long categoryId, ProductCategoryRequest request) {
        ProductCategory category = findCategory(categoryId);
        category.setCodeCategorie(request.codeCategorie());
        category.setLibelleCategorie(request.libelleCategorie());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ProductCategoryNotFoundException(categoryId);
        }
        categoryRepository.deleteById(categoryId);
    }

    private ProductCategory findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ProductCategoryNotFoundException(categoryId));
    }

    private ProductCategoryResponse toResponse(ProductCategory category) {
        return new ProductCategoryResponse(
                category.getIdCategorieProduit(),
                category.getCodeCategorie(),
                category.getLibelleCategorie());
    }
}
