package com.qingming.borderguard.listener;

import com.qingming.borderguard.BorderGuardPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/** The debug wand: right click adds a node, left click removes the last one. */
public class WandListener implements Listener {

    private final BorderGuardPlugin plugin;

    public WandListener(BorderGuardPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.settings().wandEnabled) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("borderguard.admin")) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        if (item.getType() != plugin.settings().wandMaterial) {
            return;
        }

        ItemStack main = player.getInventory().getItemInMainHand();
        if (main == null || main.getType() != plugin.settings().wandMaterial) {
            return;
        }

        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            if (player.isSneaking()) {
                boolean on = plugin.renderer().toggle(player);
                plugin.lang().send(player, on ? "debug-on" : "debug-off");
                return;
            }
            plugin.command().addNode(player, player.getLocation());
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            plugin.command().undo(player);
        }
    }
}
