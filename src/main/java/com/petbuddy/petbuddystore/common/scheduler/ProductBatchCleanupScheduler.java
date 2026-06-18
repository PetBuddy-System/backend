package com.petbuddy.petbuddystore.common.scheduler;

import com.petbuddy.petbuddystore.service.ProductBatchService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductBatchCleanupScheduler {

    ProductBatchService productBatchService;

    @Scheduled(cron = "0 0 2 * * *")
    public void deleteOldDeletedBatches() {
        log.info("Start deleting product batches deleted more than 90 days ago");
        productBatchService.deleteDeletedBatchesOlderThan90Days();
        log.info("Finished deleting product batches deleted more than 90 days ago");
    }
}