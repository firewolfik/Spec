package xd.firewolfik.spec.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import xd.firewolfik.spec.service.SpecRequestService;
import xd.firewolfik.spec.service.SpectatorService;

public final class SpecTabCompleter implements TabCompleter {

    private final SpecRequestService requests;
    private final SpectatorService spectators;

    public SpecTabCompleter(SpecRequestService requests, SpectatorService spectators) {
        this.requests = requests;
        this.spectators = spectators;
    }

    @Override
    public List<String> onTabComplete(
            @NonNull CommandSender sender,
            @NonNull Command command,
            @NonNull String alias,
            String[] args
    ) {
        if (args.length != 1 || !sender.hasPermission("spec.use")) {
            return Collections.emptyList();
        }

        String input = args[0];
        List<String> completions = new ArrayList<>();

        if (input.isEmpty()) {
            completions.add("alerts");
            if (sender.hasPermission("spec.reload")) {
                completions.add("reload");
            }
            return completions;
        }

        String prefix = input.toLowerCase(Locale.ROOT);

        if ("alerts".startsWith(prefix)) {
            completions.add("alerts");
        }

        if (sender.hasPermission("spec.reload") && "reload".startsWith(prefix)) {
            completions.add("reload");
        }

        boolean canSpecAny = sender.hasPermission("spec.any");
        Set<UUID> activeRequesters = requests.getActiveRequestPlayerIds();
        UUID currentTargetId = (sender instanceof Player)
                ? spectators.getCurrentTarget(((Player) sender).getUniqueId())
                : null;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.equals(sender)) {
                continue;
            }

            if (!canSpecAny && !activeRequesters.contains(player.getUniqueId())
                    && !player.getUniqueId().equals(currentTargetId)) {
                continue;
            }

            if (player.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                completions.add(player.getName());
            }
        }

        return completions;
    }
}
