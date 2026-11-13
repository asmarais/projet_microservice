package org.example.achat.product.application;

import org.example.achat.product.api.dto.ProductRequest;
import org.example.achat.product.api.dto.ProductResponse;
import org.example.achat.product.domain.Product;
import org.example.achat.product.infrastructure.persistence.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductApplicationService {

    private final ProductRepository productRepository;

    public ProductApplicationService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long productId) {
        return toResponse(findProduct(productId));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        applyRequest(product, request);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long productId, ProductRequest request) {
        Product product = findProduct(productId);
        product.setCodeProduit(request.codeProduit());
        product.setLibelleProduit(request.libelleProduit());
        product.setPrix(request.prix());
        product.setDateDerniereModification(request.dateDerniereModification());
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        productRepository.deleteById(productId);
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setCodeProduit(request.codeProduit());
        product.setLibelleProduit(request.libelleProduit());
        product.setPrix(request.prix());
        product.setDateCreation(request.dateCreation());
        product.setDateDerniereModification(request.dateDerniereModification());
    }

    private ProductResponse toResponse(Product product) {
        Long categoryId = product.getCategorieProduit() == null
                ? null
                : product.getCategorieProduit().getIdCategorieProduit();
        return new ProductResponse(
                product.getIdProduit(),
                product.getCodeProduit(),
                product.getLibelleProduit(),
                product.getPrix(),
                null,
                categoryId);
    }
}
