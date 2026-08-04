package xd.firewolfik.spec.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xd.firewolfik.spec.Main;
import xd.firewolfik.spec.manager.SpecSessionManager;
import xd.firewolfik.spec.service.SpectatorService;
import xd.firewolfik.spec.service.VisibilityService;

public final class PlayerConnectionListener implements Listener {
    private final Main plugin;
    private final SpecSessionManager sessions;
    private final SpectatorService spectators;
    private final VisibilityService visibility;

    public PlayerConnectionListener(
            Main plugin,
            SpecSessionManager sessions,
            SpectatorService spectators,
            VisibilityService visibility
    ) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.spectators = spectators;
        this.visibility = visibility;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        visibility.applyForJoiningViewer(event.getPlayer());
        if (sessions.contains(event.getPlayer().getUniqueId())) {
            Bukkit.getScheduler().runTask(plugin, () -> spectators.restoreAfterRestart(event.getPlayer()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        spectators.notifyTargetQuit(event.getPlayer());
        spectators.stop(event.getPlayer(), true);
    }
}
