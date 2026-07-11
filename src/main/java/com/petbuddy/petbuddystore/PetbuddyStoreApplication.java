package com.petbuddy.petbuddystore;

import com.petbuddy.petbuddystore.configuration.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PetbuddyStoreApplication {

    public static void main(String[] args) {
        DotenvLoader.loadEnv();
        SpringApplication.run(PetbuddyStoreApplication.class, args);
    }

}
