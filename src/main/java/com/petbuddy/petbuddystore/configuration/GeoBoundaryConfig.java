package com.petbuddy.petbuddystore.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class GeoBoundaryConfig {

    @Value("classpath:geo/hcm_boundary.geojson")
    private Resource hcmBoundaryResource;

    @Value("classpath:geo/water.geojson")
    private Resource waterResource;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Bean
    public GeoBoundaries geoBoundaries() throws IOException {
        Geometry hcmBoundary;
        try (InputStream is = hcmBoundaryResource.getInputStream()) {
            hcmBoundary = parseFeatureCollection(is);
        }

        Geometry water;
        try (InputStream is = waterResource.getInputStream()) {
            water = parseFeatureCollection(is);
        }

        PreparedGeometry preparedHcm = PreparedGeometryFactory.prepare(hcmBoundary);
        PreparedGeometry preparedWater = water.isEmpty() ? null : PreparedGeometryFactory.prepare(water);

        return new GeoBoundaries(preparedHcm, preparedWater);
    }

    private Geometry parseFeatureCollection(InputStream is) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(is);
        JsonNode features = root.path("features");

        List<Geometry> geometries = new ArrayList<>();

        for (JsonNode feature : features) {
            JsonNode geometryNode = feature.path("geometry");
            String type = geometryNode.path("type").asText();
            JsonNode coordinates = geometryNode.path("coordinates");

            switch (type) {
                case "Polygon" -> geometries.add(parsePolygon(coordinates));
                case "MultiPolygon" -> {
                    for (JsonNode polygonCoords : coordinates) {
                        geometries.add(parsePolygon(polygonCoords));
                    }
                }
                default -> throw new IllegalArgumentException(
                        "Không hỗ trợ geometry type: " + type);
            }
        }

        if (geometries.isEmpty()) {
            throw new IllegalStateException("Không tìm thấy geometry hợp lệ trong file GeoJSON");
        }

        // FIX: fix từng geometry (self-intersection, ring lỗi...) trước khi union
        // tránh TopologyException: side location conflict
        List<Geometry> fixedGeometries = new ArrayList<>();
        for (Geometry g : geometries) {
            fixedGeometries.add(g.isValid() ? g : GeometryFixer.fix(g));
        }

        return UnaryUnionOp.union(fixedGeometries);
    }

    private Polygon parsePolygon(JsonNode polygonCoords) {
        LinearRing exteriorRing = toLinearRing(polygonCoords.get(0));

        List<LinearRing> holes = new ArrayList<>();
        for (int i = 1; i < polygonCoords.size(); i++) {
            holes.add(toLinearRing(polygonCoords.get(i)));
        }

        return GEOMETRY_FACTORY.createPolygon(exteriorRing, holes.toArray(new LinearRing[0]));
    }

    private LinearRing toLinearRing(JsonNode ring) {
        Coordinate[] coords = new Coordinate[ring.size()];
        for (int i = 0; i < ring.size(); i++) {
            JsonNode p = ring.get(i);
            coords[i] = new Coordinate(p.get(0).asDouble(), p.get(1).asDouble());
        }
        return GEOMETRY_FACTORY.createLinearRing(coords);
    }

    public record GeoBoundaries(PreparedGeometry hcmBoundaryGeometry, PreparedGeometry waterGeometry) {
    }
}