package com.petbuddy.petbuddystore.model;

import com.petbuddy.petbuddystore.common.enums.RegistrationPeriodStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "shift_registration_periods")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShiftRegistrationPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "registration_period_id")
    String registrationPeriodId;

    @Column(nullable = false)
    LocalDate workFromDate;

    @Column(nullable = false)
    LocalDate workToDate;

    @Column(nullable = false)
    LocalDateTime registerOpenAt;

    @Column(nullable = false)
    LocalDateTime registerCloseAt;

    @Enumerated(EnumType.STRING)
    RegistrationPeriodStatus status;
}
