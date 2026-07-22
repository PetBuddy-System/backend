package com.petbuddy.petbuddystore.mapper;

import com.petbuddy.petbuddystore.dto.response.BookingResponse;
import com.petbuddy.petbuddystore.dto.response.BookingDetailResponse;
import com.petbuddy.petbuddystore.dto.response.MediaFileResponse;
import com.petbuddy.petbuddystore.model.Booking;
import com.petbuddy.petbuddystore.model.BookingDetail;
import com.petbuddy.petbuddystore.model.MediaFile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface BookingMapper {
    @Mapping(source = "staffSchedule.staffScheduleId", target = "staffScheduleId")
    @Mapping(source = "staffSchedule.staff.userId", target = "staffId")
    @Mapping(source = "staffSchedule.staff.fullName", target = "staffName")
    @Mapping(source = "bookingStatus", target = "bookingStatus")
    @Mapping(target = "estimatedEndAt", expression = "java(calculateEstimatedEndAt(booking))")
    @Mapping(target = "stripeClientSecret", ignore = true)
    BookingResponse toBookingResponse(Booking booking);

    @Mapping(source = "pet.petId", target = "petId")
    @Mapping(source = "catalog.catalogId", target = "catalogId")
    @Mapping(source = "timeSlot.id", target = "timeSlotId")
    @Mapping(target = "timeSlot", expression = "java(bookingDetail.getTimeSlot() == null ? null : bookingDetail.getTimeSlot().getStartTime().toString())")
    @Mapping(target = "petImage", expression = "java(resolvePetImage(bookingDetail))")
    @Mapping(source = "catalog.imageUrl", target = "catalogImage")
    BookingDetailResponse toBookingDetailResponse(BookingDetail bookingDetail);

    MediaFileResponse toMediaFileResponse(MediaFile mediaFile);

    default LocalDateTime calculateEstimatedEndAt(Booking booking) {
        if (booking == null || booking.getScheduledAt() == null || booking.getBookingDetails() == null) {
            return null;
        }
        int totalDuration = booking.getBookingDetails().stream()
                .mapToInt(detail -> {
                    if (detail.getTotalDurationMinute() != null) {
                        return detail.getTotalDurationMinute();
                    }
                    return detail.getDurationMinute() == null ? 0 : detail.getDurationMinute();
                })
                .sum();
        return booking.getScheduledAt().plusMinutes(totalDuration);
    }

    default String resolvePetImage(BookingDetail bookingDetail) {
        if (bookingDetail == null || bookingDetail.getPet() == null || bookingDetail.getPet().getMediaFiles() == null) {
            return null;
        }
        return bookingDetail.getPet().getMediaFiles().stream()
                .map(MediaFile::getFileUrl)
                .filter(fileUrl -> fileUrl != null && !fileUrl.isBlank())
                .findFirst()
                .orElse(null);
    }
}
