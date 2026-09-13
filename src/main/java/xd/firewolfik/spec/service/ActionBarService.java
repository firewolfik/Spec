package xd.firewolfik.spec.service;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import xd.firewolfik.spec.Main;
import xd.firewolfik.spec.config.ConfigService;
import xd.firewolfik.spec.manager.SpecSessionManager;
import xd.firewolfik.spec.model.SpecSession;

/**
 * Periodically sends a configurable action bar message to all active spectator moderators.
 */
public final class ActionBarService {

    private final Main plugin;
    private final ConfigService config;
    private final SpecSessionManager sessions;

    private BukkitTask task;

    public ActionBarService(Main plugin, ConfigService config, SpecSessionManager sessions) {
        this.plugin = plugin;
        this.config = config;
        this.sessions = sessions;
    }

    public void refresh() {
        if (!config.isActionBarEnabled() || sessions.getAll().isEmpty()) {
            stop();
            return;
        }

        if (task == null) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::sendToAllModerators, 0L, 20L);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void sendToAllModerators() {
        if (!config.isActionBarEnabled()) {
            stop();
            return;
        }

        String formattedMessage = config.getActionBar();

        for (SpecSession session : sessions.getAll()) {
            Player moderator = Bukkit.getPlayer(session.moderatorId());
            if (moderator != null && moderator.isOnline()) {
                moderator.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(formattedMessage)
                );
            }
        }
    }
}
