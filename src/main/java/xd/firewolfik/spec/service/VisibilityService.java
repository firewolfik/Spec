package xd.firewolfik.spec.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xd.firewolfik.spec.Main;
import xd.firewolfik.spec.manager.SpecSessionManager;
import xd.firewolfik.spec.model.SpecSession;

public final class VisibilityService {
    private final Main plugin;
    private final SpecSessionManager sessions;

    public VisibilityService(Main plugin, SpecSessionManager sessions) {
        this.plugin = plugin;
        this.sessions = sessions;
    }

    public void hideModerator(Player moderator) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            update(viewer, moderator, true);
        }
    }

    public void showModerator(Player moderator) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            update(viewer, moderator, false);
        }
    }

    public void applyForJoiningViewer(Player viewer) {
        for (SpecSession session : sessions.getAll()) {
            Player moderator = Bukkit.getPlayer(session.moderatorId());
            if (moderator != null) {
                update(viewer, moderator, true);
            }
        }
    }

    private void update(Player viewer, Player moderator, boolean hidden) {
        if (viewer.equals(moderator)) {
            return;
        }
        if (hidden && !viewer.hasPermission("spec.see")) {
            viewer.hidePlayer(plugin, moderator);
        } else {
            viewer.showPlayer(plugin, moderator);
        }
    }
}
