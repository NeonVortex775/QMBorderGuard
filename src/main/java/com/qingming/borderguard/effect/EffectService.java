package com.qingming.borderguard.effect;

import com.qingming.borderguard.BorderGuardPlugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Particles and sound when a player hits the border. */
public class EffectService {

    public static class Spec {
        public final Particle particle;
        public final int count;
        public final double offset;
        public final double speed;

        Spec(Particle particle, int count, double offset, double speed) {
            this.particle = particle;
            this.count = count;
            this.offset = offset;
            this.speed = speed;
        }
    }

    private static final Map<String, String> ALIAS = new HashMap<String, String>();

    static {
        ALIAS.put("BLACK_SMOKE", "SMOKE_LARGE");
        ALIAS.put("BIG_SMOKE", "SMOKE_LARGE");
        ALIAS.put("SMOKE", "SMOKE_NORMAL");
        ALIAS.put("FIRE", "FLAME");
        ALIAS.put("BLACKSMOKE", "SMOKE_LARGE");
        ALIAS.put("WITCH", "SPELL_WITCH");
    }

    private final BorderGuardPlugin plugin;

    private final List<Spec> touch = new ArrayList<Spec>();
    private Particle lineParticle = Particle.SPELL_WITCH;
    private Particle nodeParticle = Particle.FLAME;
    private Sound sound;

    public EffectService(BorderGuardPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        touch.clear();
        lineParticle = resolve(plugin.settings().lineParticle, Particle.SPELL_WITCH);
        nodeParticle = resolve(plugin.settings().nodeParticle, Particle.FLAME);
        sound = resolveSound(plugin.settings().effectSound);

        List<?> list = plugin.getConfig().getList("effects.touch");
        if (list != null) {
            for (Object o : list) {
                Spec spec = parseSpec(o);
                if (spec != null) {
                    touch.add(spec);
                }
            }
        }
        if (touch.isEmpty()) {

            touch.add(new Spec(Particle.SMOKE_LARGE, 16, 0.3D, 0.02D));
            touch.add(new Spec(Particle.SMOKE_NORMAL, 10, 0.45D, 0.01D));
        }
    }

    @SuppressWarnings("unchecked")
    private Spec parseSpec(Object o) {
        if (o instanceof String) {

            String[] p = ((String) o).split(":");
            Particle particle = resolve(p.length > 0 ? p[0] : null, null);
            if (particle == null) {
                return null;
            }
            int count = p.length > 1 ? safeInt(p[1], 12) : 12;
            double offset = p.length > 2 ? safeDouble(p[2], 0.3D) : 0.3D;
            double speed = p.length > 3 ? safeDouble(p[3], 0.02D) : 0.02D;
            return new Spec(particle, count, offset, speed);
        }
        String name = null;
        int count = 12;
        double offset = 0.3D;
        double speed = 0.02D;
        if (o instanceof ConfigurationSection) {
            ConfigurationSection s = (ConfigurationSection) o;
            name = s.getString("particle");
            count = s.getInt("count", count);
            offset = s.getDouble("offset", offset);
            speed = s.getDouble("speed", speed);
        } else if (o instanceof Map) {
            Map<Object, Object> m = (Map<Object, Object>) o;
            Object n = m.get("particle");
            name = n == null ? null : String.valueOf(n);
            count = (int) safeDouble(m.get("count"), count);
            offset = safeDouble(m.get("offset"), offset);
            speed = safeDouble(m.get("speed"), speed);
        }
        Particle particle = resolve(name, null);
        if (particle == null) {
            return null;
        }
        return new Spec(particle, Math.max(1, count), offset, speed);
    }

    private static double safeDouble(Object o, double def) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        if (o instanceof String) {
            try {
                return Double.parseDouble((String) o);
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    private static int safeInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static Particle resolve(String name, Particle def) {
        if (name == null || name.trim().isEmpty()) {
            return def;
        }
        String key = name.trim().toUpperCase(Locale.ROOT);
        if (ALIAS.containsKey(key)) {
            key = ALIAS.get(key);
        }
        try {
            return Particle.valueOf(key);
        } catch (IllegalArgumentException e) {
            return def;
        }
    }

    private static Sound resolveSound(String name) {
        if (name == null || name.trim().isEmpty() || "NONE".equalsIgnoreCase(name.trim())) {
            return null;
        }
        try {
            return Sound.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public Particle lineParticle() {
        return lineParticle;
    }

    public Particle nodeParticle() {
        return nodeParticle;
    }

    public void touch(Location loc) {
        if (!plugin.settings().effectsEnabled || loc == null || loc.getWorld() == null) {
            return;
        }
        int view = plugin.settings().effectViewDistance;
        double maxDist = (double) view * (double) view;
        for (Player viewer : loc.getWorld().getPlayers()) {
            Location vl = viewer.getLocation();
            if (vl.getWorld() != loc.getWorld()) {
                continue;
            }
            double dx = vl.getX() - loc.getX();
            double dy = vl.getY() - loc.getY();
            double dz = vl.getZ() - loc.getZ();
            if (dx * dx + dy * dy + dz * dz > maxDist) {
                continue;
            }
            for (Spec spec : touch) {
                spawn(viewer, spec.particle, loc, spec.count, spec.offset, spec.speed);
            }
            if (sound != null) {
                viewer.playSound(loc, sound, plugin.settings().soundVolume, plugin.settings().soundPitch);
            }
        }
    }

    public void dot(Player viewer, Particle particle, Location loc) {
        spawn(viewer, particle, loc, 1, 0.0D, 0.0D);
    }

    private static void spawn(Player viewer, Particle particle, Location loc,
                              int count, double offset, double speed) {
        if (particle == null) {
            return;
        }
        try {
            viewer.spawnParticle(particle, loc, count, offset, offset, offset, speed);
        } catch (Throwable ignored) {

        }
    }

    static boolean isSection(Object o) {
        return o instanceof MemorySection;
    }
}
