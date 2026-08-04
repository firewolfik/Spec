package xd.firewolfik.spec.command;

import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xd.firewolfik.spec.Main;
import xd.firewolfik.spec.config.ConfigService;
import xd.firewolfik.spec.message.MessageService;
import xd.firewolfik.spec.service.ActionBarService;
import xd.firewolfik.spec.service.SpectatorResult;
import xd.firewolfik.spec.service.SpectatorService;

public final class SpecCommand implements CommandExecutor {
    private final Main plugin;
    private final ConfigService config;
    private final MessageService messages;
    private final SpectatorService spectators;
    private final ActionBarService actionBar;

    public SpecCommand(
            Main plugin,
            ConfigService config,
            MessageService messages,
            SpectatorService spectators,
            ActionBarService actionBar
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.spectators = spectators;
        this.actionBar = actionBar;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reload(sender);
            return true;
        }
        if (!(sender instanceof Player moderator)) {
            messages.send(sender, "messages.player-only");
            return true;
        }
        if (!moderator.hasPermission("spec.use")) {
            messages.send(moderator, "messages.no-permission");
            return true;
        }
        if (args.length == 0) {
            if (!spectators.stop(moderator, true)) {
                messages.send(moderator, "messages.usage");
            }
            return true;
        }
        if (args.length != 1) {
            messages.send(moderator, "messages.usage");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            messages.send(moderator, "messages.player-not-found", Map.of("player", args[0]));
            return true;
        }
        if (target.equals(moderator)) {
            messages.send(moderator, "messages.cannot-spec-yourself");
            return true;
        }

        SpectatorResult result = spectators.observe(moderator, target);
        if (result == SpectatorResult.TELEPORT_FAILED) {
            messages.send(moderator, "messages.teleport-failed");
        } else if (result == SpectatorResult.RETELEPORTED) {
            messages.send(moderator, "messages.spec-teleported", Map.of("player", target.getName()));
        } else {
            messages.send(moderator, "messages.spec-started", Map.of("player", target.getName()));
        }
        return true;
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("spec.reload")) {
            messages.send(sender, "messages.no-permission");
            return;
        }
        try {
            config.reload();
            actionBar.refresh();
            messages.send(sender, "messages.reload-success");
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not reload config.yml", exception);
            messages.send(sender, "messages.reload-error");
        }
    }
}
