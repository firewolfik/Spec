package xd.firewolfik.spec.model;

import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Immutable data snapshot representing a moderator's active spectating session,
 * including their pre-spectate location and movement state.
 */
public final class SpecSession {

    private final UUID moderatorId;
    private final UUID targetId;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final GameMode gameMode;
    private final boolean allowFlight;
    private final boolean flying;
    private final long startedAt;

    public SpecSession(
            UUID moderatorId,
            UUID targetId,
            String worldName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            GameMode gameMode,
            boolean allowFlight,
            boolean flying,
            long startedAt
    ) {
        this.moderatorId = moderatorId;
        this.targetId = targetId;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.gameMode = gameMode;
        this.allowFlight = allowFlight;
        this.flying = flying;
        this.startedAt = startedAt;
    }

    public static SpecSession capture(UUID targetId, Player moderator) {
        Location location = moderator.getLocation();
        return new SpecSession(
                moderator.getUniqueId(),
                targetId,
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch(),
                moderator.getGameMode(),
                moderator.getAllowFlight(),
                moderator.isFlying(),
                System.currentTimeMillis()
        );
    }

    public SpecSession withTarget(UUID newTargetId) {
        return new SpecSession(
                moderatorId,
                newTargetId,
                worldName,
                x,
                y,
                z,
                yaw,
                pitch,
                gameMode,
                allowFlight,
                flying,
                System.currentTimeMillis()
        );
    }

    public UUID moderatorId() {
        return moderatorId;
    }

    public UUID targetId() {
        return targetId;
    }

    public String worldName() {
        return worldName;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public GameMode gameMode() {
        return gameMode;
    }

    public boolean allowFlight() {
        return allowFlight;
    }

    public boolean flying() {
        return flying;
    }

    public long startedAt() {
        return startedAt;
    }
}
