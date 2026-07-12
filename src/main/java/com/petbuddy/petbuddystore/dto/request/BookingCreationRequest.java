package com.petbuddy.petbuddystore.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingCreationRequest {
    @NotNull
    String bookingType;

    @NotNull
    String customerName;

    @NotNull
    String customerPhone;

    String address;

    @NotNull
    @FutureOrPresent
    LocalDate scheduledAt;

    String note;

    @Valid
    @NotEmpty
    List<BookingDetailCreationRequest> bookingDetails;
}
