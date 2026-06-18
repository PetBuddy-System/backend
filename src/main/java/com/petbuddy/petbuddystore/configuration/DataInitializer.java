package com.petbuddy.petbuddystore.configuration;

import com.petbuddy.petbuddystore.model.Category;
import com.petbuddy.petbuddystore.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        initCategories();
    }

    private void initCategories() {
        if (categoryRepository.count() > 0) {
            return;
        }

        List<Category> categories = List.of(
                Category.builder()
                        .name("Thức ăn cho chó")
                        .description("Các loại thức ăn khô, pate, hạt và sản phẩm dinh dưỡng dành cho chó")
                        .build(),

                Category.builder()
                        .name("Thức ăn cho mèo")
                        .description("Các loại thức ăn khô, pate, hạt và sản phẩm dinh dưỡng dành cho mèo")
                        .build(),

                Category.builder()
                        .name("Phụ kiện thú cưng")
                        .description("Vòng cổ, dây dắt, balo, chuồng, nệm và các phụ kiện dành cho thú cưng")
                        .build(),

                Category.builder()
                        .name("Dinh dưỡng bổ sung")
                        .description("Thực phẩm chức năng và sản phẩm bổ sung dinh dưỡng cho thú cưng, không phải thuốc")
                        .build(),

                Category.builder()
                        .name("Đồ chơi thú cưng")
                        .description("Đồ chơi và sản phẩm giải trí giúp thú cưng vận động, giảm stress")
                        .build(),

                Category.builder()
                        .name("Vệ sinh và chăm sóc")
                        .description("Sữa tắm, lược chải, cát vệ sinh, dung dịch khử mùi và sản phẩm chăm sóc thú cưng")
                        .build()
        );

        categoryRepository.saveAll(categories);
    }

    }
