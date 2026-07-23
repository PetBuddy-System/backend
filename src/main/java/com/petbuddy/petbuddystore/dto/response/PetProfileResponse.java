package com.petbuddy.petbuddystore.dto.response;

import com.petbuddy.petbuddystore.common.enums.PetStatus;
import com.petbuddy.petbuddystore.common.enums.VaccinationStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PetProfileResponse {
    String petId;
    String userId;
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
    PetStatus petStatus;
    LocalDateTime createdAt;
    List<MediaFileResponse> mediaFiles;
}
