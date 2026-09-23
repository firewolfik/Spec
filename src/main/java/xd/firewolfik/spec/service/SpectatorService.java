package xd.firewolfik.spec.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
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
    private final SoundService sounds;

    public SpectatorService(
            SpecSessionManager sessions,
            PlayerStateService playerState,
            VisibilityService visibility,
            ActionBarService actionBar,
            MessageService messages,
            SoundService sounds
    ) {
        this.sessions = sessions;
        this.playerState = playerState;
        this.visibility = visibility;
        this.actionBar = actionBar;
        this.messages = messages;
        this.sounds = sounds;
    }

    public SpectatorResult observe(Player moderator, Player target) {
        SpecSession currentSession = sessions.get(moderator.getUniqueId());
        if (currentSession != null) {
            return switchOrReteleport(moderator, target, currentSession);
        }

        return startSpectating(moderator, target);
    }

    private SpectatorResult switchOrReteleport(Player moderator, Player target, SpecSession currentSession) {
        if (!moderator.teleport(target.getLocation())) {
            return SpectatorResult.TELEPORT_FAILED;
        }

        sounds.play(moderator, "teleport");

        if (currentSession.targetId().equals(target.getUniqueId())) {
            return SpectatorResult.RETELEPORTED;
        }

        sessions.put(currentSession.withTarget(target.getUniqueId()));
        return SpectatorResult.SWITCHED;
    }

    private SpectatorResult startSpectating(Player moderator, Player target) {
        SpecSession newSession = SpecSession.capture(target.getUniqueId(), moderator);
        sessions.put(newSession);

        moderator.setFireTicks(0);
        moderator.setGameMode(GameMode.SPECTATOR);

        if (!moderator.teleport(target.getLocation())) {
            playerState.restore(moderator, newSession);
            sessions.remove(moderator.getUniqueId());
            actionBar.refresh();
            return SpectatorResult.TELEPORT_FAILED;
        }

        visibility.hideModerator(moderator);
        actionBar.refresh();
        sounds.play(moderator, "start");
        return SpectatorResult.STARTED;
    }

    public UUID getCurrentTarget(UUID moderatorId) {
        SpecSession session = sessions.get(moderatorId);
        return session != null ? session.targetId() : null;
    }

    public boolean isSpectatingTarget(UUID moderatorId, UUID targetId) {
        SpecSession session = sessions.get(moderatorId);
        return session != null && session.targetId().equals(targetId);
    }

    public boolean stop(Player moderator, boolean notifyPlayer) {
        return stopSession(moderator, notifyPlayer, true);
    }

    private boolean stopSession(Player moderator, boolean notifyPlayer, boolean refreshActionBar) {
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

        if (notifyPlayer) {
            messages.send(moderator, "messages.spec-stopped");
            sounds.play(moderator, "stop");
        }

        return true;
    }

    public void restoreAfterRestart(Player moderator) {
        if (stop(moderator, false)) {
            messages.send(moderator, "messages.restored-after-restart");
        }
    }

    public void notifyTargetQuit(Player target) {
        UUID targetId = target.getUniqueId();
        String targetName = target.getName();

        for (SpecSession session : sessions.getAll()) {
            if (!session.targetId().equals(targetId)) {
                continue;
            }

            Player moderator = Bukkit.getPlayer(session.moderatorId());
            if (moderator != null && moderator.isOnline()) {
                messages.send(moderator, "messages.target-left", "player", targetName);
            }
        }
    }

    public void shutdown() {
        actionBar.stop();

        for (SpecSession session : new ArrayList<>(sessions.getAll())) {
            Player moderator = Bukkit.getPlayer(session.moderatorId());
            if (moderator != null && moderator.isOnline()) {
                stopSession(moderator, false, false);
            }
        }

        sessions.save();
    }
}
