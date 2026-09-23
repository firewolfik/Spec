package xd.firewolfik.spec.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xd.firewolfik.spec.Main;
import xd.firewolfik.spec.manager.SpecSessionManager;
import xd.firewolfik.spec.service.PermissionTrackerService;
import xd.firewolfik.spec.service.SpectatorService;
import xd.firewolfik.spec.service.VisibilityService;

public final class PlayerConnectionListener implements Listener {

    private final Main plugin;
    private final SpecSessionManager sessions;
    private final SpectatorService spectators;
    private final VisibilityService visibility;
    private final PermissionTrackerService permissionTracker;

    public PlayerConnectionListener(
            Main plugin,
            SpecSessionManager sessions,
            SpectatorService spectators,
            VisibilityService visibility,
            PermissionTrackerService permissionTracker
    ) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.spectators = spectators;
        this.visibility = visibility;
        this.permissionTracker = permissionTracker;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        visibility.applyForJoiningViewer(player);
        permissionTracker.handleJoin(player);

        if (sessions.contains(player.getUniqueId())) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    spectators.restoreAfterRestart(player);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        permissionTracker.handleQuit(player);
        spectators.notifyTargetQuit(player);
        spectators.stop(player, true);
    }
}
