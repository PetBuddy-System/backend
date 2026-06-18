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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_schedules")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "work_schedule_id")
    String workScheduleId;

    @Column(nullable = false)
    LocalDate workDate;

    @Column(nullable = false)
    LocalTime startTime;

    @Column(nullable = false)
    LocalTime endTime;

    @Column(columnDefinition = "TEXT")
    String note;

    @Enumerated(EnumType.STRING)
    ShiftType shiftType;

    @CreationTimestamp
    @Column(updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;

    @OneToMany(mappedBy = "workSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    List<StaffSchedule> staffSchedules = new ArrayList<>();

}
