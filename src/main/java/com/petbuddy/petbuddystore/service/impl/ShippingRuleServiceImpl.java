package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.common.util.GeoUtils;
import com.petbuddy.petbuddystore.dto.request.ShippingRuleRequest;
import com.petbuddy.petbuddystore.dto.response.ShippingFeeResponse;
import com.petbuddy.petbuddystore.dto.response.ShippingRuleResponse;
import com.petbuddy.petbuddystore.mapper.ShippingMapper;
import com.petbuddy.petbuddystore.model.ShippingRule;
import com.petbuddy.petbuddystore.model.StoreLocation;
import com.petbuddy.petbuddystore.repository.ShippingRuleRepository;
import com.petbuddy.petbuddystore.repository.StoreLocationRepository;
import com.petbuddy.petbuddystore.service.OrsRoutingService;
import com.petbuddy.petbuddystore.service.ShippingRuleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import com.petbuddy.petbuddystore.configuration.GeoBoundaryConfig.GeoBoundaries;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShippingRuleServiceImpl implements ShippingRuleService {

    static double FREE_SHIP_RADIUS = 5.0;
    static double MAX_DISTANCE_FOR_ORS_CALL_KM = 15.0;

    ShippingRuleRepository shippingRuleRepository;
    StoreLocationRepository storeLocationRepository;
    ShippingMapper shippingMapper;
    OrsRoutingService orsRoutingService;
    GeoBoundaries geoBoundaries;

    static GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Override
    public ShippingFeeResponse calculateFee(Double latitude, Double longitude) {
        if (latitude == null || (latitude < -90 || latitude > 90) ||
                longitude == null || (longitude < -180 || longitude > 180)) {
            throw new AppException(ErrorCode.INVALID_COORDINATES);
        }

        validateLocation(latitude, longitude);

        StoreLocation store = storeLocationRepository.findByActiveTrue()
                .orElseThrow(() -> new AppException(ErrorCode.STORE_LOCATION_NOT_FOUND));

        double distance = resolveDistanceKm(
                store.getLatitude(), store.getLongitude(), latitude, longitude);

        if (distance <= FREE_SHIP_RADIUS) {
            return shippingMapper.toResponse(distance, BigDecimal.ZERO, true);
        }

        ShippingRule config = shippingRuleRepository.findRuleByDistance(distance)
                .orElseThrow(() -> new AppException(ErrorCode.SHIPPING_CONFIG_NOT_FOUND));
        return shippingMapper.toResponse(distance, config.getFee(), false);
    }


    @Override
    public ShippingRuleResponse createRule(ShippingRuleRequest request) {
        boolean exists = shippingRuleRepository.existsOverlap(request.getMinDistance(), request.getMaxDistance());
        if (exists) {
            throw new AppException(ErrorCode.SHIPPING_RULE_OVERLAP);
        }

        if (request.getMinDistance() > request.getMaxDistance()) {
            throw new AppException(ErrorCode.INVALID_DISTANCE);
        }

        ShippingRule fee = shippingMapper.toShipping(request);
        return shippingMapper.toShippingResponse(shippingRuleRepository.save(fee));
    }

    @Override
    public ShippingRuleResponse updateRule(Long id, ShippingRuleRequest request) {
        ShippingRule rule = shippingRuleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHIPPING_CONFIG_NOT_FOUND));
        rule.setMinDistance(request.getMinDistance());
        rule.setMaxDistance(request.getMaxDistance());
        rule.setFee(request.getFee());

        return shippingMapper.toShippingResponse(shippingRuleRepository.save(rule));
    }

    @Override
    public List<ShippingRule> getAllShippingRules() {
        return shippingRuleRepository.findAll();
    }

    @Override
    public ShippingRule getShippingRuleById(Long id) {
        return shippingRuleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHIPPING_CONFIG_NOT_FOUND));
    }

    @Override
    public void deleteShippingRule(Long id) {
        ShippingRule rule = shippingRuleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHIPPING_CONFIG_NOT_FOUND));

        shippingRuleRepository.delete(rule);
    }

    @Override
    public void validateLocation(double lat, double lon) {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat));

        if (!geoBoundaries.hcmBoundaryGeometry().covers(point)) {
            throw new AppException(ErrorCode.LOCATION_OUTSIDE_HCM);
        }

        if (geoBoundaries.waterGeometry() != null && geoBoundaries.waterGeometry().covers(point)) {
            throw new AppException(ErrorCode.LOCATION_ON_WATER);
        }
    }
    private double resolveDistanceKm(double storeLat, double storeLon, double destLat, double destLon) {
        double haversine = GeoUtils.distanceKm(storeLat, storeLon, destLat, destLon);

        if (haversine > MAX_DISTANCE_FOR_ORS_CALL_KM) {
            return GeoUtils.estimateRoadDistanceKm(haversine);
        }

        try {
            return orsRoutingService.getRoadDistanceKm(storeLat, storeLon, destLat, destLon);
        } catch (Exception e) {
            return GeoUtils.estimateRoadDistanceKm(haversine);
        }
    }
}