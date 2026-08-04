package xd.firewolfik.spec.repository;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
    private static final String CREATE_SESSIONS = "CREATE TABLE IF NOT EXISTS sessions ("
            + "moderator_id TEXT PRIMARY KEY, target_id TEXT NOT NULL, world_name TEXT NOT NULL, "
            + "x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL, yaw REAL NOT NULL, pitch REAL NOT NULL, "
            + "game_mode TEXT NOT NULL, allow_flight INTEGER NOT NULL, flying INTEGER NOT NULL, "
            + "started_at INTEGER NOT NULL)";
    private static final String CREATE_METADATA = "CREATE TABLE IF NOT EXISTS metadata ("
            + "key TEXT PRIMARY KEY, value TEXT NOT NULL)";
    private static final String INSERT_SESSION = "INSERT INTO sessions (moderator_id, target_id, world_name, "
            + "x, y, z, yaw, pitch, game_mode, allow_flight, flying, started_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final Main plugin;
    private final File databaseFile;
    private final File legacyFile;

    public SessionRepository(Main plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "sessions.db");
        this.legacyFile = new File(plugin.getDataFolder(), "sessions.yml");

        try {
            Class.forName("org.sqlite.JDBC");
            initialize();
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("SQLite JDBC driver is missing", exception);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not initialize sessions.db", exception);
        }
    }

    public Map<UUID, SpecSession> load() {
        Map<UUID, SpecSession> sessions = loadDatabase();
        if (legacyFile.isFile() && !isLegacyMigrationComplete()) {
            Map<UUID, SpecSession> legacySessions = loadLegacy();
            for (Map.Entry<UUID, SpecSession> entry : legacySessions.entrySet()) {
                if (!sessions.containsKey(entry.getKey())) {
                    sessions.put(entry.getKey(), entry.getValue());
                }
            }
            save(sessions.values());
            markLegacyMigrationComplete();
            plugin.getLogger().info("Migrated " + legacySessions.size() + " session(s) from sessions.yml to SQLite");
        }
        return sessions;
    }

    public void save(Collection<SpecSession> sessions) {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try (Statement delete = connection.createStatement();
                 PreparedStatement insert = connection.prepareStatement(INSERT_SESSION)) {
                delete.executeUpdate("DELETE FROM sessions");
                for (SpecSession session : sessions) {
                    bind(insert, session);
                    insert.addBatch();
                }
                insert.executeBatch();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save sessions to SQLite", exception);
        }
    }

    private void initialize() throws SQLException {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_SESSIONS);
            statement.executeUpdate(CREATE_METADATA);
        }
    }

    private Map<UUID, SpecSession> loadDatabase() {
        Map<UUID, SpecSession> sessions = new HashMap<>();
        String query = "SELECT moderator_id, target_id, world_name, x, y, z, yaw, pitch, game_mode, "
                + "allow_flight, flying, started_at FROM sessions";
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(query)) {
            while (result.next()) {
                UUID moderatorId = UUID.fromString(result.getString("moderator_id"));
                sessions.put(moderatorId, new SpecSession(
                        moderatorId,
                        UUID.fromString(result.getString("target_id")),
                        result.getString("world_name"),
                        result.getDouble("x"),
                        result.getDouble("y"),
                        result.getDouble("z"),
                        result.getFloat("yaw"),
                        result.getFloat("pitch"),
                        GameMode.valueOf(result.getString("game_mode")),
                        result.getInt("allow_flight") != 0,
                        result.getInt("flying") != 0,
                        result.getLong("started_at")
                ));
            }
        } catch (SQLException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not load sessions from SQLite", exception);
        }
        return sessions;
    }

    private Map<UUID, SpecSession> loadLegacy() {
        Map<UUID, SpecSession> sessions = new HashMap<>();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(legacyFile);
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
                plugin.getLogger().log(Level.WARNING, "Ignoring invalid legacy session '" + key + "'", exception);
            }
        }
        return sessions;
    }

    private boolean isLegacyMigrationComplete() {
        String query = "SELECT value FROM metadata WHERE key = 'legacy-yaml-migrated'";
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(query)) {
            return result.next();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not read SQLite migration state", exception);
        }
    }

    private void markLegacyMigrationComplete() {
        String query = "INSERT OR REPLACE INTO metadata (key, value) VALUES ('legacy-yaml-migrated', '1')";
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(query);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save SQLite migration state", exception);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
    }

    private void bind(PreparedStatement statement, SpecSession session) throws SQLException {
        statement.setString(1, session.moderatorId().toString());
        statement.setString(2, session.targetId().toString());
        statement.setString(3, session.worldName());
        statement.setDouble(4, session.x());
        statement.setDouble(5, session.y());
        statement.setDouble(6, session.z());
        statement.setFloat(7, session.yaw());
        statement.setFloat(8, session.pitch());
        statement.setString(9, session.gameMode().name());
        statement.setInt(10, session.allowFlight() ? 1 : 0);
        statement.setInt(11, session.flying() ? 1 : 0);
        statement.setLong(12, session.startedAt());
    }

    private String requireString(YamlConfiguration yaml, String path) {
        String value = yaml.getString(path);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing value at " + path);
        }
        return value;
    }
}
