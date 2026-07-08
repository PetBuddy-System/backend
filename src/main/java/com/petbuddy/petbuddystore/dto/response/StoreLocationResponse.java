package com.petbuddy.petbuddystore.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StoreLocationResponse {
    Long id;
    Double latitude;
    Double longitude;
    String address;
    boolean active;
    LocalDateTime createdAt;
    LocalDateTime deactivatedAt;
}
