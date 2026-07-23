package com.petbuddy.petbuddystore.dto.request;

import com.petbuddy.petbuddystore.common.enums.VaccinationStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PetProfileUpdateRequest {
    String petName;
    String species;
    String breed;
    String gender;
    LocalDate dateOfBirth;
    Double weight;
    String color;
    String healthNote;
    String allergyNote;
    String behaviorNote;
    VaccinationStatus vaccinationStatus;
}
