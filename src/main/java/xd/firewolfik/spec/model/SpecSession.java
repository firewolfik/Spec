package xd.firewolfik.spec.model;

import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;

public record SpecSession(
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
    public static SpecSession capture(UUID targetId, org.bukkit.entity.Player moderator) {
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
}
