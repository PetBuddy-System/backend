package com.petbuddy.petbuddystore.scheduler;

import com.petbuddy.petbuddystore.service.OrderService;
import com.petbuddy.petbuddystore.service.ShipperAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ShipperZoneScheduler {

    ShipperAssignmentService shipperAssignmentService;
    OrderService orderService;

    @Scheduled(cron = "0 30 5 * * *")
    public void recomputeZones() {
        shipperAssignmentService.recomputeDailyZones();
    }

    @Scheduled(cron = "0 0 12 * * *")
    public void retryNoonDeliveryContacts() {
        orderService.retryNoonDeliveryContacts();
    }

}
