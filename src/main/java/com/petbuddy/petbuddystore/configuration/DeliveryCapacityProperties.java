package com.petbuddy.petbuddystore.configuration;

import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "delivery.capacity")
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class DeliveryCapacityProperties {

    /** Vận tốc trung bình xe máy trong nội thành (km/h) - có kẹt xe, đèn đỏ... */
    double avgSpeedKmh = 25.0;

    /** Thời gian xử lý tại mỗi điểm giao: gửi hàng, chờ khách, thu COD... (phút) */
    double handlingTimeMinutes = 5.0;

    /** Buffer an toàn cho rủi ro (kẹt xe, khách bận...), tính theo % thời gian ca */
   double safetyBufferPercent = 15.0;

    /**
     * Bán kính vận hành tối đa cho xe máy tính từ cửa hàng (km).
     * Vượt ngưỡng này -> không tự động phân vào khu vực nữa,
     * vì thời gian di chuyển khứ hồi sẽ nuốt gần hết ca làm việc.
     */
    double maxOperationalRadiusKm = 15.0;

    /** Số vòng lặp tối đa cho K-Means */
    int kMeansMaxIterations = 20;
    int restockThresholdOrders = 2;
}