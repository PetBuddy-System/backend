package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.configuration.OrsConfig;
import com.petbuddy.petbuddystore.dto.response.OrsDirectionsResponse;
import com.petbuddy.petbuddystore.service.OrsRoutingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrsRoutingServiceIml implements OrsRoutingService {

    @Qualifier("orsRestTemplate")
    RestTemplate orsRestTemplate;

    OrsConfig orsConfig;

    @Override
    public double getRoadDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        String url = UriComponentsBuilder.fromUriString(orsConfig.getBaseUrl())
                .queryParam("api_key", orsConfig.getApiKey())
                .queryParam("start", lon1 + "," + lat1)
                .queryParam("end", lon2 + "," + lat2)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<OrsDirectionsResponse> response = orsRestTemplate.exchange(
                url, HttpMethod.GET, entity, OrsDirectionsResponse.class);

        OrsDirectionsResponse body = response.getBody();
        if (body == null || body.getFeatures() == null || body.getFeatures().isEmpty()) {
            throw new IllegalStateException("ORS trả về response rỗng hoặc không hợp lệ");
        }

        double distanceMeters = body.getFeatures()
                .getFirst()
                .getProperties()
                .getSummary()
                .getDistance();

        return distanceMeters / 1000.0;
    }
}