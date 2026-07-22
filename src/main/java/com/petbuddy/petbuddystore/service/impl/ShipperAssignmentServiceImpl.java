package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.*;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.common.util.CapacitatedKMeans;
import com.petbuddy.petbuddystore.common.util.GeoUtils;
import com.petbuddy.petbuddystore.configuration.DeliveryCapacityProperties;
import com.petbuddy.petbuddystore.dto.request.AssignReturnShipperRequest;
import com.petbuddy.petbuddystore.dto.response.*;
import com.petbuddy.petbuddystore.model.*;
import com.petbuddy.petbuddystore.repository.*;
import com.petbuddy.petbuddystore.service.OrsRoutingService;
import com.petbuddy.petbuddystore.service.ShipperAssignmentService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShipperAssignmentServiceImpl implements ShipperAssignmentService {

    StaffScheduleRepository staffScheduleRepository;
    OrderRepository orderRepository;
    ReturnRequestRepository returnRequestRepository;
    StoreLocationRepository storeLocationRepository;
    WorkScheduleRepository workScheduleRepository;
    OrsRoutingService orsRoutingService;
    DeliveryCapacityProperties capacityProperties;
    UserRepository userRepository;

    static double MAX_DISTANCE_FOR_ORS_CALL_KM = 7.0;
    static List<OrderStatus> ACTIVE_ORDER_STATUSES = List.of(OrderStatus.PICKED, OrderStatus.SHIPPING);
    static int MAX_LOOKAHEAD_DAYS = 60;

    @Override
    @Transactional
    public void recomputeDailyZones() {
        List<StaffSchedule> onDuty = staffScheduleRepository.findOnDutySchedules(
                LocalDate.now(), List.of(ScheduleStatus.SCHEDULED, ScheduleStatus.WORKING));

        List<StaffSchedule> shipperSchedules = onDuty.stream()
                .filter(s -> s.getStaff().getRole() == Role.STAFF
                        && s.getStaff().getStaffTask() == StaffTask.SHIPPER)
                .toList();

        if (shipperSchedules.isEmpty()) {
            log.info("Không có shipper nào đang trực, bỏ qua recomputeDailyZones");
            return;
        }

        StoreLocation store = storeLocationRepository.findByActiveTrue()
                .orElseThrow(() -> new AppException(ErrorCode.STORE_LOCATION_NOT_FOUND));

        List<Order> pendingOrders = orderRepository
                .findByStatusAndLatitudeIsNotNullAndLongitudeIsNotNull(OrderStatus.PICKED);

        if (pendingOrders.isEmpty()) {
            log.info("Không có đơn PICKED nào có tọa độ, bỏ qua recomputeDailyZones");
            return;
        }

        List<Order> inRangeOrders = new ArrayList<>();
        List<Order> outOfRangeOrders = new ArrayList<>();
        for (Order o : pendingOrders) {
            double distFromStore = GeoUtils.distanceKm(
                    store.getLatitude(), store.getLongitude(), o.getLatitude(), o.getLongitude());
            if (distFromStore > capacityProperties.getMaxOperationalRadiusKm()) {
                outOfRangeOrders.add(o);
            } else {
                inRangeOrders.add(o);
            }
        }
        if (!outOfRangeOrders.isEmpty()) {
            log.warn("{} đơn vượt bán kính vận hành {}km, cần xử lý thủ công. orderIds={}",
                    outOfRangeOrders.size(), capacityProperties.getMaxOperationalRadiusKm(),
                    outOfRangeOrders.stream().map(Order::getOrderId).toList());
        }
        if (inRangeOrders.isEmpty()) {
            log.info("Không còn đơn nào trong bán kính vận hành, bỏ qua recomputeDailyZones");
            return;
        }

        int n = shipperSchedules.size();
        List<Double> initLat = new ArrayList<>();
        List<Double> initLng = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            StaffSchedule s = shipperSchedules.get(i);
            if (s.getZoneCenterLat() != null && s.getZoneCenterLng() != null) {
                initLat.add(s.getZoneCenterLat());
                initLng.add(s.getZoneCenterLng());
            } else {
                double angle = 2 * Math.PI * i / n;
                initLat.add(store.getLatitude() + 0.02 * Math.cos(angle));
                initLng.add(store.getLongitude() + 0.02 * Math.sin(angle));
            }
        }

        List<CapacitatedKMeans.WeightedPoint> points = inRangeOrders.stream()
                .map(o -> new CapacitatedKMeans.WeightedPoint(
                        o.getOrderId(), o.getLatitude(), o.getLongitude(), 1.0))
                .toList();

        double roughCapacityEach = Math.ceil((double) inRangeOrders.size() / n);
        List<Double> roughCapacities = new ArrayList<>(Collections.nCopies(n, roughCapacityEach));

        List<CapacitatedKMeans.Cluster> roughClusters = CapacitatedKMeans.cluster(
                points, initLat, initLng, roughCapacities, capacityProperties.getKMeansMaxIterations());

        Map<Long, Order> orderById = inRangeOrders.stream()
                .collect(Collectors.toMap(Order::getOrderId, o -> o));

        List<Double> realCapacities = new ArrayList<>();
        List<Integer> realCapacityInts = new ArrayList<>();

        for (int i = 0; i < roughClusters.size(); i++) {
            CapacitatedKMeans.Cluster cluster = roughClusters.get(i);
            List<Order> clusterOrders = cluster.assignedPointIds().stream()
                    .map(orderById::get)
                    .toList();

            int realCapacity = estimateMaxOrdersForRoute(store, clusterOrders, shipperSchedules.get(i));
            realCapacityInts.add(realCapacity);
            realCapacities.add((double) realCapacity);
        }

        List<Double> pass1CenterLat = roughClusters.stream().map(CapacitatedKMeans.Cluster::centerLat).toList();
        List<Double> pass1CenterLng = roughClusters.stream().map(CapacitatedKMeans.Cluster::centerLng).toList();

        List<CapacitatedKMeans.Cluster> finalClusters = CapacitatedKMeans.cluster(
                points, pass1CenterLat, pass1CenterLng, realCapacities, capacityProperties.getKMeansMaxIterations());

        for (int i = 0; i < n; i++) {
            StaffSchedule schedule = shipperSchedules.get(i);
            CapacitatedKMeans.Cluster cluster = finalClusters.get(i);

            schedule.setZoneCenterLat(cluster.centerLat());
            schedule.setZoneCenterLng(cluster.centerLng());
            schedule.setMaxOrderCapacity(realCapacityInts.get(i));

            staffScheduleRepository.save(schedule);

            log.info("Shipper {} - zone=({}, {}) - maxOrderCapacity = {}",
                    schedule.getStaff().getUserId(), cluster.centerLat(), cluster.centerLng(),
                    realCapacityInts.get(i));
        }
    }

    @Override
    @Transactional
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

            Integer maxCapacity = schedule.getMaxOrderCapacity();
            if (maxCapacity == null) {
                log.warn("StaffSchedule {} chưa có maxOrderCapacity, cần chạy recomputeDailyZones trước",
                        schedule.getStaffScheduleId());
                continue;
            }

            int currentLoad = countActiveOrders(schedule);
            if (currentLoad >= maxCapacity) continue;

            Double distance = distanceToShipperZone(order, schedule);

            suggestions.add(ShipperSuggestionResponse.builder()
                    .staffId(schedule.getStaff().getUserId())
                    .staffName(schedule.getStaff().getFullName())
                    .staffEmail(schedule.getStaff().getEmail())
                    .staffTask(schedule.getStaff().getStaffTask())
                    .currentLoad(currentLoad)
                    .maxCapacity(maxCapacity)
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

        validateShipperCapacityForOrder(order, schedule);

        order.setStaffSchedule(schedule);
        order.setShippedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.SHIPPING);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    private void validateShipperCapacityForOrder(Order order, StaffSchedule schedule) {
        if (order.getLatitude() == null || order.getLongitude() == null) {
            throw new AppException(ErrorCode.ROUTE_CALCULATION_FAILED);
        }

        StoreLocation store = storeLocationRepository.findByActiveTrue()
                .orElseThrow(() -> new AppException(ErrorCode.STORE_LOCATION_NOT_FOUND));

        List<Order> committedOrders = schedule.getOrders().stream()
                .filter(o -> ACTIVE_ORDER_STATUSES.contains(o.getStatus()))
                .filter(o -> o.getLatitude() != null && o.getLongitude() != null)
                .toList();

        List<Order> routeOrders = new ArrayList<>(committedOrders);
        routeOrders.add(order);

        WorkSchedule workSchedule = schedule.getWorkSchedule();
        LocalDateTime shiftEnd = LocalDateTime.of(workSchedule.getWorkDate(), workSchedule.getEndTime());
        double remainingShiftMinutes = Duration.between(LocalDateTime.now(), shiftEnd).toMinutes();

        if (remainingShiftMinutes <= 0) {
            throw new AppException(ErrorCode.SHIPPER_CAPACITY_FULL);
        }

        double availableMinutes = remainingShiftMinutes * (1 - capacityProperties.getSafetyBufferPercent() / 100.0);
        double maxWeightGrams = capacityProperties.getMaxLoadWeightKg() * 1000.0;

        RouteSimulationResult result = simulateRoute(
                store.getLatitude(), store.getLongitude(), routeOrders, availableMinutes, maxWeightGrams);

        if (!result.allOrdersFit()) {
            throw new AppException(ErrorCode.SHIPPER_CAPACITY_FULL);
        }
    }

    private RouteSimulationResult simulateRoute(double startLat, double startLon, List<Order> orders,
                                                double availableMinutes, double maxWeightGrams) {
        List<Order> remaining = new ArrayList<>(orders);
        double currentLat = startLat;
        double currentLon = startLon;
        double elapsedMinutes = 0;
        double currentWeightGrams = 0;
        int fitted = 0;

        while (!remaining.isEmpty()) {
            Order nearest = null;
            double nearestEstDistance = Double.MAX_VALUE;

            for (Order candidate : remaining) {
                double d = GeoUtils.estimateRoadDistanceKm(
                        GeoUtils.distanceKm(currentLat, currentLon, candidate.getLatitude(), candidate.getLongitude()));
                if (d < nearestEstDistance) {
                    nearestEstDistance = d;
                    nearest = candidate;
                }
            }

            double actualDistance = resolveDistanceKm(currentLat, currentLon, nearest.getLatitude(), nearest.getLongitude());
            double legMinutes = (actualDistance / capacityProperties.getAvgSpeedKmh()) * 60;
            double handlingMinutes = capacityProperties.getHandlingTimeMinutes();
            double projectedMinutes = elapsedMinutes + legMinutes + handlingMinutes;

            double nearestWeightGrams = orderWeightGrams(nearest);
            double projectedWeightGrams = currentWeightGrams + nearestWeightGrams;

            if (projectedMinutes > availableMinutes || projectedWeightGrams > maxWeightGrams) {
                break;
            }

            elapsedMinutes = projectedMinutes;
            currentWeightGrams = projectedWeightGrams;
            currentLat = nearest.getLatitude();
            currentLon = nearest.getLongitude();
            remaining.remove(nearest);
            fitted++;
        }

        return new RouteSimulationResult(fitted, elapsedMinutes, currentWeightGrams, fitted == orders.size());
    }

    private Double distanceToShipperZone(Order order, StaffSchedule schedule) {
        if (order.getLatitude() == null || order.getLongitude() == null) {
            return null;
        }

        if (schedule.getZoneCenterLat() != null && schedule.getZoneCenterLng() != null) {
            return resolveDistanceKm(order.getLatitude(), order.getLongitude(),
                    schedule.getZoneCenterLat(), schedule.getZoneCenterLng());
        }

        List<Order> ordersWithLocation = schedule.getOrders().stream()
                .filter(o -> o.getLatitude() != null && o.getLongitude() != null)
                .toList();
        if (ordersWithLocation.isEmpty()) return null;

        double avgLat = ordersWithLocation.stream().mapToDouble(Order::getLatitude).average().orElse(0);
        double avgLng = ordersWithLocation.stream().mapToDouble(Order::getLongitude).average().orElse(0);
        return resolveDistanceKm(order.getLatitude(), order.getLongitude(), avgLat, avgLng);
    }

    private int countActiveOrders(StaffSchedule schedule) {
        return (int) schedule.getOrders().stream()
                .filter(o -> ACTIVE_ORDER_STATUSES.contains(o.getStatus()))
                .count();
    }

    @Override
    @Transactional
    public List<RestockEligibilityResponse> getShippersEligibleForRestock() {
        List<StaffSchedule> onDuty = staffScheduleRepository.findOnDutySchedules(
                LocalDate.now(), List.of(ScheduleStatus.WORKING));

        StoreLocation store = storeLocationRepository.findByActiveTrue()
                .orElseThrow(() -> new AppException(ErrorCode.STORE_LOCATION_NOT_FOUND));

        List<RestockEligibilityResponse> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (StaffSchedule schedule : onDuty) {
            if (schedule.getStaff().getRole() != Role.STAFF
                    || schedule.getStaff().getStaffTask() != StaffTask.SHIPPER) {
                continue;
            }
            if (schedule.getCheckInAt() == null || schedule.getCheckOutAt() != null) {
                continue;
            }

            List<Order> pendingOrders = schedule.getOrders().stream()
                    .filter(o -> o.getStatus() == OrderStatus.SHIPPING)
                    .toList();

            if (pendingOrders.size() > capacityProperties.getRestockThresholdOrders()) {
                continue;
            }

            LocalDateTime shiftEndDateTime = LocalDateTime.of(
                    schedule.getWorkSchedule().getWorkDate(),
                    schedule.getWorkSchedule().getEndTime());

            double remainingShiftMinutes = Duration.between(now, shiftEndDateTime).toMinutes();
            if (remainingShiftMinutes <= 0) {
                continue;
            }

            double fromLat, fromLng;
            if (!pendingOrders.isEmpty()) {
                Order lastKnown = pendingOrders.get(pendingOrders.size() - 1);
                fromLat = lastKnown.getLatitude();
                fromLng = lastKnown.getLongitude();
            } else if (schedule.getZoneCenterLat() != null) {
                fromLat = schedule.getZoneCenterLat();
                fromLng = schedule.getZoneCenterLng();
            } else {
                continue;
            }

            double distanceToStore = resolveDistanceKm(fromLat, fromLng, store.getLatitude(), store.getLongitude());
            double returnMinutes = (distanceToStore / capacityProperties.getAvgSpeedKmh()) * 60;

            double minRoundTripMinutes = 2 * returnMinutes + capacityProperties.getHandlingTimeMinutes();

            boolean eligible = remainingShiftMinutes > minRoundTripMinutes;

            result.add(RestockEligibilityResponse.builder()
                    .staffId(schedule.getStaff().getUserId())
                    .staffName(schedule.getStaff().getFullName())
                    .currentPendingOrders(pendingOrders.size())
                    .remainingShiftMinutes(remainingShiftMinutes)
                    .distanceToStoreKm(distanceToStore)
                    .eligibleForRestock(eligible)
                    .estimatedExtraOrders(eligible
                            ? estimateExtraOrdersInRemainingTime(remainingShiftMinutes, returnMinutes)
                            : 0)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional
    public List<ReturnShipperResponse> getAvailableShippers() {

        List<StaffSchedule> schedules = staffScheduleRepository.findOnDutySchedules(
                LocalDate.now(),
                List.of(
                        ScheduleStatus.SCHEDULED,
                        ScheduleStatus.WORKING
                )
        );

        List<ReturnStatus> activeStatuses = List.of(
                ReturnStatus.APPROVED,
                ReturnStatus.PICKING_UP,
                ReturnStatus.PICKED_UP,
                ReturnStatus.PICKUP_FAILED,
                ReturnStatus.RETURNED_TO_STORE,
                ReturnStatus.READY_TO_DELIVER,
                ReturnStatus.DELIVERING,
                ReturnStatus.DELIVERING_FAILED
        );

        return schedules.stream()
                .filter(schedule ->
                        schedule.getStaff().getRole() == Role.STAFF
                                && schedule.getStaff().getStaffTask() == StaffTask.SHIPPER
                )
                .map(schedule -> {

                    User staff = schedule.getStaff();

                    long activeReturnCount =
                            returnRequestRepository.countByShipper_UserIdAndStatusIn(
                                    staff.getUserId(),
                                    activeStatuses
                            );

                    return ReturnShipperResponse.builder()
                            .staffId(staff.getUserId())
                            .staffName(staff.getFullName())
                            .staffEmail(staff.getEmail())
                            .activeReturnCount((int) activeReturnCount)
                            .build();
                })
                .sorted(Comparator.comparing(ReturnShipperResponse::getActiveReturnCount))
                .toList();
    }
    @Override
    @Transactional
    public void assignReturnShipper(Long returnRequestId,
                                    AssignReturnShipperRequest request) {

        ReturnRequest returnRequest = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_REQUEST_NOT_FOUND));

        if (returnRequest.getStatus() != ReturnStatus.APPROVED
                && returnRequest.getStatus() != ReturnStatus.DELIVERING_FAILED
                && returnRequest.getStatus() != ReturnStatus.PICKUP_FAILED
                && returnRequest.getStatus() != ReturnStatus.REJECTED_RETURN_SHIPPING
                && returnRequest.getStatus() != ReturnStatus.RETURNED_TO_STORE) {
            throw new AppException(ErrorCode.INVALID_RETURN_STATUS);
        }

        User shipper = userRepository.findById(request.getShipperId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (shipper.getRole() != Role.STAFF
                || shipper.getStaffTask() != StaffTask.SHIPPER) {
            throw new AppException(ErrorCode.INVALID_SHIPPER);
        }

        boolean onDuty = staffScheduleRepository.findOnDutySchedules(
                        LocalDate.now(),
                        List.of(ScheduleStatus.SCHEDULED, ScheduleStatus.WORKING)
                ).stream()
                .anyMatch(schedule ->
                        schedule.getStaff().getUserId().equals(shipper.getUserId()));

        if (!onDuty) {
            throw new AppException(ErrorCode.SHIPPER_NOT_ON_DUTY);
        }

        returnRequest.setShipper(shipper);
        returnRequest.setPickupFailedCount(0);
        returnRequest.setDeliveryFailedCount(0);

        // If reassigned after a failed delivery, reset to READY_TO_DELIVER
        if (returnRequest.getStatus() == ReturnStatus.DELIVERING_FAILED) {
            returnRequest.setStatus(ReturnStatus.READY_TO_DELIVER);
        }

        // If reassigned after a failed pickup, reset to APPROVED
        if (returnRequest.getStatus() == ReturnStatus.PICKUP_FAILED) {
            returnRequest.setStatus(ReturnStatus.APPROVED);
        }

        if (returnRequest.getStatus() == ReturnStatus.REJECTED_RETURN_SHIPPING) {
            returnRequest.setStatus(ReturnStatus.READY_TO_DELIVER);
        }

        if (returnRequest.getStatus() == ReturnStatus.RETURNED_TO_STORE) {
            returnRequest.setStatus(ReturnStatus.READY_TO_DELIVER);
        }

        returnRequestRepository.save(returnRequest);
    }
    @Override
    @Transactional
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

    @Override
    @Transactional
    public void updateEstimatedDeliveryTime(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (order.getLatitude() == null || order.getLongitude() == null) {
            log.warn("Đơn {} chưa có tọa độ, bỏ qua ước tính thời gian giao hàng", orderId);
            return;
        }

        LocalDateTime estimatedAt = estimateForUnassignedOrder(order);

        order.setEstimatedDeliveryAt(estimatedAt);
        orderRepository.save(order);
    }

    private LocalDateTime estimateForUnassignedOrder(Order order) {
        StoreLocation store = storeLocationRepository.findByActiveTrue()
                .orElseThrow(() -> new AppException(ErrorCode.STORE_LOCATION_NOT_FOUND));

        double distanceKm = resolveDistanceKm(store.getLatitude(), store.getLongitude(),
                order.getLatitude(), order.getLongitude());

        double travelMinutes = (distanceKm / capacityProperties.getAvgSpeedKmh()) * 60;
        double totalMinutes = capacityProperties.getPendingAssignmentBufferMinutes()
                + travelMinutes
                + capacityProperties.getHandlingTimeMinutes();

        LocalDateTime afterPickup = addWarehousePickupDays(
                LocalDateTime.now(),
                capacityProperties.getWarehousePickupDays()
        );
        LocalDateTime rawEstimatedAt = afterPickup.plusMinutes(Math.round(totalMinutes));
        return clampToActualWorkingHours(rawEstimatedAt);
    }

    private LocalDateTime addWarehousePickupDays(LocalDateTime from, int pickupDays) {
        LocalDate date = from.toLocalDate();
        int added = 0;

        while (added < pickupDays) {
            date = findNextWorkingDate(date.plusDays(1));
            added++;
        }

        LocalTime earliestStart = getEarliestStartOrThrow(date);
        return LocalDateTime.of(date, earliestStart);
    }

    private LocalDateTime clampToActualWorkingHours(LocalDateTime estimatedAt) {
        LocalDate date = findNextWorkingDate(estimatedAt.toLocalDate());

        LocalTime earliestStart = getEarliestStartOrThrow(date);
        LocalTime latestEnd = workScheduleRepository.findLatestEndTimeWithActiveStaff(date)
                .orElseThrow(() -> new AppException(ErrorCode.WORK_SCHEDULE_NOT_EXISTED));

        LocalDateTime dayStart = LocalDateTime.of(date, earliestStart);
        LocalDateTime dayEnd = LocalDateTime.of(date, latestEnd);

        if (!estimatedAt.toLocalDate().equals(date) || estimatedAt.isBefore(dayStart)) {
            return dayStart;
        }

        if (estimatedAt.isAfter(dayEnd)) {
            LocalDate nextDate = findNextWorkingDate(date.plusDays(1));
            return LocalDateTime.of(nextDate, getEarliestStartOrThrow(nextDate));
        }

        return estimatedAt;
    }

    private LocalDate findNextWorkingDate(LocalDate fromDate) {
        LocalDate date = fromDate;

        for (int i = 0; i <= MAX_LOOKAHEAD_DAYS; i++) {
            if (workScheduleRepository.existsByWorkDateWithActiveStaff(date)) {
                return date;
            }
            date = date.plusDays(1);
        }

        throw new AppException(ErrorCode.WORK_SCHEDULE_NOT_EXISTED);
    }

    private LocalTime getEarliestStartOrThrow(LocalDate date) {
        return workScheduleRepository.findEarliestStartTimeWithActiveStaff(date)
                .orElseThrow(() -> new AppException(ErrorCode.WORK_SCHEDULE_NOT_EXISTED));
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
                        GeoUtils.distanceKm(currentLat, currentLon, candidate.getLatitude(), candidate.getLongitude()));
                if (d < nearestDistance) {
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
                    .status(nearest.getStatus())
                    .finalAmount(nearest.getFinalAmount())
                    .paymentMethod(nearest.getPayment() != null ? nearest.getPayment().getPaymentMethod() : null)
                    .paymentStatus(nearest.getPayment() != null ? nearest.getPayment().getStatus() : null)
                    .estimatedDeliveryAt(nearest.getEstimatedDeliveryAt())
                    .build());

            currentLat = nearest.getLatitude();
            currentLon = nearest.getLongitude();
            remaining.remove(nearest);
        }

        return route;
    }

    private double resolveDistanceKm(double storeLat, double storeLon, double destLat, double destLon) {
        double haversine = GeoUtils.distanceKm(storeLat, storeLon, destLat, destLon);

        if (haversine < MAX_DISTANCE_FOR_ORS_CALL_KM) {
            return haversine;
        }

        try {
            return orsRoutingService.getRoadDistanceKm(storeLat, storeLon, destLat, destLon);
        } catch (Exception e) {
            log.warn("ORS call failed for store=({}, {}) dest=({}, {}), falling back to estimated distance",
                    storeLat, storeLon, destLat, destLon, e);
            return GeoUtils.estimateRoadDistanceKm(haversine);
        }
    }

    private int estimateMaxOrdersForRoute(StoreLocation store, List<Order> clusterOrders, StaffSchedule schedule) {
        if (clusterOrders.isEmpty()) return 0;

        WorkSchedule workSchedule = schedule.getWorkSchedule();
        double shiftMinutes = Duration.between(
                workSchedule.getStartTime(), workSchedule.getEndTime()).toMinutes();
        double availableMinutes = shiftMinutes * (1 - capacityProperties.getSafetyBufferPercent() / 100.0);
        double maxWeightGrams = capacityProperties.getMaxLoadWeightKg() * 1000.0;

        RouteSimulationResult result = simulateRoute(
                store.getLatitude(), store.getLongitude(), clusterOrders, availableMinutes, maxWeightGrams);

        if (result.ordersFit() == 0) {
            log.warn("Không có đơn nào trong cụm vừa với 1 shipper trong ca (thời gian/tải trọng), staffId={}",
                    schedule.getStaff().getUserId());
        }

        return result.ordersFit();
    }

    record RouteSimulationResult(int ordersFit, double elapsedMinutes,
                                 double weightGrams, boolean allOrdersFit) {}

    private int estimateExtraOrdersInRemainingTime(double remainingMinutes, double returnTripMinutes) {
        double usableMinutes = remainingMinutes - returnTripMinutes;
        if (usableMinutes <= 0) return 0;
        double perOrderMinutes = capacityProperties.getHandlingTimeMinutes()
                + (capacityProperties.getMaxOperationalRadiusKm() / 3.0 / capacityProperties.getAvgSpeedKmh()) * 60;
        return (int) Math.floor(usableMinutes / perOrderMinutes);
    }

    private double orderWeightGrams(Order order) {
        if (order.getOrderDetails() == null) return 0;
        return order.getOrderDetails().stream()
                .mapToDouble(od -> {
                    int weight = od.getWeight() != null ? od.getWeight() : 0;
                    int quantity = od.getQuantity() != null ? od.getQuantity() : 0;
                    return (double) weight * quantity;
                })
                .sum();
    }
}