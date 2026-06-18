package com.petbuddy.petbuddystore.configuration;

import com.petbuddy.petbuddystore.common.enums.Role;
import com.petbuddy.petbuddystore.common.enums.UserStatus;
import com.petbuddy.petbuddystore.model.User;
import com.petbuddy.petbuddystore.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {
    PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository) {
        return application -> {
            if (userRepository.findByEmail("admin@gmail.com").isEmpty()) {
                User user = User.builder()
                        .email("admin@gmail.com")
                        .password(passwordEncoder.encode("admin"))
                        .fullName("Admin")
                        .role(Role.ADMIN)
                        .status(UserStatus.ACTIVE)
                        .build();
                userRepository.save(user);
                log.warn("Admin has been created");
            }

            if (userRepository.findByEmail("manager@gmail.com").isEmpty()) {
                User user = User.builder()
                        .email("manager@gmail.com")
                        .password(passwordEncoder.encode("manager"))
                        .fullName("Manager")
                        .role(Role.MANAGER)
                        .status(UserStatus.ACTIVE)
                        .build();
                userRepository.save(user);
                log.warn("Manager has been created");
            }

            if (userRepository.findByEmail("staff@gmail.com").isEmpty()) {
                User user = User.builder()
                        .email("staff@gmail.com")
                        .password(passwordEncoder.encode("staff"))
                        .fullName("Staff")
                        .role(Role.STAFF)
                        .status(UserStatus.ACTIVE)
                        .build();
                userRepository.save(user);
                log.warn("Staff has been created");
            }

            if (userRepository.findByEmail("user@gmail.com").isEmpty()) {
                User user = User.builder()
                        .email("user@gmail.com")
                        .password(passwordEncoder.encode("user"))
                        .fullName("User")
                        .role(Role.CUSTOMER)
                        .status(UserStatus.ACTIVE)
                        .build();
                userRepository.save(user);
                log.warn("User has been created");
            }
        };
    }
}
