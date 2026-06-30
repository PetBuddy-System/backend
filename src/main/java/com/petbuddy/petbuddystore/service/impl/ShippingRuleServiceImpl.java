package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.ShippingRuleRequest;
import com.petbuddy.petbuddystore.dto.response.ShippingFeeResponse;
import com.petbuddy.petbuddystore.dto.response.ShippingRuleResponse;
import com.petbuddy.petbuddystore.mapper.ShippingMapper;
import com.petbuddy.petbuddystore.model.ShippingRule;
import com.petbuddy.petbuddystore.repository.ShippingRuleRepository;
import com.petbuddy.petbuddystore.service.ShippingRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShippingRuleServiceImpl implements ShippingRuleService {

    private static final double FREE_SHIP_RADIUS = 5.0;

    private static final double STORE_LAT = 10.776889;
    private static final double STORE_LON = 106.700806;

    private final ShippingRuleRepository shippingRuleRepository;
    private final ShippingMapper  shippingMapper;


    @Override
    public ShippingFeeResponse calculateFee(Double latitude, Double longitude) {
        if(latitude == null || ((latitude < -90 || latitude > 90)) ||
                longitude == null || ((longitude < -180 || longitude > 180))) {
            throw new AppException(ErrorCode.INVALID_COORDINATES);
        }

        if(!isInsideHCM(latitude, longitude)){
            throw new AppException(ErrorCode.LOCATION_OUTSIDE_HCM);
        }

        double distance = calculateDistance(STORE_LAT, STORE_LON, latitude, longitude);
        if(distance <= FREE_SHIP_RADIUS){
            return shippingMapper.toResponse(distance, BigDecimal.ZERO, true);
        }

        ShippingRule config = shippingRuleRepository.findRuleByDistance(distance)
                        .orElseThrow(() -> new AppException(ErrorCode.SHIPPING_CONFIG_NOT_FOUND));
        return shippingMapper.toResponse(distance, config.getFee(), false);
    }

    @Override
    public ShippingRuleResponse createRule(ShippingRuleRequest request) {
        boolean exists = shippingRuleRepository.existsOverlap(request.getMinDistance(), request.getMaxDistance());
        if(exists) {
            throw new AppException(ErrorCode.SHIPPING_RULE_OVERLAP);
        }

        if(request.getMinDistance() > request.getMaxDistance()) {
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

        return shippingMapper.toShippingResponse(shippingRuleRepository.save(shippingRuleRepository.save(rule)));
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

    private boolean isInsideHCM(double lat, double lon){

        double MIN_LAT = 10.35;
        double MIN_LON = 106.30;
        double MAX_LAT = 11.20;
        double MAX_LON = 107.10;
        return lat >= MIN_LAT && lat <= MAX_LAT && lon >= MIN_LON && lon <= MAX_LON;
    }
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {

            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lon2 - lon1);

            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                            + Math.cos(Math.toRadians(lat1))
                            * Math.cos(Math.toRadians(lat2))
                            * Math.sin(dLon / 2)
                            * Math.sin(dLon / 2);

            double c = 2 * Math.atan2(
                            Math.sqrt(a),
                            Math.sqrt(1 - a));

            double EARTH_RADIUS = 6371;
            return EARTH_RADIUS * c;
        }
}
