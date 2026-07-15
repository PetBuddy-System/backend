package com.petbuddy.petbuddystore.scheduler;

import com.petbuddy.petbuddystore.common.enums.UserStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.model.User;
import com.petbuddy.petbuddystore.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserSuspensionScheduler {
    UserRepository userRepository;

    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void unsuspendExpiredUsers() {
        List<User> expiredSuspensions = userRepository
                .findByStatusAndSuspendedUntilBefore(UserStatus.SUSPENDED, LocalDateTime.now());

        for (User user : expiredSuspensions) {
            try {
                unsuspendUser(user);
            } catch (AppException ignored) {
            }
        }
    }

    private void unsuspendUser(User user) {
        user.setStatus(UserStatus.ACTIVE);
        user.setSuspendedUntil(null);
        user.setPaymentFailStreak(0);
        userRepository.save(user);
    }
}