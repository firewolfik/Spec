package xd.firewolfik.spec.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import xd.firewolfik.spec.Main;
import xd.firewolfik.spec.config.ConfigService;

/**
 * Handles playing configured action sounds to players with cached lookups and graceful fallbacks.
 */
public final class SoundService {

    private final Main plugin;
    private final ConfigService config;
    private final Map<String, SoundEffect> soundCache = new HashMap<>();
    private final Set<String> warnedUnknownSounds = new HashSet<>();

    public SoundService(Main plugin, ConfigService config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void play(Player player, String action) {
        if (!config.getBoolean("sounds.enabled", true) || player == null || !player.isOnline()) {
            return;
        }

        SoundEffect effect = soundCache.get(action);
        if (effect == null && !soundCache.containsKey(action)) {
            effect = loadSoundEffect(action);
            soundCache.put(action, effect);
        }

        if (effect != null) {
            effect.playTo(player);
        }
    }

    public void reload() {
        soundCache.clear();
        warnedUnknownSounds.clear();
    }

    private SoundEffect loadSoundEffect(String action) {
        String pathPrefix = "sounds." + action + '.';
        String soundName = config.getString(pathPrefix + "name").trim().toUpperCase(Locale.ROOT);
        if (soundName.isEmpty()) {
            return null;
        }

        try {
            Sound sound = Sound.valueOf(soundName);
            float volume = (float) config.getDouble(pathPrefix + "volume", 1.0);
            float pitch = (float) config.getDouble(pathPrefix + "pitch", 1.0);
            return new SoundEffect(sound, volume, pitch);
        } catch (IllegalArgumentException exception) {
            if (warnedUnknownSounds.add(soundName)) {
                plugin.getLogger().warning("Unknown sound '" + soundName + "' configured at " + pathPrefix + "name");
            }
            return null;
        }
    }

    private static final class SoundEffect {
        private final Sound sound;
        private final float volume;
        private final float pitch;

        SoundEffect(Sound sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }

        void playTo(Player player) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }
}
