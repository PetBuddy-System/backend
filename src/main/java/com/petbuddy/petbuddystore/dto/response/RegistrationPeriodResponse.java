package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.RegistrationPeriodStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegistrationPeriodResponse {
    String registrationPeriodId;
    LocalDate workFromDate;
    LocalDate workToDate;
    LocalDateTime registerOpenAt;
    LocalDateTime registerCloseAt;
    RegistrationPeriodStatus status;
}
