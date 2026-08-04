package xd.firewolfik.spec.repository;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import xd.firewolfik.spec.Main;
import xd.firewolfik.spec.model.SpecSession;

public final class SessionRepository {
    private final Main plugin;
    private final File file;

    public SessionRepository(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "sessions.yml");
    }

    public Map<UUID, SpecSession> load() {
        Map<UUID, SpecSession> sessions = new HashMap<>();
        if (!file.isFile()) {
            return sessions;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("sessions");
        if (root == null) {
            return sessions;
        }

        for (String key : root.getKeys(false)) {
            try {
                String path = "sessions." + key + '.';
                UUID moderatorId = UUID.fromString(key);
                sessions.put(moderatorId, new SpecSession(
                        moderatorId,
                        UUID.fromString(requireString(yaml, path + "target")),
                        requireString(yaml, path + "world"),
                        yaml.getDouble(path + "x"),
                        yaml.getDouble(path + "y"),
                        yaml.getDouble(path + "z"),
                        (float) yaml.getDouble(path + "yaw"),
                        (float) yaml.getDouble(path + "pitch"),
                        GameMode.valueOf(requireString(yaml, path + "game-mode")),
                        yaml.getBoolean(path + "allow-flight"),
                        yaml.getBoolean(path + "flying"),
                        yaml.getLong(path + "started-at")
                ));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().log(Level.WARNING, "Ignoring invalid session '" + key + "'", exception);
            }
        }
        return sessions;
    }

    public void save(Collection<SpecSession> sessions) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (SpecSession session : sessions) {
            String path = "sessions." + session.moderatorId() + '.';
            yaml.set(path + "target", session.targetId().toString());
            yaml.set(path + "world", session.worldName());
            yaml.set(path + "x", session.x());
            yaml.set(path + "y", session.y());
            yaml.set(path + "z", session.z());
            yaml.set(path + "yaw", session.yaw());
            yaml.set(path + "pitch", session.pitch());
            yaml.set(path + "game-mode", session.gameMode().name());
            yaml.set(path + "allow-flight", session.allowFlight());
            yaml.set(path + "flying", session.flying());
            yaml.set(path + "started-at", session.startedAt());
        }

        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save sessions.yml", exception);
        }
    }

    private String requireString(YamlConfiguration yaml, String path) {
        String value = yaml.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing value at " + path);
        }
        return value;
    }
}
