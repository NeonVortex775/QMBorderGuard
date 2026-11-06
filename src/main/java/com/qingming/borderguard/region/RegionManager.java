package com.qingming.borderguard.region;

import com.qingming.borderguard.BorderGuardPlugin;
import org.bukkit.World;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** All regions, one file per world. */
public class RegionManager {

    private final BorderGuardPlugin plugin;
    private final File dir;
    private final Map<String, Region> regions = new LinkedHashMap<String, Region>();

    public RegionManager(BorderGuardPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "regions");
    }

    public void loadAll() {
        regions.clear();
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("无法创建区域目录: " + dir.getAbsolutePath());
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            String name = f.getName();
            if (!f.isFile() || !name.toLowerCase().endsWith(".yml")) {
                continue;
            }
            String world = name.substring(0, name.length() - 4);
            Region region = new Region(world);
            try {
                region.load(f);
            } catch (Exception e) {
                plugin.getLogger().warning("加载边界文件失败 " + name + ": " + e.getMessage());
                continue;
            }
            regions.put(world, region);
        }
        plugin.getLogger().info("已载入 " + regions.size() + " 个世界的边界数据");
    }

    public void saveAll() {
        for (Region region : regions.values()) {
            save(region);
        }
    }

    public void save(Region region) {
        try {
            region.save(fileFor(region.getWorldName()));
        } catch (Exception e) {
            plugin.getLogger().warning("保存边界数据失败 (" + region.getWorldName() + "): " + e.getMessage());
        }
    }

    public File fileFor(String world) {
        return new File(dir, world + ".yml");
    }

    public Region get(World world) {
        if (world == null) {
            return null;
        }
        return regions.get(world.getName());
    }

    public Region get(String world) {
        return world == null ? null : regions.get(world);
    }

    public Region getOrCreate(World world) {
        Region region = regions.get(world.getName());
        if (region == null) {
            region = new Region(world.getName());
            region.setMinNodes(plugin.settings().minNodes);
            regions.put(world.getName(), region);
        }
        return region;
    }

    public Collection<Region> all() {
        return regions.values();
    }

    public int activeCount() {
        int n = 0;
        for (Region r : regions.values()) {
            if (r.isActive()) {
                n++;
            }
        }
        return n;
    }

    public List<String> worldNames() {
        return new ArrayList<String>(regions.keySet());
    }

    public void remove(String world) {
        regions.remove(world);
        File f = fileFor(world);
        if (f.exists()) {
            f.delete();
        }
    }
}
