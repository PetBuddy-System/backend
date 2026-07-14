package com.petbuddy.petbuddystore.configuration;

import com.petbuddy.petbuddystore.common.enums.Role;
import com.petbuddy.petbuddystore.common.enums.StaffTask;
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
                        .password(passwordEncoder.encode("Admin@1234"))
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
                        .password(passwordEncoder.encode("Manager@1234"))
                        .fullName("Manager")
                        .role(Role.MANAGER)
                        .status(UserStatus.ACTIVE)
                        .build();
                userRepository.save(user);
                log.warn("Manager has been created");
            }

            if (userRepository.findByEmail("groomer@gmail.com").isEmpty()) {
                User user = User.builder()
                        .email("groomer@gmail.com")
                        .password(passwordEncoder.encode("Groomer@1234"))
                        .fullName("Groomer Staff")
                        .role(Role.STAFF)
                        .staffTask(StaffTask.GROOMER)
                        .status(UserStatus.ACTIVE)
                        .build();
                userRepository.save(user);
                log.warn("Groomer Staff has been created");
            }

            if (userRepository.findByEmail("coordinator@gmail.com").isEmpty()) {
                User user = User.builder()
                        .email("coordinator@gmail.com")
                        .password(passwordEncoder.encode("Coordinator@1234"))
                        .fullName("Coordinator Staff")
                        .role(Role.STAFF)
                        .staffTask(StaffTask.COORDINATOR)
                        .status(UserStatus.ACTIVE)
                        .build();
                userRepository.save(user);
                log.warn("Coordinator Staff has been created");
            }

            if (userRepository.findByEmail("shipper@gmail.com").isEmpty()) {
                User user = User.builder()
                        .email("shipper@gmail.com")
                        .password(passwordEncoder.encode("Shipper@1234"))
                        .fullName("Shipper Staff")
                        .role(Role.STAFF)
                        .staffTask(StaffTask.SHIPPER)
                        .status(UserStatus.ACTIVE)
                        .build();
                userRepository.save(user);
                log.warn("Shipper Staff has been created");
            }

            if (userRepository.findByEmail("customer@gmail.com").isEmpty()) {
                User user = User.builder()
                        .email("customer@gmail.com")
                        .password(passwordEncoder.encode("Customer@1234"))
                        .fullName("Customer")
                        .role(Role.CUSTOMER)
                        .status(UserStatus.ACTIVE)
                        .build();
                userRepository.save(user);
                log.warn("Customer has been created");
            }
        };
    }
}
