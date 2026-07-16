package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.BookingMediaType;
import com.petbuddy.petbuddystore.common.enums.MediaStatus;
import com.petbuddy.petbuddystore.model.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
    boolean existsByBookingDetail_Booking_BookingIdAndBookingMediaTypeAndMediaStatus(
            Integer bookingId,
            BookingMediaType bookingMediaType,
            MediaStatus mediaStatus
    );
}
