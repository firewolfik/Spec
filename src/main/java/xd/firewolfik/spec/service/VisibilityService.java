package xd.firewolfik.spec.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xd.firewolfik.spec.Main;
import xd.firewolfik.spec.config.ConfigService;
import xd.firewolfik.spec.manager.SpecSessionManager;
import xd.firewolfik.spec.model.SpecSession;

public final class VisibilityService {

    private final Main plugin;
    private final SpecSessionManager sessions;
    private final ConfigService config;
    private final GamemodeMaskService masks;
    private final VanishService vanish;

    public VisibilityService(
            Main plugin,
            SpecSessionManager sessions,
            ConfigService config,
            GamemodeMaskService masks,
            VanishService vanish
    ) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.config = config;
        this.masks = masks;
        this.vanish = vanish;
    }

    public void hideModerator(Player moderator) {
        if (config.getVisibilityMode() == VisibilityMode.VANISH) {
            vanish.vanish(moderator);
            return;
        }

        vanish.unvanish(moderator);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(moderator)) {
                continue;
            }
            viewer.showPlayer(plugin, moderator);
            masks.mask(viewer, moderator);
        }
    }

    public void showModerator(Player moderator) {
        if (config.getVisibilityMode() == VisibilityMode.VANISH) {
            vanish.unvanish(moderator);
            return;
        }

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(moderator)) {
                viewer.showPlayer(plugin, moderator);
            }
        }
    }

    public void applyForJoiningViewer(Player viewer) {
        if (config.getVisibilityMode() != VisibilityMode.SILENT) {
            return;
        }

        for (SpecSession session : sessions.getAll()) {
            Player moderator = Bukkit.getPlayer(session.moderatorId());
            if (moderator != null && !viewer.equals(moderator)) {
                masks.mask(viewer, moderator);
            }
        }
    }

    public void refreshActiveSessions() {
        for (SpecSession session : sessions.getAll()) {
            Player moderator = Bukkit.getPlayer(session.moderatorId());
            if (moderator != null && moderator.isOnline()) {
                hideModerator(moderator);
            }
        }
    }
}
