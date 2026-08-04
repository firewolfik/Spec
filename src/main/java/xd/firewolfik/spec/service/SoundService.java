package xd.firewolfik.spec.service;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import xd.firewolfik.spec.Main;
import xd.firewolfik.spec.config.ConfigService;

public final class SoundService {
    private final Main plugin;
    private final ConfigService config;
    private final Set<String> invalidSounds = new HashSet<>();

    public SoundService(Main plugin, ConfigService config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void play(Player player, String action) {
        if (!config.getBoolean("sounds.enabled", true)) {
            return;
        }

        String path = "sounds." + action + '.';
        String name = config.getString(path + "name").trim().toUpperCase(Locale.ROOT);
        if (name.isEmpty()) {
            return;
        }

        try {
            Sound sound = Sound.valueOf(name);
            float volume = (float) config.getDouble(path + "volume", 1.0);
            float pitch = (float) config.getDouble(path + "pitch", 1.0);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException exception) {
            if (invalidSounds.add(name)) {
                plugin.getLogger().warning("Unknown sound '" + name + "' at " + path + "name");
            }
        }
    }

    public void reload() {
        invalidSounds.clear();
    }
}
