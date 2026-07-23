package com.petbuddy.petbuddystore.scheduler;

import com.petbuddy.petbuddystore.service.BookingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingAutoReassignScheduler {
    BookingService bookingService;

    @Scheduled(fixedRate = 60 * 1000)
    public void autoReassignUncheckedInGroomers() {
        try {
            bookingService.autoReassignOverdueBookings();
        } catch (Exception ex) {
            log.error("Booking auto reassign batch failed: {}", ex.getMessage(), ex);
        }
    }
}
