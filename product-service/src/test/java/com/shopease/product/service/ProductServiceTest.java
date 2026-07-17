package com.shopease.product.service;

import com.shopease.product.dto.ProductRequest;
import com.shopease.product.dto.ProductResponse;
import com.shopease.product.model.Product;
import com.shopease.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("iPhone 15");
        product.setDescription("Latest Apple smartphone");
        product.setPrice(79999.0);
        product.setCategory("Electronics");
        product.setImageUrl("iphone.jpg");
        product.setQuantity(50);

        productRequest = new ProductRequest();
        productRequest.setName("iPhone 15");
        productRequest.setDescription("Latest Apple smartphone");
        productRequest.setPrice(79999.0);
        productRequest.setCategory("Electronics");
        productRequest.setImageUrl("iphone.jpg");
        productRequest.setQuantity(50);
    }

    @Test
    void addProduct_ShouldReturnProductResponse() {
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.addProduct(productRequest);

        assertNotNull(response);
        assertEquals("iPhone 15", response.getName());
        assertEquals(79999.0, response.getPrice());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void getAllProducts_ShouldReturnList() {
        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Samsung S24");
        product2.setPrice(69999.0);
        product2.setCategory("Electronics");

        when(productRepository.findAll()).thenReturn(Arrays.asList(product, product2));

        List<ProductResponse> products = productService.getAllProducts();

        assertEquals(2, products.size());
        assertEquals("iPhone 15", products.get(0).getName());
        assertEquals("Samsung S24", products.get(1).getName());
    }

    @Test
    void getProductById_ShouldReturnProduct_WhenExists() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("iPhone 15", response.getName());
    }

    @Test
    void getProductById_ShouldThrowException_WhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productService.getProductById(99L));
    }

    @Test
    void getByCategory_ShouldReturnFilteredProducts() {
        when(productRepository.findByCategory("Electronics")).thenReturn(Arrays.asList(product));

        List<ProductResponse> results = productService.getByCategory("Electronics");

        assertEquals(1, results.size());
        assertEquals("Electronics", results.get(0).getCategory());
    }

    @Test
    void searchProducts_ShouldReturnMatchingProducts() {
        when(productRepository.findByNameContainingIgnoreCase("iPhone")).thenReturn(Arrays.asList(product));

        List<ProductResponse> results = productService.searchProducts("iPhone");

        assertEquals(1, results.size());
        assertEquals("iPhone 15", results.get(0).getName());
    }

    @Test
    void deleteProduct_ShouldCallRepository() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        doNothing().when(productRepository).deleteById(1L);

        productService.deleteProduct(1L);

        verify(productRepository, times(1)).deleteById(1L);
    }
}
