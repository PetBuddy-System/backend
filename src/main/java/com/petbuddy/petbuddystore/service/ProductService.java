package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import com.petbuddy.petbuddystore.dto.request.ProductCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ProductUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.ProductDetailResponse;
import com.petbuddy.petbuddystore.dto.response.ProductManagementResponse;
import com.petbuddy.petbuddystore.dto.response.ProductPublicResponse;
import com.petbuddy.petbuddystore.model.Category;
import com.petbuddy.petbuddystore.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProductService {

    ProductManagementResponse createProduct(ProductCreationRequest request, List<MultipartFile> images);

    Page<ProductPublicResponse> getProductsForUser(String keyword, Long categoryId, String brandName, String sortBy, Pageable pageable);

    Page<ProductManagementResponse> getProductsForManagement(String keyword, Long categoryId, String brandName, ProductStatus status, String sortBy, Pageable pageable );

    ProductDetailResponse getProductDetail(UUID productId);

    ProductManagementResponse updateProduct(UUID productId, ProductUpdateRequest request, List<MultipartFile> images);

    Product getProductEntityById(UUID productId);

    Product getActiveProductEntityById(UUID productId);

    Product getProductEntityByName(String name);

    Product createProductFromImport(String name, String description, BigDecimal price, String brandName, Category category);}