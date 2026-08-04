package xd.firewolfik.spec.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import xd.firewolfik.spec.Main;
import xd.firewolfik.spec.model.SpecSession;

public final class PlayerStateService {
    private final Main plugin;

    public PlayerStateService(Main plugin) {
        this.plugin = plugin;
    }

    public void restore(Player player, SpecSession session) {
        if (!player.teleport(resolveLocation(session))) {
            plugin.getLogger().warning("Could not restore location for " + player.getName());
        }

        player.setGameMode(session.gameMode());
        if (!session.allowFlight() && player.isFlying()) {
            player.setFlying(false);
        }
        player.setAllowFlight(session.allowFlight());
        player.setFlying(session.allowFlight() && session.flying());
    }

    private Location resolveLocation(SpecSession session) {
        World world = Bukkit.getWorld(session.worldName());
        if (world != null) {
            return new Location(world, session.x(), session.y(), session.z(), session.yaw(), session.pitch());
        }
        if (Bukkit.getWorlds().isEmpty()) {
            throw new IllegalStateException("No loaded world is available to restore a spectator");
        }

        World fallback = Bukkit.getWorlds().get(0);
        plugin.getLogger().warning("World '" + session.worldName()
                + "' is unavailable; using the spawn of '" + fallback.getName() + "'");
        return fallback.getSpawnLocation();
    }
}
