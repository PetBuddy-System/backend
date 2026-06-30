package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import com.petbuddy.petbuddystore.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    boolean existsByProductCode(String productCode);

    boolean existsByNameIgnoreCaseAndStatusNot(String name, ProductStatus status);

    Optional<Product> findByNameIgnoreCaseAndStatusNot(String name, ProductStatus status);

    boolean existsByCategory_CategoryIdAndStatusIn(Long categoryId, List<ProductStatus> statuses);
}