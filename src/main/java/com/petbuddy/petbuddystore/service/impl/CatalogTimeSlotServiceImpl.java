package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.BookingStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.TimeSlotCreationRequest;
import com.petbuddy.petbuddystore.dto.request.TimeSlotUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.TimeSlotResponse;
import com.petbuddy.petbuddystore.mapper.TimeSlotMapper;
import com.petbuddy.petbuddystore.model.Catalog;
import com.petbuddy.petbuddystore.model.CatalogTimeSlot;
import com.petbuddy.petbuddystore.repository.BookingRepository;
import com.petbuddy.petbuddystore.repository.CatalogRepository;
import com.petbuddy.petbuddystore.repository.CatalogTimeSlotRepository;
import com.petbuddy.petbuddystore.service.CatalogTimeSlotService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CatalogTimeSlotServiceImpl implements CatalogTimeSlotService {
    CatalogTimeSlotRepository catalogTimeSlotRepository;
    CatalogRepository catalogRepository;
    BookingRepository bookingRepository;
    TimeSlotMapper timeSlotMapper;

    @Override
    public TimeSlotResponse createTimeSlot(TimeSlotCreationRequest request) {
        validateMaxPets(request.getMaxPets());

        Catalog catalog = catalogRepository.findById(request.getCatalogId())
                .orElseThrow(() -> new AppException(ErrorCode.CATALOG_NOT_FOUND));

        if (catalogTimeSlotRepository.existsByCatalogCatalogIdAndDayOfWeekAndStartTime(
                request.getCatalogId(),
                request.getDayOfWeek(),
                request.getStartTime()
        )) {
            throw new AppException(ErrorCode.CATALOG_TIME_SLOT_EXISTED);
        }

        CatalogTimeSlot catalogTimeSlot = timeSlotMapper.toCatalogTimeSlot(request);
        catalogTimeSlot.setCatalog(catalog);
        if (catalogTimeSlot.getIsActive() == null) {
            catalogTimeSlot.setIsActive(true);
        }
        if (catalogTimeSlot.getMaxPets() == null) {
            catalogTimeSlot.setMaxPets(5);
        }

        return timeSlotMapper.toCatalogTimeSlotResponse(catalogTimeSlotRepository.save(catalogTimeSlot));
    }

    @Override
    public TimeSlotResponse getTimeSlotById(Integer timeSlotId) {
        return timeSlotMapper.toCatalogTimeSlotResponse(getTimeSlotEntity(timeSlotId));
    }

    @Override
    public List<TimeSlotResponse> getTimeSlotsByCatalog(Integer catalogId) {
        ensureCatalogExists(catalogId);
        return catalogTimeSlotRepository.findByCatalogCatalogIdOrderByDayOfWeekAscStartTimeAsc(catalogId)
                .stream()
                .map(timeSlotMapper::toCatalogTimeSlotResponse)
                .toList();
    }

    @Override
    public List<TimeSlotResponse> getTimeSlotsByCatalogAndDayOfWeek(Integer catalogId, DayOfWeek dayOfWeek) {
        ensureCatalogExists(catalogId);
        return catalogTimeSlotRepository.findByCatalogCatalogIdAndDayOfWeekOrderByStartTimeAsc(catalogId, dayOfWeek)
                .stream()
                .map(timeSlotMapper::toCatalogTimeSlotResponse)
                .toList();
    }

    @Override
    public List<TimeSlotResponse> getAvailableTimeSlotsBySelectedDate(Integer catalogId, LocalDate selectedDate) {
        ensureCatalogExists(catalogId);
        DayOfWeek dayOfWeek = selectedDate.getDayOfWeek();
        return catalogTimeSlotRepository
                .findByCatalogCatalogIdAndDayOfWeekAndIsActiveTrueOrderByStartTimeAsc(catalogId, dayOfWeek)
                .stream()
                .filter(timeSlot -> hasAvailableCapacity(timeSlot, selectedDate))
                .map(timeSlotMapper::toCatalogTimeSlotResponse)
                .toList();
    }

    @Override
    public TimeSlotResponse updateTimeSlot(Integer timeSlotId, TimeSlotUpdateRequest request) {
        validateMaxPets(request.getMaxPets());

        CatalogTimeSlot catalogTimeSlot = getTimeSlotEntity(timeSlotId);
        boolean timeChanged = !Objects.equals(catalogTimeSlot.getDayOfWeek(), request.getDayOfWeek())
                || !Objects.equals(catalogTimeSlot.getStartTime(), request.getStartTime());

        if (timeChanged && catalogTimeSlotRepository.existsByCatalogCatalogIdAndDayOfWeekAndStartTime(
                catalogTimeSlot.getCatalog().getCatalogId(),
                request.getDayOfWeek(),
                request.getStartTime()
        )) {
            throw new AppException(ErrorCode.CATALOG_TIME_SLOT_EXISTED);
        }

        timeSlotMapper.updateCatalogTimeSlot(catalogTimeSlot, request);
        return timeSlotMapper.toCatalogTimeSlotResponse(catalogTimeSlotRepository.save(catalogTimeSlot));
    }

    @Override
    public TimeSlotResponse updateActiveStatus(Integer timeSlotId, Boolean isActive) {
        CatalogTimeSlot catalogTimeSlot = getTimeSlotEntity(timeSlotId);
        catalogTimeSlot.setIsActive(isActive);
        return timeSlotMapper.toCatalogTimeSlotResponse(catalogTimeSlotRepository.save(catalogTimeSlot));
    }

    @Override
    public TimeSlotResponse toggleTimeSlotActive(Integer timeSlotId) {
        CatalogTimeSlot catalogTimeSlot = getTimeSlotEntity(timeSlotId);
        catalogTimeSlot.setIsActive(!Boolean.TRUE.equals(catalogTimeSlot.getIsActive()));
        return timeSlotMapper.toCatalogTimeSlotResponse(catalogTimeSlotRepository.save(catalogTimeSlot));
    }

    private CatalogTimeSlot getTimeSlotEntity(Integer timeSlotId) {
        return catalogTimeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new AppException(ErrorCode.CATALOG_TIME_SLOT_NOT_FOUND));
    }

    private void ensureCatalogExists(Integer catalogId) {
        if (!catalogRepository.existsById(catalogId)) {
            throw new AppException(ErrorCode.CATALOG_NOT_FOUND);
        }
    }

    private void validateMaxPets(Integer maxPets) {
        if (maxPets != null && maxPets <= 0) {
            throw new AppException(ErrorCode.INVALID_TIME_SLOT_CAPACITY);
        }
    }

    private boolean hasAvailableCapacity(CatalogTimeSlot timeSlot, LocalDate selectedDate) {
        LocalDateTime from = selectedDate.atStartOfDay();
        LocalDateTime to = selectedDate.plusDays(1).atStartOfDay();
        long bookedPets = bookingRepository.countBookedPetsInSlot(
                timeSlot.getId(),
                from,
                to,
                List.of(BookingStatus.FAILED, BookingStatus.CANCELLED)
        );
        int maxPets = timeSlot.getMaxPets() == null ? 5 : timeSlot.getMaxPets();
        return bookedPets < maxPets;
    }
}
