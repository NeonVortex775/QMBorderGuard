package com.qingming.borderguard.command;

import com.qingming.borderguard.BorderGuardPlugin;
import com.qingming.borderguard.config.Lang;
import com.qingming.borderguard.region.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** /border - wand, nodes, region settings, reload. */
public class BorderCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = Arrays.asList(
            "help", "wand", "add", "undo", "remove", "insert", "list", "clear", "tp",
            "on", "off", "min", "y", "spawn", "toggle", "bypass", "info", "reload");

    private final BorderGuardPlugin plugin;

    public BorderCommand(BorderGuardPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("borderguard.admin")) {
            plugin.lang().send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        Lang lang = plugin.lang();

        if ("help".equals(sub)) {
            sendHelp(sender);
            return true;
        }
        if ("reload".equals(sub)) {
            plugin.reloadAll();
            lang.send(sender, "reload-done", plugin.regions().all().size());
            return true;
        }
        if ("wand".equals(sub)) {
            if (!(sender instanceof Player)) {
                lang.send(sender, "player-only");
                return true;
            }
            giveWand((Player) sender);
            return true;
        }

        if ("info".equals(sub) || "list".equals(sub)) {
            World target = resolveWorld(sender, args.length > 1 ? args[1] : null);
            if (target == null) {
                if (args.length > 1) {
                    lang.send(sender, "world-not-found", args[1]);
                } else {
                    lang.send(sender, "need-world");
                }
                return true;
            }
            Region r = plugin.regions().get(target);
            if (r == null) {
                lang.send(sender, "no-region", target.getName());
                return true;
            }
            if ("info".equals(sub)) {
                sendInfo(sender, r);
            } else {
                sendList(sender, r);
            }
            return true;
        }

        if (!(sender instanceof Player)) {
            lang.send(sender, "player-only");
            return true;
        }
        Player player = (Player) sender;
        World world = player.getWorld();
        Region region = plugin.regions().getOrCreate(world);

        if ("add".equals(sub)) {
            addNode(player, player.getLocation());
            return true;
        }
        if ("undo".equals(sub)) {
            undo(player);
            return true;
        }
        if ("remove".equals(sub)) {
            int index = parseInt(args, 1, 0);
            if (index <= 0 || index > region.nodeCount()) {
                lang.send(sender, "node-index-invalid", region.nodeCount());
                return true;
            }
            region.removeNode(index - 1);
            plugin.regions().save(region);
            lang.send(sender, "node-removed", index, region.nodeCount());
            return true;
        }
        if ("insert".equals(sub)) {
            int index = parseInt(args, 1, 0);
            if (index <= 0 || index > region.nodeCount()) {
                lang.send(sender, "node-index-invalid", region.nodeCount());
                return true;
            }
            Location loc = player.getLocation();
            region.insertNode(index - 1, loc.getBlockX() + 0.5D, loc.getBlockY(), loc.getBlockZ() + 0.5D);
            plugin.regions().save(region);
            lang.send(sender, "node-added", index, fmt(loc.getBlockX() + 0.5D),
                    loc.getBlockY(), fmt(loc.getBlockZ() + 0.5D), region.nodeCount(), region.requiredNodes());
            return true;
        }
        if ("clear".equals(sub)) {
            if (region.nodeCount() == 0) {
                lang.send(sender, "already-clear");
                return true;
            }
            int n = region.nodeCount();
            region.clearNodes();
            plugin.regions().save(region);
            lang.send(sender, "cleared", n);
            return true;
        }
        if ("tp".equals(sub)) {
            int index = parseInt(args, 1, 0);
            double[] node = region.getNode(index - 1);
            if (node == null) {
                lang.send(sender, "node-index-invalid", region.nodeCount());
                return true;
            }
            player.teleport(new Location(world, node[0], node[1] + 0.2D, node[2],
                    player.getLocation().getYaw(), player.getLocation().getPitch()));
            lang.send(sender, "tp-node", index);
            return true;
        }
        if ("on".equals(sub) || "off".equals(sub)) {
            region.setEnabled("on".equals(sub));
            plugin.regions().save(region);
            lang.send(sender, "on".equals(sub) ? "region-on" : "region-off");
            return true;
        }
        if ("min".equals(sub)) {
            int n = parseInt(args, 1, -1);
            if (n < 3) {
                lang.send(sender, "min-invalid");
                return true;
            }
            region.setMinNodes(n);
            plugin.regions().save(region);
            lang.send(sender, "min-set", n);
            return true;
        }
        if ("y".equals(sub)) {
            if (args.length >= 2 && "full".equalsIgnoreCase(args[1])) {
                region.setYFull();
                plugin.regions().save(region);
                lang.send(sender, "y-full");
                return true;
            }
            if (args.length < 3) {
                lang.send(sender, "usage-y");
                return true;
            }
            double minY, maxY;
            try {
                minY = Double.parseDouble(args[1]);
                maxY = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                lang.send(sender, "y-invalid");
                return true;
            }
            if (maxY <= minY) {
                lang.send(sender, "y-invalid");
                return true;
            }
            region.setYRange(minY, maxY);
            plugin.regions().save(region);
            lang.send(sender, "y-set", fmt(minY), fmt(maxY));
            return true;
        }
        if ("spawn".equals(sub)) {
            Location loc = player.getLocation();
            region.setSpawn(loc);
            plugin.regions().save(region);
            lang.send(sender, "spawn-set", fmt(loc.getX()), fmt(loc.getY()), fmt(loc.getZ()));
            return true;
        }
        if ("toggle".equals(sub)) {
            boolean on = plugin.renderer().toggle(player);
            lang.send(sender, on ? "debug-on" : "debug-off");
            return true;
        }
        if ("bypass".equals(sub)) {
            boolean on = plugin.renderer().toggleBypass(player);
            lang.send(sender, on ? "bypass-on" : "bypass-off");
            return true;
        }
        lang.send(sender, "unknown-command", sub);
        return true;
    }

    public void addNode(Player player, Location loc) {
        World world = loc.getWorld() == null ? player.getWorld() : loc.getWorld();
        Region region = plugin.regions().getOrCreate(world);
        double x = loc.getBlockX() + 0.5D;
        double y = loc.getBlockY();
        double z = loc.getBlockZ() + 0.5D;

        if (!region.addNode(x, y, z)) {
            plugin.lang().send(player, "node-duplicate");
            return;
        }
        plugin.regions().save(region);

        int count = region.nodeCount();
        int need = region.requiredNodes();
        if (region.isActive()) {
            plugin.lang().sendRaw(player, "node-added-active", count, fmt(x), fmt(y), fmt(z), count, need);
        } else {
            plugin.lang().sendRaw(player, "node-added", count, fmt(x), fmt(y), fmt(z), count, need);
        }
        plugin.effects().touch(new Location(world, x, y + 1.0D, z));
    }

    public void undo(Player player) {
        Region region = plugin.regions().getOrCreate(player.getWorld());
        if (region.removeLast() == null) {
            plugin.lang().send(player, "node-none-yet");
            return;
        }
        plugin.regions().save(region);
        plugin.lang().send(player, "node-undone", region.nodeCount());
    }

    private void giveWand(Player player) {
        if (!plugin.settings().wandEnabled) {
            plugin.lang().send(player, "wand-disabled");
            return;
        }
        ItemStack item = new ItemStack(plugin.settings().wandMaterial, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Lang.color(plugin.settings().wandName));
            List<String> lore = new ArrayList<String>();
            for (String line : plugin.settings().wandLore) {
                lore.add(Lang.color(line));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        java.util.Map<Integer, ItemStack> left = player.getInventory().addItem(item);
        if (!left.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
        plugin.lang().send(player, "wand-given");
    }

    private void sendHelp(CommandSender sender) {
        for (String line : plugin.lang().list("help", plugin.getDescription().getVersion())) {
            sender.sendMessage(line);
        }
    }

    private void sendList(CommandSender sender, Region region) {
        Lang lang = plugin.lang();
        sender.sendMessage(lang.msg("node-list-header", region.getWorldName(),
                region.nodeCount(), region.requiredNodes()));
        if (region.nodeCount() == 0) {
            sender.sendMessage(lang.msg("node-none-yet"));
        } else {
            int i = 1;
            for (double[] node : region.getNodes()) {
                sender.sendMessage(lang.msg("node-list-item", i++, fmt(node[0]), fmt(node[1]), fmt(node[2])));
            }
        }
        sender.sendMessage(lang.msg("node-list-footer"));
    }

    private void sendInfo(CommandSender sender, Region region) {
        Lang lang = plugin.lang();
        String state = region.isActive() ? lang.msg("region-active", region.nodeCount(), region.requiredNodes())
                : lang.msg("region-inactive", region.nodeCount(), region.requiredNodes());
        sender.sendMessage(lang.getPrefix() + lang.msg("info-line",
                region.getWorldName(), region.nodeCount(), state,
                region.getYMode() == Region.YMode.FULL ? "FULL" : "RANGE",
                plugin.settings().rescue.name()));

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (double[] node : region.getNodes()) {
            minX = Math.min(minX, node[0]);
            maxX = Math.max(maxX, node[0]);
            minZ = Math.min(minZ, node[2]);
            maxZ = Math.max(maxZ, node[2]);
        }
        if (region.nodeCount() > 0) {
            sender.sendMessage(lang.msg("info-bounds", fmt(minX), fmt(maxX), fmt(minZ), fmt(maxZ),
                    region.getYMode() == Region.YMode.FULL ? "不限"
                            : (fmt(region.getMinY()) + "~" + fmt(region.getMaxY()))));
        }
        Location spawn = region.getSpawn();
        if (spawn != null) {
            sender.sendMessage(lang.msg("info-spawn", fmt(spawn.getX()), fmt(spawn.getY()), fmt(spawn.getZ())));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (!sender.hasPermission("borderguard.admin")) {
            return out;
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String sub : SUBS) {
                if (sub.startsWith(prefix)) {
                    out.add(sub);
                }
            }
            return out;
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if ("remove".equals(sub) || "tp".equals(sub) || "insert".equals(sub)) {
                int count = 0;
                if (sender instanceof Player) {
                    Region r = plugin.regions().get(((Player) sender).getWorld());
                    count = r == null ? 0 : r.nodeCount();
                }
                for (int i = 1; i <= count; i++) {
                    out.add(String.valueOf(i));
                }
            } else if ("y".equals(sub)) {
                out.add("full");
                out.add("0");
            } else if ("info".equals(sub) || "list".equals(sub)) {
                for (World w : Bukkit.getWorlds()) {
                    out.add(w.getName());
                }
            }
        }
        return out;
    }

    private static int parseInt(String[] args, int index, int def) {
        if (args.length <= index) {
            return def;
        }
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static String fmt(double v) {
        long l = Math.round(v * 100.0D);
        if (l % 100L == 0L) {
            return Long.toString(l / 100L);
        }
        return Double.toString(l / 100.0D);
    }

    public World resolveWorld(CommandSender sender, String name) {
        if (name == null) {
            return sender instanceof Player ? ((Player) sender).getWorld() : null;
        }
        return Bukkit.getWorld(name);
    }
}
