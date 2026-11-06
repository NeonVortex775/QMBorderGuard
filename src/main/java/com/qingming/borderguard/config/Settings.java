package com.qingming.borderguard.config;

import com.qingming.borderguard.BorderGuardPlugin;
import com.qingming.borderguard.region.Region;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** config.yml in memory, rebuilt on /border reload. */
public class Settings {

    private final BorderGuardPlugin plugin;

    public boolean enforce = true;
    public int minNodes = 3;
    public Region.Rescue rescue = Region.Rescue.PUSH;
    public double rescueSpeed = 0.9D;
    public int messageCooldownMs = 3000;
    public boolean touchMessage = true;
    public boolean pushMessage = true;
    public Set<GameMode> ignoreGamemodes = EnumSet.of(GameMode.SPECTATOR);
    public boolean ignoreOps = false;
    public String bypassPermission = "borderguard.bypass";
    public boolean teleportGuard = true;
    public boolean rescueOnJoin = true;

    public boolean wandEnabled = true;
    public Material wandMaterial = Material.BLAZE_ROD;
    public String wandName = "&b&l边界调试杖";
    public List<String> wandLore = new ArrayList<String>();
    public boolean showLineWhenHolding = true;

    public int renderInterval = 4;
    public double renderStep = 0.7D;
    public double renderRadius = 40.0D;
    public double renderYOffset = 1.0D;
    public int renderMaxParticles = 900;
    public String lineParticle = "SPELL_WITCH";
    public String nodeParticle = "FLAME";
    public double nodePillar = 3.0D;

    public boolean effectsEnabled = true;
    public int effectViewDistance = 24;
    public int effectCooldownMs = 700;
    public String effectSound = "BLOCK_FIRE_EXTINGUISH";
    public float soundVolume = 0.45F;
    public float soundPitch = 0.7F;

    public Settings(BorderGuardPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration c = plugin.getConfig();

        enforce = c.getBoolean("settings.enforce", true);
        minNodes = Math.max(3, c.getInt("settings.min-nodes", 3));
        rescue = parseEnum(Region.Rescue.class, c.getString("settings.rescue"), Region.Rescue.PUSH);
        rescueSpeed = Math.max(0.2D, c.getDouble("settings.rescue-speed", 0.9D));
        messageCooldownMs = Math.max(0, c.getInt("settings.message-cooldown-ms", 3000));
        touchMessage = c.getBoolean("settings.touch-message", true);
        pushMessage = c.getBoolean("settings.push-message", true);
        ignoreOps = c.getBoolean("settings.ignore-ops", false);
        bypassPermission = c.getString("settings.bypass-permission", "borderguard.bypass");
        teleportGuard = c.getBoolean("settings.teleport-guard", true);
        rescueOnJoin = c.getBoolean("settings.rescue-on-join", true);

        ignoreGamemodes = EnumSet.noneOf(GameMode.class);
        for (String s : c.getStringList("settings.ignore-gamemodes")) {
            GameMode gm = parseEnum(GameMode.class, s, null);
            if (gm != null) {
                ignoreGamemodes.add(gm);
            }
        }

        wandEnabled = c.getBoolean("wand.enabled", true);
        Material mat = Material.matchMaterial(c.getString("wand.material", "BLAZE_ROD"));
        wandMaterial = mat == null ? Material.BLAZE_ROD : mat;
        wandName = c.getString("wand.name", "&b&l边界调试杖");
        wandLore = c.getStringList("wand.lore");
        showLineWhenHolding = c.getBoolean("wand.show-line-when-holding", true);

        renderInterval = Math.max(1, c.getInt("render.interval-ticks", 4));
        renderStep = Math.max(0.15D, c.getDouble("render.step", 0.7D));
        renderRadius = Math.max(4.0D, c.getDouble("render.radius", 40.0D));
        renderYOffset = c.getDouble("render.y-offset", 1.0D);
        renderMaxParticles = Math.max(20, c.getInt("render.max-particles-per-tick", 900));
        lineParticle = c.getString("render.line-particle", "SPELL_WITCH");
        nodeParticle = c.getString("render.node-particle", "FLAME");
        nodePillar = Math.max(0.0D, c.getDouble("render.node-pillar", 3.0D));

        effectsEnabled = c.getBoolean("effects.enabled", true);
        effectViewDistance = Math.max(1, c.getInt("effects.view-distance", 24));
        effectCooldownMs = Math.max(0, c.getInt("effects.cooldown-ms", 700));
        effectSound = c.getString("effects.sound", "BLOCK_FIRE_EXTINGUISH");
        soundVolume = (float) c.getDouble("effects.sound-volume", 0.45D);
        soundPitch = (float) c.getDouble("effects.sound-pitch", 0.7D);
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String name, E def) {
        if (name == null || name.trim().isEmpty()) {
            return def;
        }
        try {
            return Enum.valueOf(type, name.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return def;
        }
    }
}
