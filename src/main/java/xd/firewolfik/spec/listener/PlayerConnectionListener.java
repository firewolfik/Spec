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
import xd.firewolfik.spec.service.SpectatorService;
import xd.firewolfik.spec.service.VisibilityService;

/**
 * Handles connection events to restore interrupted sessions and update tab-list masking.
 */
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
        final Player player = event.getPlayer();

        // If joining in SILENT mode, mask any already spectating moderators in this player's tab
        visibility.applyForJoiningViewer(player);

        // If this player was spectating before a server restart/crash, restore their pre-spectate state
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

        // Notify moderators if the player they were watching disconnected
        spectators.notifyTargetQuit(player);

        // If the disconnecting player was spectating, stop and restore their session
        spectators.stop(player, true);
    }
}
