package com.petbuddy.petbuddystore.common.util;

import java.util.*;

public class CapacitatedKMeans {

    public record WeightedPoint(Long id, double lat, double lng, double weight) {}
    public record Cluster(int shipperIndex, double centerLat, double centerLng, List<Long> assignedPointIds) {}

    public static List<Cluster> cluster(
            List<WeightedPoint> points,
            List<Double> initialCenterLats,
            List<Double> initialCenterLngs,
            List<Double> capacities,
            int maxIterations) {

        int k = initialCenterLats.size();
        double[] centerLat = toArray(initialCenterLats);
        double[] centerLng = toArray(initialCenterLngs);
        List<List<WeightedPoint>> assignment = null;

        for (int iter = 0; iter < maxIterations; iter++) {
            assignment = assignCapacityAware(points, centerLat, centerLng, capacities);

            double[] newLat = new double[k];
            double[] newLng = new double[k];
            boolean moved = false;

            for (int c = 0; c < k; c++) {
                List<WeightedPoint> members = assignment.get(c);
                if (members.isEmpty()) {
                    newLat[c] = centerLat[c];
                    newLng[c] = centerLng[c];
                    continue;
                }
                double sumW = members.stream().mapToDouble(WeightedPoint::weight).sum();
                newLat[c] = members.stream().mapToDouble(p -> p.lat() * p.weight()).sum() / sumW;
                newLng[c] = members.stream().mapToDouble(p -> p.lng() * p.weight()).sum() / sumW;
                if (Math.abs(newLat[c] - centerLat[c]) > 1e-6 || Math.abs(newLng[c] - centerLng[c]) > 1e-6) {
                    moved = true;
                }
            }
            centerLat = newLat;
            centerLng = newLng;
            if (!moved) break; // hội tụ
        }

        List<Cluster> result = new ArrayList<>();
        for (int c = 0; c < k; c++) {
            List<Long> ids = assignment.get(c).stream().map(WeightedPoint::id).toList();
            result.add(new Cluster(c, centerLat[c], centerLng[c], ids));
        }
        return result;
    }

    /** Bước gán điểm: greedy nearest-first, tôn trọng capacity còn lại của từng cluster (kiểu bin-packing). */
    private static List<List<WeightedPoint>> assignCapacityAware(
            List<WeightedPoint> points, double[] centerLat, double[] centerLng, List<Double> capacities) {

        int k = centerLat.length;
        List<List<WeightedPoint>> assignment = new ArrayList<>();
        double[] remaining = new double[k];
        for (int i = 0; i < k; i++) {
            assignment.add(new ArrayList<>());
            remaining[i] = capacities.get(i);
        }

        record Candidate(WeightedPoint point, int cluster, double distance) {}
        List<Candidate> candidates = new ArrayList<>();
        for (WeightedPoint p : points) {
            for (int c = 0; c < k; c++) {
                double d = GeoUtils.distanceKm(p.lat(), p.lng(), centerLat[c], centerLng[c]);
                candidates.add(new Candidate(p, c, d));
            }
        }
        candidates.sort(Comparator.comparingDouble(Candidate::distance));

        Set<Long> assigned = new HashSet<>();
        for (Candidate cand : candidates) {
            if (assigned.contains(cand.point().id())) continue;
            if (remaining[cand.cluster()] >= cand.point().weight()) {
                assignment.get(cand.cluster()).add(cand.point());
                remaining[cand.cluster()] -= cand.point().weight();
                assigned.add(cand.point().id());
            }
        }

        // Điểm còn dư (mọi cluster đều full) -> ép vào cluster gần nhất để không mất đơn
        for (WeightedPoint p : points) {
            if (assigned.contains(p.id())) continue;
            int best = 0;
            double bestDist = Double.MAX_VALUE;
            for (int c = 0; c < k; c++) {
                double d = GeoUtils.distanceKm(p.lat(), p.lng(), centerLat[c], centerLng[c]);
                if (d < bestDist) { bestDist = d; best = c; }
            }
            assignment.get(best).add(p);
        }
        return assignment;
    }

    private static double[] toArray(List<Double> list) {
        double[] arr = new double[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }
}