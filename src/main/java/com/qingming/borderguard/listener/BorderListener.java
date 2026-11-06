package com.qingming.borderguard.listener;

import com.qingming.borderguard.BorderGuardPlugin;
import com.qingming.borderguard.region.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/** Movement and teleport interception. */
public class BorderListener implements Listener {

    private final BorderGuardPlugin plugin;

    public BorderListener(BorderGuardPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.settings().enforce) {
            return;
        }
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Region region = plugin.regions().get(player.getWorld());
        if (region == null || !region.isActive() || isExempt(player)) {
            return;
        }

        if (!region.contains(from)) {
            handleOutside(player, event, region, to);
            return;
        }
        if (region.contains(to)) {
            return;
        }

        Location blocked = null;

        Location xOnly = from.clone();
        xOnly.setX(to.getX());
        xOnly.setY(to.getY());
        xOnly.setYaw(to.getYaw());
        xOnly.setPitch(to.getPitch());
        if (region.contains(xOnly)) {
            blocked = xOnly;
        } else {
            Location zOnly = from.clone();
            zOnly.setZ(to.getZ());
            zOnly.setY(to.getY());
            zOnly.setYaw(to.getYaw());
            zOnly.setPitch(to.getPitch());
            if (region.contains(zOnly)) {
                blocked = zOnly;
            } else {
                Location stay = from.clone();
                stay.setY(to.getY());
                stay.setYaw(to.getYaw());
                stay.setPitch(to.getPitch());
                blocked = stay;
            }
        }
        event.setTo(blocked);
        onTouch(player, blocked);
    }

    private void handleOutside(Player player, PlayerMoveEvent event, Region region, Location to) {
        Region.Rescue policy = plugin.settings().rescue;
        if (policy == Region.Rescue.NONE) {
            return;
        }

        if (policy == Region.Rescue.SPAWN) {
            Location spawn = region.getSpawn();
            if (spawn == null) {
                spawn = region.nearestInside(to);
            }
            if (spawn == null) {
                return;
            }

            Location target = spawn.clone();
            target.setYaw(to.getYaw());
            target.setPitch(to.getPitch());
            event.setTo(target);
            if (plugin.renderer().allowPushMessage(player)) {
                if (plugin.settings().pushMessage) {
                    plugin.lang().send(player, "pushed");
                }
                plugin.effects().touch(to.clone().add(0.0D, 1.0D, 0.0D));
            }
            return;
        }

        Location inside = region.nearestInside(to);
        if (inside == null) {
            return;
        }
        double speed = Math.max(0.2D, plugin.settings().rescueSpeed);
        Location stepped = stepToward(to, inside, speed);
        event.setTo(stepped);

        if (plugin.renderer().allowPushMessage(player)) {
            if (plugin.settings().pushMessage) {
                plugin.lang().send(player, "pushed");
            }
            plugin.effects().touch(to.clone().add(0.0D, 1.0D, 0.0D));
        }
    }

    private static Location stepToward(Location from, Location target, double maxStep) {
        double dx = target.getX() - from.getX();
        double dz = target.getZ() - from.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist <= maxStep || dist < 1.0E-6D) {
            return new Location(from.getWorld(), target.getX(), from.getY(), target.getZ(),
                    from.getYaw(), from.getPitch());
        }
        double k = maxStep / dist;
        return new Location(from.getWorld(),
                from.getX() + dx * k, from.getY(), from.getZ() + dz * k,
                from.getYaw(), from.getPitch());
    }

    private void onTouch(Player player, Location contact) {
        if (plugin.renderer().allowEffect(player)) {
            plugin.effects().touch(contact.clone().add(0.0D, 1.0D, 0.0D));
        }
        if (plugin.settings().touchMessage && plugin.renderer().allowTouchMessage(player)) {
            plugin.lang().send(player, "blocked");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (!plugin.settings().enforce || !plugin.settings().teleportGuard) {
            return;
        }
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause != PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                && cause != PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
            return;
        }
        Player player = event.getPlayer();
        Region region = plugin.regions().get(player.getWorld());
        if (region == null || !region.isActive() || isExempt(player)) {
            return;
        }
        Location to = event.getTo();
        if (to == null || region.contains(to)) {
            return;
        }
        event.setCancelled(true);
        if (plugin.renderer().allowEffect(player)) {
            plugin.effects().touch(event.getFrom().clone().add(0.0D, 1.0D, 0.0D));
        }
        if (plugin.settings().touchMessage && plugin.renderer().allowTouchMessage(player)) {
            plugin.lang().send(player, "blocked");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduleRescue(event.getPlayer(), 30L);
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        scheduleRescue(event.getPlayer(), 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.renderer().forget(event.getPlayer());
    }

    private void scheduleRescue(final Player player, long delay) {
        if (!plugin.settings().enforce || !plugin.settings().rescueOnJoin) {
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                Region region = plugin.regions().get(player.getWorld());
                if (region == null || !region.isActive() || isExempt(player)) {
                    return;
                }
                Location loc = player.getLocation();
                if (region.contains(loc)) {
                    return;
                }
                Location target = region.getSpawn();
                if (target == null) {
                    target = region.nearestInside(loc);
                }
                if (target == null) {
                    return;
                }
                target.setYaw(loc.getYaw());
                target.setPitch(loc.getPitch());
                player.teleport(target);
                plugin.lang().send(player, "rescued");
                plugin.effects().touch(target.clone().add(0.0D, 1.0D, 0.0D));
            }
        }, delay);
    }

    public boolean isExempt(Player player) {
        if (!plugin.settings().enforce) {
            return true;
        }
        if (plugin.renderer().isBypassing(player)) {
            return true;
        }
        if (plugin.settings().ignoreOps && player.isOp()) {
            return true;
        }
        if (plugin.settings().ignoreGamemodes.contains(player.getGameMode())) {
            return true;
        }
        String perm = plugin.settings().bypassPermission;
        return perm != null && !perm.isEmpty() && player.hasPermission(perm);
    }
}
