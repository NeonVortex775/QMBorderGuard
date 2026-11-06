package com.qingming.borderguard.region;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** One world's polygon: nodes, optional y range, rescue mode. */
public class Region {

    public enum YMode {

        FULL,

        RANGE
    }

    public enum Rescue {
        PUSH, SPAWN, NONE
    }

    private final String worldName;
    private final List<double[]> nodes = new ArrayList<double[]>();

    private boolean enabled = true;
    private int minNodes = 3;
    private YMode yMode = YMode.FULL;
    private double minY = 0.0D;
    private double maxY = 256.0D;
    private double[] spawn;

    private double[][] polyCache;

    public Region(String worldName) {
        this.worldName = worldName;
    }

    public String getWorldName() {
        return worldName;
    }

    public World world() {
        return Bukkit.getWorld(worldName);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMinNodes() {
        return minNodes;
    }

    public void setMinNodes(int minNodes) {
        this.minNodes = Math.max(3, minNodes);
    }

    public int requiredNodes() {
        return Math.max(3, minNodes);
    }

    public boolean isActive() {
        return enabled && nodes.size() >= requiredNodes();
    }

    public int nodeCount() {
        return nodes.size();
    }

    public boolean hasNodes() {
        return !nodes.isEmpty();
    }

    public List<double[]> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public double[] getNode(int index) {
        if (index < 0 || index >= nodes.size()) {
            return null;
        }
        return nodes.get(index);
    }

    public boolean addNode(double x, double y, double z) {
        if (indexOfNear(x, z) >= 0) {
            return false;
        }
        nodes.add(new double[]{x, y, z});
        invalidate();
        return true;
    }

    public void insertNode(int index, double x, double y, double z) {
        int i = Math.max(0, Math.min(index, nodes.size()));
        nodes.add(i, new double[]{x, y, z});
        invalidate();
    }

    public int indexOfNear(double x, double z) {
        for (int i = 0; i < nodes.size(); i++) {
            double[] n = nodes.get(i);
            double dx = n[0] - x;
            double dz = n[2] - z;
            if (dx * dx + dz * dz <= 0.36D) {
                return i;
            }
        }
        return -1;
    }

    public boolean removeNode(int index) {
        if (index < 0 || index >= nodes.size()) {
            return false;
        }
        nodes.remove(index);
        invalidate();
        return true;
    }

    public double[] removeLast() {
        if (nodes.isEmpty()) {
            return null;
        }
        double[] last = nodes.remove(nodes.size() - 1);
        invalidate();
        return last;
    }

    public void clearNodes() {
        nodes.clear();
        invalidate();
    }

    private void invalidate() {
        polyCache = null;
    }

    private double[][] poly() {
        if (polyCache == null && nodes.size() >= 3) {
            double[][] arr = new double[nodes.size()][2];
            for (int i = 0; i < nodes.size(); i++) {
                arr[i][0] = nodes.get(i)[0];
                arr[i][1] = nodes.get(i)[2];
            }
            polyCache = arr;
        }
        return polyCache;
    }

    public boolean containsXZ(double x, double z) {
        if (!isActive()) {
            return true;
        }
        return Geometry.insideXZ(x, z, poly());
    }

    public boolean contains(Location loc) {
        if (loc == null) {
            return true;
        }
        if (!isActive()) {
            return true;
        }
        if (yMode == YMode.RANGE) {
            double y = loc.getY();
            if (y < minY || y > maxY) {
                return false;
            }
        }
        return containsXZ(loc.getX(), loc.getZ());
    }

    public Location nearestInside(Location from) {
        World w = world();
        if (w == null || nodes.size() < 3 || !isActive()) {
            return null;
        }
        double py = from.getY();
        if (yMode == YMode.RANGE) {

            if (py < minY) {
                py = minY + 0.1D;
            } else if (py > maxY - 1.0D) {
                py = maxY - 1.0D;
            }
        }
        double px = from.getX();
        double pz = from.getZ();

        double bestDist = Double.MAX_VALUE;
        double bx = 0.0D, bz = 0.0D;
        int n = nodes.size();
        for (int i = 0; i < n; i++) {
            double[] a = nodes.get(i);
            double[] b = nodes.get((i + 1) % n);
            double[] c = Geometry.closestOnSegment(px, pz, a[0], a[2], b[0], b[2]);
            if (c[2] < bestDist) {
                bestDist = c[2];
                bx = c[0];
                bz = c[1];
            }
        }
        if (bestDist == Double.MAX_VALUE) {
            return null;
        }

        double[] c0 = Geometry.centroid(nodes);
        double dirX = c0[0] - bx;
        double dirZ = c0[1] - bz;
        double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (len < 1.0E-6D) {
            dirX = 0.0D;
            dirZ = 0.0D;
            len = 1.0D;
        }
        for (double step = 0.6D; step <= 4.0D; step += 0.6D) {
            double tx = bx + dirX / len * step;
            double tz = bz + dirZ / len * step;
            if (containsXZ(tx, tz)) {
                return new Location(w, tx, py, tz, from.getYaw(), from.getPitch());
            }
        }
        if (containsXZ(c0[0], c0[1])) {
            return new Location(w, c0[0], py, c0[1], from.getYaw(), from.getPitch());
        }
        double[] first = nodes.get(0);
        return new Location(w, first[0], py, first[2], from.getYaw(), from.getPitch());
    }

    public YMode getYMode() {
        return yMode;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxY() {
        return maxY;
    }

    public void setYRange(double minY, double maxY) {
        this.yMode = YMode.RANGE;
        this.minY = minY;
        this.maxY = maxY;
    }

    public void setYFull() {
        this.yMode = YMode.FULL;
    }

    public Location getSpawn() {
        World w = world();
        if (spawn == null || w == null) {
            return null;
        }
        return new Location(w, spawn[0], spawn[1], spawn[2]);
    }

    public void setSpawn(Location loc) {
        this.spawn = new double[]{loc.getX(), loc.getY(), loc.getZ()};
    }

    public boolean hasSpawn() {
        return spawn != null;
    }

    public void save(File file) {
        YamlConfiguration yml = new YamlConfiguration();
        try {
            yml.options().header(
                    "# BorderGuard 边界数据 · 世界 " + worldName + "\n"
                            + "# 可以直接改这个文件，然后 /border reload 生效\n"
                            + "#\n"
                            + "# enabled     是否启用该世界的边界限制\n"
                            + "# min-nodes   区域生效所需的最少节点数（至少 3）\n"
                            + "# y-mode      FULL = 不限高度 / RANGE = 只认 min-y ~ max-y\n"
                            + "# nodes       节点列表，格式 \"x,y,z\"，按顺序连成多边形\n"
                            + "# spawn       越界时的救援点（rescue: SPAWN 时使用）\n");
        } catch (Throwable ignored) {

        }

        yml.set("world", worldName);
        yml.set("enabled", enabled);
        yml.set("min-nodes", minNodes);
        yml.set("y-mode", yMode.name());
        yml.set("min-y", roundNum(minY));
        yml.set("max-y", roundNum(maxY));

        if (spawn != null) {
            yml.set("spawn.x", roundNum(spawn[0]));
            yml.set("spawn.y", roundNum(spawn[1]));
            yml.set("spawn.z", roundNum(spawn[2]));
        }

        List<String> list = new ArrayList<String>();
        for (double[] node : nodes) {
            list.add(round(node[0]) + "," + round(node[1]) + "," + round(node[2]));
        }
        yml.set("nodes", list);

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            yml.save(file);
        } catch (IOException e) {
            throw new IllegalStateException("无法保存边界文件 " + file.getName(), e);
        }
    }

    public void load(File file) {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        nodes.clear();
        spawn = null;
        invalidate();

        enabled = yml.getBoolean("enabled", true);
        minNodes = Math.max(3, yml.getInt("min-nodes", 3));

        String mode = yml.getString("y-mode", "FULL");
        yMode = "RANGE".equalsIgnoreCase(mode) ? YMode.RANGE : YMode.FULL;
        minY = yml.getDouble("min-y", 0.0D);
        maxY = yml.getDouble("max-y", 256.0D);
        if (maxY <= minY) {
            maxY = minY + 256.0D;
        }

        if (yml.isSet("spawn")) {
            spawn = new double[]{
                    yml.getDouble("spawn.x"),
                    yml.getDouble("spawn.y"),
                    yml.getDouble("spawn.z")
            };
        }

        List<?> raw = yml.getList("nodes");
        if (raw != null) {
            for (Object o : raw) {
                double[] node = parseNode(o);
                if (node != null) {
                    nodes.add(node);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static double[] parseNode(Object o) {
        if (o instanceof String) {
            String[] parts = ((String) o).trim().split("\\s*,\\s*");
            if (parts.length < 2) {
                return null;
            }
            try {
                double x = Double.parseDouble(parts[0]);
                double y = parts.length > 2 ? Double.parseDouble(parts[1]) : 64.0D;
                double z = Double.parseDouble(parts.length > 2 ? parts[2] : parts[1]);
                return new double[]{x, y, z};
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (o instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) o;
            Double x = asDouble(map.get("x"));
            Double z = asDouble(map.get("z"));
            Double y = asDouble(map.get("y"));
            if (x == null || z == null) {
                return null;
            }
            return new double[]{x, y == null ? 64.0D : y, z};
        }
        return null;
    }

    private static Double asDouble(Object o) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        if (o instanceof String) {
            try {
                return Double.valueOf((String) o);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String round(double v) {
        long l = Math.round(v * 100.0D);
        if (l % 100L == 0L) {
            return Long.toString(l / 100L);
        }
        return Double.toString(l / 100.0D);
    }

    private static double roundNum(double v) {
        return Math.round(v * 100.0D) / 100.0D;
    }
}
