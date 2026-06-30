package com.petbuddy.petbuddystore;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PetbuddyStoreApplicationTests {

    static {
        com.petbuddy.petbuddystore.configuration.DotenvLoader.loadEnv();
    }

    @Test
    void contextLoads() {
    }

}
