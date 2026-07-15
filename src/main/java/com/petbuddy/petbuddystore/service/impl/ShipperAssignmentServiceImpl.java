package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.OrderStatus;
import com.petbuddy.petbuddystore.common.enums.Role;
import com.petbuddy.petbuddystore.common.enums.ScheduleStatus;
import com.petbuddy.petbuddystore.common.enums.StaffTask;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.common.util.GeoUtils;
import com.petbuddy.petbuddystore.dto.response.DeliveryStopResponse;
import com.petbuddy.petbuddystore.dto.response.ShipperSuggestionResponse;
import com.petbuddy.petbuddystore.model.Order;
import com.petbuddy.petbuddystore.model.StaffSchedule;
import com.petbuddy.petbuddystore.model.StoreLocation;
import com.petbuddy.petbuddystore.repository.OrderRepository;
import com.petbuddy.petbuddystore.repository.StaffScheduleRepository;
import com.petbuddy.petbuddystore.repository.StoreLocationRepository;
import com.petbuddy.petbuddystore.service.OrsRoutingService;
import com.petbuddy.petbuddystore.service.ShipperAssignmentService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShipperAssignmentServiceImpl implements ShipperAssignmentService {

    StaffScheduleRepository staffScheduleRepository;
    OrderRepository orderRepository;
    StoreLocationRepository storeLocationRepository;
    OrsRoutingService orsRoutingService;

    static double MAX_DISTANCE_FOR_ORS_CALL_KM = 7.0;
    static double MAX_CLUSTER_DISTANCE_KM = 7.0;

    @Override
    public List<ShipperSuggestionResponse> getShipperSuggestions(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PICKED) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        List<StaffSchedule> onDuty = staffScheduleRepository.findOnDutySchedules(
                LocalDate.now(), List.of(ScheduleStatus.SCHEDULED, ScheduleStatus.WORKING));

        List<ShipperSuggestionResponse> suggestions = new ArrayList<>();

        for (StaffSchedule schedule : onDuty) {
            if (schedule.getStaff().getRole() != Role.STAFF
                    || schedule.getStaff().getStaffTask() != StaffTask.SHIPPER) {
                continue;
            }
            int currentLoad = schedule.getOrders().size();
            if (currentLoad >= schedule.getMaxOrderCapacity()) continue;

            List<Order> ordersWithLocation = schedule.getOrders().stream()
                    .filter(o -> o.getLatitude() != null && o.getLongitude() != null)
                    .toList();

            Double distance = null;
            if (!ordersWithLocation.isEmpty()) {
                double avgLat = ordersWithLocation.stream().mapToDouble(Order::getLatitude).average().orElse(0);
                double avgLng = ordersWithLocation.stream().mapToDouble(Order::getLongitude).average().orElse(0);
                distance = resolveDistanceKm(order.getLatitude(), order.getLongitude(), avgLat, avgLng);
            }

            suggestions.add(ShipperSuggestionResponse.builder()
                    .staffId(schedule.getStaff().getUserId())
                    .staffName(schedule.getStaff().getFullName())
                    .staffEmail(schedule.getStaff().getEmail())
                    .staffTask(schedule.getStaff().getStaffTask())
                    .currentLoad(currentLoad)
                    .maxCapacity(schedule.getMaxOrderCapacity())
                    .distanceToClusterKm(distance)
                    .build());
        }

        suggestions.sort(Comparator
                .comparing((ShipperSuggestionResponse s) -> s.getDistanceToClusterKm() == null)
                .thenComparing(s -> s.getDistanceToClusterKm() == null ? 0.0 : s.getDistanceToClusterKm())
                .thenComparing(ShipperSuggestionResponse::getCurrentLoad));

        return suggestions;
    }

    @Override
    @Transactional
    public void assignShipper(Long orderId, String staffId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.PICKED) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        StaffSchedule schedule = staffScheduleRepository
                .findTodayScheduleByStaffId(staffId, LocalDate.now(),
                        List.of(ScheduleStatus.SCHEDULED, ScheduleStatus.WORKING))
                .orElseThrow(() -> new AppException(ErrorCode.SHIPPER_NOT_ON_DUTY));

        if (schedule.getStaff().getRole() != Role.STAFF || schedule.getStaff().getStaffTask() != StaffTask.SHIPPER) {
            throw new AppException(ErrorCode.NOT_SHIPPER);
        }

        long currentLoad = orderRepository.countByStaffSchedule_StaffScheduleId(schedule.getStaffScheduleId());
        if (currentLoad >= schedule.getMaxOrderCapacity()) {
            throw new AppException(ErrorCode.SHIPPER_CAPACITY_FULL);
        }

        validateClusterDistance(order, schedule);

        order.setStaffSchedule(schedule);
        order.setShippedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.SHIPPING);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    private void validateClusterDistance(Order order, StaffSchedule schedule) {
        if (order.getLatitude() == null || order.getLongitude() == null) {
            return;
        }

        List<Order> ordersWithLocation = schedule.getOrders().stream()
                .filter(o -> o.getLatitude() != null && o.getLongitude() != null)
                .toList();

        if (ordersWithLocation.isEmpty()) {
            return;
        }

        double avgLat = ordersWithLocation.stream().mapToDouble(Order::getLatitude).average().orElse(0);
        double avgLng = ordersWithLocation.stream().mapToDouble(Order::getLongitude).average().orElse(0);

        double distance = resolveDistanceKm(order.getLatitude(), order.getLongitude(), avgLat, avgLng);

        if (distance > MAX_CLUSTER_DISTANCE_KM) {
            throw new AppException(ErrorCode.SHIPPER_TOO_FAR_FROM_CLUSTER);
        }
    }

    @Override
    public List<DeliveryStopResponse> suggestDeliveryRoute(String staffId) {
        StaffSchedule schedule = staffScheduleRepository
                .findTodayScheduleByStaffId(staffId, LocalDate.now(),
                        List.of(ScheduleStatus.SCHEDULED, ScheduleStatus.WORKING))
                .orElseThrow(() -> new AppException(ErrorCode.SHIPPER_NOT_ON_DUTY));

        List<Order> ordersToDeliver = schedule.getOrders().stream()
                .filter(o -> o.getStatus() == OrderStatus.SHIPPING)
                .filter(o -> o.getLatitude() != null && o.getLongitude() != null)
                .toList();

        if (ordersToDeliver.isEmpty()) {
            return List.of();
        }

        StoreLocation store = storeLocationRepository.findByActiveTrue()
                .orElseThrow(() -> new AppException(ErrorCode.STORE_LOCATION_NOT_FOUND));

        return buildNearestNeighborRoute(store.getLatitude(), store.getLongitude(), ordersToDeliver);
    }

    private List<DeliveryStopResponse> buildNearestNeighborRoute(double startLat, double startLon, List<Order> orders) {
        List<Order> remaining = new ArrayList<>(orders);
        List<DeliveryStopResponse> route = new ArrayList<>();

        double currentLat = startLat;
        double currentLon = startLon;
        int sequence = 1;

        while (!remaining.isEmpty()) {
            Order nearest = null;
            double nearestDistance = Double.MAX_VALUE;

            for (Order candidate : remaining) {
                double d = GeoUtils.estimateRoadDistanceKm(
                        GeoUtils.distanceKm(currentLat, currentLon, candidate.getLatitude(), candidate.getLongitude()));                if (d < nearestDistance) {
                    nearestDistance = d;
                    nearest = candidate;
                }
            }

            if (nearest == null) {
                throw new AppException(ErrorCode.ROUTE_CALCULATION_FAILED);
            }
            route.add(DeliveryStopResponse.builder()
                    .orderId(nearest.getOrderId())
                    .orderCode(nearest.getOrderCode())
                    .address(nearest.getAddress())
                    .recipientName(nearest.getRecipientName())
                    .phoneNumber(nearest.getPhoneNumber())
                    .sequence(sequence++)
                    .distanceFromPreviousKm(nearestDistance)
                    .build());

            currentLat = nearest.getLatitude();
            currentLon = nearest.getLongitude();
            remaining.remove(nearest);
        }

        return route;
    }

    private double resolveDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double haversine = GeoUtils.distanceKm(lat1, lon1, lat2, lon2);

        if (haversine > MAX_DISTANCE_FOR_ORS_CALL_KM) {
            return GeoUtils.estimateRoadDistanceKm(haversine);
        }

        try {
            return orsRoutingService.getRoadDistanceKm(lat1, lon1, lat2, lon2);
        } catch (Exception e) {
            return GeoUtils.estimateRoadDistanceKm(haversine);
        }
    }
}