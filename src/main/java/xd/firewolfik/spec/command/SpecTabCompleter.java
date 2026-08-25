package xd.firewolfik.spec.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public final class SpecTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(
            @NonNull CommandSender sender,
            @NonNull Command command,
            @NonNull String alias,
            String[] args
    ) {
        if (args.length != 1 || args[0].isEmpty() || !sender.hasPermission("spec.use")) {
            return Collections.emptyList();
        }

        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> completions = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.equals(sender) && player.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                completions.add(player.getName());
            }
        }
        return completions;
    }
}
