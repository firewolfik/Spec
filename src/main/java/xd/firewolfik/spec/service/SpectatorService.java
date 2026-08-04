package xd.firewolfik.spec.service;

import java.util.ArrayList;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import xd.firewolfik.spec.manager.SpecSessionManager;
import xd.firewolfik.spec.message.MessageService;
import xd.firewolfik.spec.model.SpecSession;

public final class SpectatorService {
    private final SpecSessionManager sessions;
    private final PlayerStateService playerState;
    private final VisibilityService visibility;
    private final ActionBarService actionBar;
    private final MessageService messages;

    public SpectatorService(
            SpecSessionManager sessions,
            PlayerStateService playerState,
            VisibilityService visibility,
            ActionBarService actionBar,
            MessageService messages
    ) {
        this.sessions = sessions;
        this.playerState = playerState;
        this.visibility = visibility;
        this.actionBar = actionBar;
        this.messages = messages;
    }

    public SpectatorResult observe(Player moderator, Player target) {
        SpecSession current = sessions.get(moderator.getUniqueId());
        if (current != null) {
            if (!moderator.teleport(target.getLocation())) {
                return SpectatorResult.TELEPORT_FAILED;
            }
            if (current.targetId().equals(target.getUniqueId())) {
                return SpectatorResult.RETELEPORTED;
            }

            sessions.put(current.withTarget(target.getUniqueId()));
            return SpectatorResult.SWITCHED;
        }

        SpecSession created = SpecSession.capture(target.getUniqueId(), moderator);
        sessions.put(created);
        moderator.setFireTicks(0);
        moderator.setGameMode(GameMode.SPECTATOR);
        if (!moderator.teleport(target.getLocation())) {
            playerState.restore(moderator, created);
            sessions.remove(moderator.getUniqueId());
            actionBar.refresh();
            return SpectatorResult.TELEPORT_FAILED;
        }

        visibility.hideModerator(moderator);
        actionBar.refresh();
        return SpectatorResult.STARTED;
    }

    public boolean stop(Player moderator, boolean sendMessage) {
        return stop(moderator, sendMessage, true);
    }

    private boolean stop(Player moderator, boolean sendMessage, boolean refreshActionBar) {
        SpecSession session = sessions.get(moderator.getUniqueId());
        if (session == null) {
            return false;
        }

        playerState.restore(moderator, session);
        visibility.showModerator(moderator);
        sessions.remove(moderator.getUniqueId());
        if (refreshActionBar) {
            actionBar.refresh();
        }
        if (sendMessage) {
            messages.send(moderator, "messages.spec-stopped");
        }
        return true;
    }

    public void restoreAfterRestart(Player moderator) {
        if (stop(moderator, false)) {
            messages.send(moderator, "messages.restored-after-restart");
        }
    }

    public void notifyTargetQuit(Player target) {
        for (SpecSession session : sessions.getAll()) {
            if (!session.targetId().equals(target.getUniqueId())) {
                continue;
            }
            Player moderator = Bukkit.getPlayer(session.moderatorId());
            if (moderator != null && moderator.isOnline()) {
                messages.send(moderator, "messages.target-left", Map.of("player", target.getName()));
            }
        }
    }

    public void shutdown() {
        actionBar.stop();
        for (SpecSession session : new ArrayList<>(sessions.getAll())) {
            Player moderator = Bukkit.getPlayer(session.moderatorId());
            if (moderator != null && moderator.isOnline()) {
                stop(moderator, false, false);
            }
        }
        sessions.save();
    }
}
