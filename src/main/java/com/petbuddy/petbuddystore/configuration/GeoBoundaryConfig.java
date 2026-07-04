package com.petbuddy.petbuddystore.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class GeoBoundaryConfig {
    @Value("classpath:geo/hcm_boundary.geojson")
    private Resource hcmBoundaryResource;

    @Bean
    public Geometry hcmBoundaryGeometry() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        GeometryFactory geometryFactory = new GeometryFactory();

        try (InputStream is = hcmBoundaryResource.getInputStream()) {
            JsonNode root = mapper.readTree(is);
            JsonNode ring = root.path("features").get(0)
                    .path("geometry").path("coordinates").get(0);

            Coordinate[] coords = new Coordinate[ring.size()];
            for (int i = 0; i < ring.size(); i++) {
                JsonNode p = ring.get(i);
                coords[i] = new Coordinate(p.get(0).asDouble(), p.get(1).asDouble());
            }

            Geometry polygon = geometryFactory.createPolygon(coords);
            log.info("Đã load ranh giới TP.HCM: {} điểm", coords.length);
            return polygon;
        }
    }
}
