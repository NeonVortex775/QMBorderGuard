package com.qingming.borderguard.region;

import java.util.List;

/** Planar helpers. Everything is computed on XZ, height is checked separately. */
public final class Geometry {

    private Geometry() {
    }

    public static boolean insideXZ(double px, double pz, double[][] poly) {
        if (poly == null || poly.length < 3) {
            return true;
        }
        boolean in = false;
        for (int i = 0, j = poly.length - 1; i < poly.length; j = i++) {
            double xi = poly[i][0], zi = poly[i][1];
            double xj = poly[j][0], zj = poly[j][1];
            boolean straddle = (zi > pz) != (zj > pz);
            if (!straddle) {
                continue;
            }
            double crossX = (xj - xi) * (pz - zi) / (zj - zi) + xi;
            if (px < crossX) {
                in = !in;
            }
        }
        return in;
    }

    public static double[] closestOnSegment(double px, double pz,
                                            double ax, double az, double bx, double bz) {
        double dx = bx - ax;
        double dz = bz - az;
        double len2 = dx * dx + dz * dz;
        double t = 0.0D;
        if (len2 > 1.0E-9D) {
            t = ((px - ax) * dx + (pz - az) * dz) / len2;
            if (t < 0.0D) {
                t = 0.0D;
            } else if (t > 1.0D) {
                t = 1.0D;
            }
        }
        double cx = ax + dx * t;
        double cz = az + dz * t;
        double ddx = cx - px;
        double ddz = cz - pz;
        return new double[]{cx, cz, ddx * ddx + ddz * ddz};
    }

    public static double[] centroid(List<double[]> poly) {
        double sx = 0.0D, sz = 0.0D;
        int n = poly.size();
        if (n == 0) {
            return new double[]{0.0D, 0.0D};
        }
        for (double[] p : poly) {
            sx += p[0];
            sz += p[2];
        }
        return new double[]{sx / n, sz / n};
    }
}
