package com.qingming.borderguard.config;

import com.qingming.borderguard.BorderGuardPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/** messages.yml, read once and formatted per line. */
public class Lang {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final BorderGuardPlugin plugin;
    private YamlConfiguration yml = new YamlConfiguration();
    private String prefix = "";

    public Lang(BorderGuardPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        yml = YamlConfiguration.loadConfiguration(file);

        try {
            InputStream in = plugin.getResource("messages.yml");
            if (in != null) {
                Reader reader = new InputStreamReader(in, UTF8);
                YamlConfiguration def = YamlConfiguration.loadConfiguration(reader);
                yml.setDefaults(def);
                reader.close();
            }
        } catch (Exception ignored) {

        }

        prefix = color(yml.getString("prefix", ""));
    }

    public static String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }

    public String raw(String key) {
        String s = yml.getString(key);
        return s == null ? "" : s;
    }

    public String msg(String key, Object... args) {
        String s = raw(key);
        if (s.isEmpty()) {
            return color("&c缺失消息: " + key);
        }
        s = fill(s, args);
        return color(s);
    }

    public String prefixed(String key, Object... args) {
        return prefix + msg(key, args);
    }

    public void send(CommandSender to, String key, Object... args) {
        String s = msg(key, args);
        if (s.isEmpty()) {
            return;
        }
        to.sendMessage(prefix + s);
    }

    public void sendRaw(CommandSender to, String key, Object... args) {
        to.sendMessage(msg(key, args));
    }

    public List<String> list(String key, Object... args) {
        List<String> out = new ArrayList<String>();
        for (String line : yml.getStringList(key)) {
            out.add(color(fill(line, args)));
        }
        return out;
    }

    public String getPrefix() {
        return prefix;
    }

    private static String fill(String s, Object... args) {
        if (args == null || args.length == 0) {
            return s;
        }
        for (int i = 0; i < args.length; i++) {
            String v = args[i] == null ? "" : String.valueOf(args[i]);
            s = s.replace("{" + i + "}", v);
        }
        return s;
    }
}
