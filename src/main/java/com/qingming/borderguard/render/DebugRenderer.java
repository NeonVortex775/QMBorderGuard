package com.qingming.borderguard.render;

import com.qingming.borderguard.BorderGuardPlugin;
import com.qingming.borderguard.region.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Border line particles for staff who turned the preview on. */
public class DebugRenderer {

    private final BorderGuardPlugin plugin;

    private final Set<UUID> toggled = new HashSet<UUID>();

    private final Set<UUID> bypass = new HashSet<UUID>();

    private final java.util.Map<UUID, Long> lastTouchMsg = new java.util.HashMap<UUID, Long>();
    private final java.util.Map<UUID, Long> lastPushMsg = new java.util.HashMap<UUID, Long>();
    private final java.util.Map<UUID, Long> lastEffect = new java.util.HashMap<UUID, Long>();

    private BukkitTask task;

    public DebugRenderer(BorderGuardPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        int interval = plugin.settings().renderInterval;
        task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                tick();
            }
        }, 20L, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public boolean toggle(Player player) {
        UUID id = player.getUniqueId();
        if (toggled.remove(id)) {
            return false;
        }
        toggled.add(id);
        return true;
    }

    public boolean isToggled(Player player) {
        return toggled.contains(player.getUniqueId());
    }

    public boolean toggleBypass(Player player) {
        UUID id = player.getUniqueId();
        if (bypass.remove(id)) {
            return false;
        }
        bypass.add(id);
        return true;
    }

    public boolean isBypassing(Player player) {
        return bypass.contains(player.getUniqueId());
    }

    public void forget(Player player) {
        UUID id = player.getUniqueId();
        toggled.remove(id);
        bypass.remove(id);
        lastTouchMsg.remove(id);
        lastPushMsg.remove(id);
        lastEffect.remove(id);
    }

    public boolean allowTouchMessage(Player p) {
        return allow(lastTouchMsg, p, plugin.settings().messageCooldownMs);
    }

    public boolean allowPushMessage(Player p) {
        return allow(lastPushMsg, p, Math.max(1500, plugin.settings().messageCooldownMs));
    }

    public boolean allowEffect(Player p) {
        return allow(lastEffect, p, plugin.settings().effectCooldownMs);
    }

    private boolean allow(java.util.Map<UUID, Long> map, Player p, long cooldown) {
        long now = System.currentTimeMillis();
        Long last = map.get(p.getUniqueId());
        if (last != null && now - last < cooldown) {
            return false;
        }
        map.put(p.getUniqueId(), now);
        return true;
    }

    private boolean shouldRender(Player p) {
        if (toggled.contains(p.getUniqueId())) {
            return true;
        }
        if (!plugin.settings().showLineWhenHolding) {
            return false;
        }
        if (!p.hasPermission("borderguard.admin")) {
            return false;
        }
        return isHoldingWand(p);
    }

    private boolean isHoldingWand(Player p) {
        ItemStack inHand = p.getInventory().getItemInMainHand();
        return inHand != null && inHand.getType() == plugin.settings().wandMaterial;
    }

    private void tick() {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!shouldRender(player)) {
                continue;
            }
            Region region = plugin.regions().get(player.getWorld());
            if (region == null || !region.hasNodes()) {
                continue;
            }
            draw(player, region);
        }
    }

    private void draw(Player viewer, Region region) {
        List<double[]> nodes = region.getNodes();
        int n = nodes.size();
        if (n == 0) {
            return;
        }

        Location eye = viewer.getLocation();
        double baseY = eye.getY() + plugin.settings().renderYOffset;
        double radius = plugin.settings().renderRadius;
        double radius2 = radius * radius;
        int budget = plugin.settings().renderMaxParticles;

        Particle lineParticle = plugin.effects().lineParticle();
        Particle nodeParticle = plugin.effects().nodeParticle();

        double pillar = plugin.settings().nodePillar;
        for (int i = 0; i < n && budget > 0; i++) {
            double[] node = nodes.get(i);
            double dx = node[0] - eye.getX();
            double dz = node[2] - eye.getZ();
            if (dx * dx + dz * dz > radius2) {
                continue;
            }
            for (double dy = 0.0D; dy <= pillar && budget > 0; dy += 1.0D) {
                plugin.effects().dot(viewer, nodeParticle,
                        new Location(eye.getWorld(), node[0], baseY + dy, node[2]));
                budget--;
            }
        }

        if (n < 2) {
            return;
        }
        int segments = n == 2 ? 1 : n;
        double step = plugin.settings().renderStep;
        for (int i = 0; i < segments && budget > 0; i++) {
            double[] a = nodes.get(i);
            double[] b = nodes.get((i + 1) % n);

            double ax = a[0], az = a[2], bx = b[0], bz = b[2];
            double len = Math.sqrt((bx - ax) * (bx - ax) + (bz - az) * (bz - az));
            int steps = (int) Math.ceil(len / step);
            if (steps < 1) {
                steps = 1;
            }

            if (!nearSegment(ax, az, bx, bz, eye.getX(), eye.getZ(), radius)) {
                continue;
            }
            for (int s = 0; s <= steps && budget > 0; s++) {
                double t = (double) s / (double) steps;
                double x = ax + (bx - ax) * t;
                double z = az + (bz - az) * t;
                double dx = x - eye.getX();
                double dz = z - eye.getZ();
                if (dx * dx + dz * dz > radius2) {
                    continue;
                }
                plugin.effects().dot(viewer, lineParticle, new Location(eye.getWorld(), x, baseY, z));
                budget--;
            }
        }
    }

    private static boolean nearSegment(double ax, double az, double bx, double bz,
                                       double px, double pz, double range) {
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
        double cx = ax + dx * t - px;
        double cz = az + dz * t - pz;
        return cx * cx + cz * cz <= range * range;
    }

    public void load() {
        toggled.clear();
        File file = new File(plugin.getDataFolder(), "debug.yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        for (String s : yml.getStringList("toggled")) {
            try {
                toggled.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {

            }
        }
    }

    public void save() {
        File file = new File(plugin.getDataFolder(), "debug.yml");
        YamlConfiguration yml = new YamlConfiguration();
        List<String> list = new ArrayList<String>();
        for (UUID id : toggled) {
            list.add(id.toString());
        }
        yml.set("toggled", list);
        try {
            yml.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("保存 debug.yml 失败: " + e.getMessage());
        }
    }
}
