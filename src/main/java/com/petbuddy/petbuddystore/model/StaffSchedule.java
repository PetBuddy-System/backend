package com.petbuddy.petbuddystore.model;

import com.petbuddy.petbuddystore.common.enums.ScheduleStatus;
import com.petbuddy.petbuddystore.common.enums.ShiftType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "staff_schedules")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String staffScheduleId;

    @Column(columnDefinition = "TEXT")
    String note;

    LocalDateTime checkInAt;
    LocalDateTime checkOutAt;
    LocalDateTime assignedAt;

    @Enumerated(EnumType.STRING)
    ScheduleStatus scheduleStatus;

    @CreationTimestamp
    @Column(updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    User staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_schedule_id", nullable = false)
    WorkSchedule workSchedule;
}
