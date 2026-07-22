package com.petbuddy.petbuddystore.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegistrationPeriodCreationRequest {
    LocalDate workFromDate;
    LocalDate workToDate;
    LocalDateTime registerOpenAt;
    LocalDateTime registerCloseAt;
}
