package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.dto.request.TimeSlotCreationRequest;
import com.petbuddy.petbuddystore.dto.request.TimeSlotUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.TimeSlotResponse;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public interface CatalogTimeSlotService {
    TimeSlotResponse createTimeSlot(TimeSlotCreationRequest request);

    TimeSlotResponse getTimeSlotById(Integer timeSlotId);

    List<TimeSlotResponse> getTimeSlotsByCatalog(Integer catalogId);

    List<TimeSlotResponse> getTimeSlotsByCatalogAndDayOfWeek(Integer catalogId, DayOfWeek dayOfWeek);

    List<TimeSlotResponse> getAvailableTimeSlotsBySelectedDate(Integer catalogId, LocalDate selectedDate);

    TimeSlotResponse updateTimeSlot(Integer timeSlotId, TimeSlotUpdateRequest request);

    TimeSlotResponse updateActiveStatus(Integer timeSlotId, Boolean isActive);
}
