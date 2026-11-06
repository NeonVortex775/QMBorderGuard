package com.qingming.borderguard;

import com.qingming.borderguard.command.BorderCommand;
import com.qingming.borderguard.config.Lang;
import com.qingming.borderguard.config.Settings;
import com.qingming.borderguard.effect.EffectService;
import com.qingming.borderguard.listener.BorderListener;
import com.qingming.borderguard.listener.WandListener;
import com.qingming.borderguard.region.RegionManager;
import com.qingming.borderguard.render.DebugRenderer;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/** Plugin entry point: settings, regions, effects, command, listeners. */
public class BorderGuardPlugin extends JavaPlugin {

    private Settings settings;
    private Lang lang;
    private RegionManager regions;
    private EffectService effects;
    private DebugRenderer renderer;
    private BorderCommand command;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!new java.io.File(getDataFolder(), "messages.yml").exists()) {
            saveResource("messages.yml", false);
        }

        settings = new Settings(this);
        lang = new Lang(this);
        effects = new EffectService(this);
        regions = new RegionManager(this);
        renderer = new DebugRenderer(this);
        command = new BorderCommand(this);

        settings.load();
        lang.reload();
        effects.reload();
        regions.loadAll();
        renderer.load();

        PluginCommand pc = getCommand("border");
        if (pc != null) {
            pc.setExecutor(command);
            pc.setTabCompleter(command);
        } else {
            getLogger().severe("plugin.yml 里没有找到 border 指令，指令将不可用！");
        }

        getServer().getPluginManager().registerEvents(new BorderListener(this), this);
        getServer().getPluginManager().registerEvents(new WandListener(this), this);
        renderer.start();

        getLogger().info("BorderGuard 已启用 · 当前 " + regions.all().size()
                + " 个世界有边界数据，其中 " + regions.activeCount() + " 个已生效");
    }

    @Override
    public void onDisable() {
        if (renderer != null) {
            renderer.save();
            renderer.stop();
        }
        if (regions != null) {
            regions.saveAll();
        }
        getLogger().info("BorderGuard 已关闭");
    }

    public void reloadAll() {
        reloadConfig();
        settings.load();
        lang.reload();
        effects.reload();
        regions.loadAll();
        renderer.start();
    }

    public Settings settings() {
        return settings;
    }

    public Lang lang() {
        return lang;
    }

    public RegionManager regions() {
        return regions;
    }

    public EffectService effects() {
        return effects;
    }

    public DebugRenderer renderer() {
        return renderer;
    }

    public BorderCommand command() {
        return command;
    }
}
