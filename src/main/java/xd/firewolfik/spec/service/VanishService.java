package xd.firewolfik.spec.service;

import com.earth2me.essentials.Essentials;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import xd.firewolfik.spec.Main;

public final class VanishService {

    private final Main plugin;
    private final Set<UUID> vanishedByUs = new HashSet<>();
    private Essentials essentials;
    private boolean warnedUnavailable;

    public VanishService(Main plugin) {
        this.plugin = plugin;
    }

    public void vanish(Player moderator) {
        UUID id = moderator.getUniqueId();
        if (vanishedByUs.contains(id) || isAlreadyVanished(moderator)) {
            return;
        }

        Essentials ess = resolveEssentials();
        if (ess == null) {
            hideFromEveryone(moderator);
        } else {
            ess.getUser(moderator).setVanished(true);
        }

        vanishedByUs.add(id);
    }

    public void unvanish(Player moderator) {
        if (!vanishedByUs.remove(moderator.getUniqueId())) {
            return;
        }

        Essentials ess = resolveEssentials();
        if (ess == null) {
            showToEveryone(moderator);
        } else {
            ess.getUser(moderator).setVanished(false);
        }
    }

    private Essentials resolveEssentials() {
        if (essentials != null) {
            return essentials;
        }

        Plugin candidate = Bukkit.getPluginManager().getPlugin("Essentials");
        if (candidate instanceof Essentials && candidate.isEnabled()) {
            essentials = (Essentials) candidate;
            return essentials;
        }

        if (!warnedUnavailable) {
            warnedUnavailable = true;
            plugin.getLogger().warning(
                    "Essentials not found: VANISH mode falls back to plain Bukkit player hiding"
            );
        }

        return null;
    }

    private static boolean isAlreadyVanished(Player player) {
        return player.hasMetadata("vanished")
                && !player.getMetadata("vanished").isEmpty()
                && player.getMetadata("vanished").get(0).asBoolean();
    }

    private void hideFromEveryone(Player moderator) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(moderator)) {
                viewer.hidePlayer(plugin, moderator);
            }
        }
    }

    private void showToEveryone(Player moderator) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(moderator)) {
                viewer.showPlayer(plugin, moderator);
            }
        }
    }
}
