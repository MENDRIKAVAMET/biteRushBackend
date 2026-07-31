package com.biterush.api.service;

import com.biterush.api.dto.ProductRequestDTO;
import com.biterush.api.dto.ProductResponseDTO;
import com.biterush.api.entity.Product;
import com.biterush.api.repository.ProductRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponseDTO> getAll() {

        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public ProductResponseDTO getById(Long id) {

        Product product = findProductEntityById(id);

        return mapToResponse(product);
    }

    public ProductResponseDTO save(ProductRequestDTO dto) {

        validateProduct(dto);

        Product product = new Product();

        product.setNom(dto.nom.trim());
        product.setDescription(dto.description.trim());
        product.setPrix(dto.prix);
        product.setStock(dto.stock);

        Product savedProduct = productRepository.save(product);

        return mapToResponse(savedProduct);
    }

    public ProductResponseDTO update(Long id,
                                     ProductRequestDTO dto) {

        validateProduct(dto);

        Product product = findProductEntityById(id);

        product.setNom(dto.nom.trim());
        product.setDescription(dto.description.trim());
        product.setPrix(dto.prix);
        product.setStock(dto.stock);

        Product updatedProduct = productRepository.save(product);

        return mapToResponse(updatedProduct);
    }

    public void delete(Long id) {

        Product product = findProductEntityById(id);

        productRepository.delete(product);
    }

    private Product findProductEntityById(Long id) {

        return productRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Produit introuvable"
                        ));
    }

    private void validateProduct(ProductRequestDTO dto) {

        if (dto.nom == null || dto.nom.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le nom est obligatoire"
            );
        }

        if (dto.prix == null || dto.prix <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Prix invalide"
            );
        }

        if (dto.stock == null || dto.stock < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Stock invalide"
            );
        }
    }

    private ProductResponseDTO mapToResponse(Product product) {

        ProductResponseDTO dto = new ProductResponseDTO();

        dto.id = product.getId();
        dto.nom = product.getNom();
        dto.description = product.getDescription();
        dto.prix = product.getPrix();

        dto.stock = product.getStock();

        dto.available = product.getStock() > 0;

        return dto;
    }
}