package xd.firewolfik.spec.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import xd.firewolfik.spec.Main;
import xd.firewolfik.spec.service.SpecRequestService;

public final class PlayerChatListener implements Listener {

    private final Main plugin;
    private final SpecRequestService requestService;

    public PlayerChatListener(Main plugin, SpecRequestService requestService) {
        this.plugin = plugin;
        this.requestService = requestService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(final AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final String message = event.getMessage();

        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    requestService.handleChatMessage(player, message);
                }
            }
        });
    }
}
