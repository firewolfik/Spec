package xd.firewolfik.spec.command;

import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import xd.firewolfik.spec.Main;
import xd.firewolfik.spec.message.MessageService;
import xd.firewolfik.spec.service.SoundService;
import xd.firewolfik.spec.service.SpecRequestService;
import xd.firewolfik.spec.service.SpectatorResult;
import xd.firewolfik.spec.service.SpectatorService;

/**
 * Handles the /spec command:
 * - /spec <player>: Begin spectating or switch target (restricted to active requests unless moderator has spec.any)
 * - /spec: Stop spectating
 * - /spec alerts [on|off|toggle|status]: Toggle or set request alert notifications with persistence
 * - /spec reload: Reload configuration and active sessions
 */
public final class SpecCommand implements CommandExecutor {

    private final Main plugin;
    private final MessageService messages;
    private final SpectatorService spectators;
    private final SpecRequestService requests;
    private final SoundService sounds;

    public SpecCommand(
            Main plugin,
            MessageService messages,
            SpectatorService spectators,
            SpecRequestService requests,
            SoundService sounds
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.spectators = spectators;
        this.requests = requests;
        this.sounds = sounds;
    }

    @Override
    public boolean onCommand(
            @NonNull CommandSender sender,
            @NonNull Command command,
            @NonNull String label,
            String[] args
    ) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            handleReload(sender);
            return true;
        }

        if (!(sender instanceof Player)) {
            messages.send(sender, "messages.player-only");
            return true;
        }

        Player moderator = (Player) sender;
        if (!moderator.hasPermission("spec.use")) {
            messages.send(moderator, "messages.no-permission");
            return true;
        }

        if (args.length == 0) {
            handleStopOrUsage(moderator);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("alerts")) {
            boolean nowEnabled = requests.toggleAlerts(moderator);
            if (nowEnabled) {
                messages.send(moderator, "messages.alerts-enabled");
            } else {
                messages.send(moderator, "messages.alerts-disabled");
            }
            return true;
        }

        handleSpectate(moderator, args[0]);
        return true;
    }

    private void handleStopOrUsage(Player moderator) {
        boolean stopped = spectators.stop(moderator, true);
        if (!stopped) {
            messages.send(moderator, "messages.usage");
        }
    }

    private void handleSpectate(Player moderator, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            messages.send(moderator, "messages.player-not-found", "player", targetName);
            sounds.play(moderator, "error");
            return;
        }

        if (target.equals(moderator)) {
            messages.send(moderator, "messages.cannot-spec-yourself");
            sounds.play(moderator, "error");
            return;
        }

        boolean canSpecAny = moderator.hasPermission("spec.any");
        boolean isCurrentTarget = spectators.isSpectatingTarget(moderator.getUniqueId(), target.getUniqueId());
        boolean hasRequest = requests.hasActiveRequest(target.getUniqueId());

        if (!canSpecAny && !isCurrentTarget && !hasRequest) {
            messages.send(moderator, "messages.no-active-request", "player", target.getName());
            sounds.play(moderator, "error");
            return;
        }

        SpectatorResult result = spectators.observe(moderator, target);
        if (result == SpectatorResult.STARTED || result == SpectatorResult.SWITCHED) {
            requests.consumeRequest(target.getUniqueId());
        }

        notifySpectateResult(moderator, target, result);
    }

    private void notifySpectateResult(Player moderator, Player target, SpectatorResult result) {
        switch (result) {
            case TELEPORT_FAILED:
                messages.send(moderator, "messages.teleport-failed");
                sounds.play(moderator, "error");
                break;
            case RETELEPORTED:
                messages.send(moderator, "messages.spec-teleported", "player", target.getName());
                break;
            case STARTED:
            case SWITCHED:
            default:
                messages.send(moderator, "messages.spec-started", "player", target.getName());
                break;
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("spec.reload")) {
            messages.send(sender, "messages.no-permission");
            return;
        }

        try {
            plugin.reloadPlugin();
            messages.send(sender, "messages.reload-success");
            if (sender instanceof Player) {
                sounds.play((Player) sender, "reload");
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not reload config.yml", exception);
            messages.send(sender, "messages.reload-error");
        }
    }
}
