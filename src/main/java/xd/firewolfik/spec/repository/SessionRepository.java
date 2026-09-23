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

    private static final String CREATE_SESSIONS_TABLE = ""
            + "CREATE TABLE IF NOT EXISTS sessions ("
            + "    moderator_id TEXT PRIMARY KEY,"
            + "    target_id    TEXT NOT NULL,"
            + "    world_name   TEXT NOT NULL,"
            + "    x            REAL NOT NULL,"
            + "    y            REAL NOT NULL,"
            + "    z            REAL NOT NULL,"
            + "    yaw          REAL NOT NULL,"
            + "    pitch        REAL NOT NULL,"
            + "    game_mode    TEXT NOT NULL,"
            + "    allow_flight INTEGER NOT NULL,"
            + "    flying       INTEGER NOT NULL,"
            + "    started_at   INTEGER NOT NULL"
            + ")";

    private static final String CREATE_METADATA_TABLE = ""
            + "CREATE TABLE IF NOT EXISTS metadata ("
            + "    key   TEXT PRIMARY KEY,"
            + "    value TEXT NOT NULL"
            + ")";

    private static final String CREATE_ALERTS_TABLE = ""
            + "CREATE TABLE IF NOT EXISTS alert_preferences ("
            + "    moderator_id TEXT PRIMARY KEY,"
            + "    enabled      INTEGER NOT NULL"
            + ")";

    private static final String SELECT_ALL_SESSIONS = ""
            + "SELECT moderator_id, target_id, world_name, x, y, z, yaw, pitch, "
            + "       game_mode, allow_flight, flying, started_at "
            + "FROM sessions";

    private static final String INSERT_SESSION = ""
            + "INSERT INTO sessions ("
            + "    moderator_id, target_id, world_name, x, y, z, yaw, pitch, "
            + "    game_mode, allow_flight, flying, started_at"
            + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final Main plugin;
    private final File databaseFile;
    private final File legacyFile;

    public SessionRepository(Main plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "sessions.db");
        this.legacyFile = new File(plugin.getDataFolder(), "sessions.yml");

        ensureDriverLoaded();
        initializeDatabase();
    }

    public Map<UUID, SpecSession> load() {
        Map<UUID, SpecSession> sessions = loadFromDatabase();

        if (legacyFile.isFile() && !isLegacyMigrationComplete()) {
            migrateLegacySessions(sessions);
        }

        return sessions;
    }

    public void save(Collection<SpecSession> sessions) {
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);

            try (Statement deleteStmt = connection.createStatement();
                 PreparedStatement insertStmt = connection.prepareStatement(INSERT_SESSION)) {

                deleteStmt.executeUpdate("DELETE FROM sessions");

                for (SpecSession session : sessions) {
                    bindSessionParameters(insertStmt, session);
                    insertStmt.addBatch();
                }

                insertStmt.executeBatch();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save sessions to SQLite", exception);
        }
    }

    public Map<UUID, Boolean> loadAlertPreferences() {
        Map<UUID, Boolean> preferences = new HashMap<>();
        String query = "SELECT moderator_id, enabled FROM alert_preferences";
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("moderator_id"));
                boolean enabled = rs.getInt("enabled") != 0;
                preferences.put(uuid, enabled);
            }
        } catch (SQLException | IllegalArgumentException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not load alert preferences from SQLite", exception);
        }
        return preferences;
    }

    public void saveAlertPreference(UUID moderatorId, boolean enabled) {
        String sql = "INSERT OR REPLACE INTO alert_preferences (moderator_id, enabled) VALUES (?, ?)";
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, moderatorId.toString());
            statement.setInt(2, enabled ? 1 : 0);
            statement.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not save alert preference to SQLite for " + moderatorId, exception);
        }
    }

    private void ensureDriverLoaded() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("SQLite JDBC driver is missing from runtime classpath", exception);
        }
    }

    private void initializeDatabase() {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_SESSIONS_TABLE);
            statement.executeUpdate(CREATE_METADATA_TABLE);
            statement.executeUpdate(CREATE_ALERTS_TABLE);

            statement.executeUpdate(""
                    + "INSERT OR IGNORE INTO alert_preferences (moderator_id, enabled) "
                    + "SELECT moderator_id, 0 FROM disabled_alerts WHERE 1=1"
            );
        } catch (SQLException exception) {
            if (!exception.getMessage().contains("disabled_alerts")) {
                throw new IllegalStateException("Could not initialize SQLite schema in sessions.db", exception);
            }
        }
    }

    private Map<UUID, SpecSession> loadFromDatabase() {
        Map<UUID, SpecSession> sessions = new HashMap<>();

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(SELECT_ALL_SESSIONS)) {

            while (rs.next()) {
                SpecSession session = parseSessionRow(rs);
                sessions.put(session.moderatorId(), session);
            }
        } catch (SQLException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not load sessions from SQLite", exception);
        }

        return sessions;
    }

    private SpecSession parseSessionRow(ResultSet rs) throws SQLException {
        UUID moderatorId = UUID.fromString(rs.getString("moderator_id"));
        UUID targetId = UUID.fromString(rs.getString("target_id"));
        String worldName = rs.getString("world_name");
        double x = rs.getDouble("x");
        double y = rs.getDouble("y");
        double z = rs.getDouble("z");
        float yaw = rs.getFloat("yaw");
        float pitch = rs.getFloat("pitch");
        GameMode gameMode = GameMode.valueOf(rs.getString("game_mode"));
        boolean allowFlight = rs.getInt("allow_flight") != 0;
        boolean flying = rs.getInt("flying") != 0;
        long startedAt = rs.getLong("started_at");

        return new SpecSession(
                moderatorId,
                targetId,
                worldName,
                x,
                y,
                z,
                yaw,
                pitch,
                gameMode,
                allowFlight,
                flying,
                startedAt
        );
    }

    private void migrateLegacySessions(Map<UUID, SpecSession> activeSessions) {
        Map<UUID, SpecSession> legacySessions = loadLegacyYaml();

        for (Map.Entry<UUID, SpecSession> entry : legacySessions.entrySet()) {
            if (!activeSessions.containsKey(entry.getKey())) {
                activeSessions.put(entry.getKey(), entry.getValue());
            }
        }

        save(activeSessions.values());
        markLegacyMigrationComplete();
        plugin.getLogger().info("Migrated " + legacySessions.size() + " session(s) from sessions.yml to SQLite");
    }

    private Map<UUID, SpecSession> loadLegacyYaml() {
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
                        UUID.fromString(requireYamlString(yaml, path + "target")),
                        requireYamlString(yaml, path + "world"),
                        yaml.getDouble(path + "x"),
                        yaml.getDouble(path + "y"),
                        yaml.getDouble(path + "z"),
                        (float) yaml.getDouble(path + "yaw"),
                        (float) yaml.getDouble(path + "pitch"),
                        GameMode.valueOf(requireYamlString(yaml, path + "game-mode")),
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
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(query);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save SQLite migration state", exception);
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
    }

    private void bindSessionParameters(PreparedStatement statement, SpecSession session) throws SQLException {
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

    private String requireYamlString(YamlConfiguration yaml, String path) {
        String value = yaml.getString(path);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing value at " + path);
        }
        return value;
    }
}
