
package my.kingdomvortex.vortexkingdom.database;

import my.kingdomvortex.vortexkingdom.VortexKingdom;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public final class DatabaseManager {

    private final VortexKingdom plugin;

    private Connection connection;

    public DatabaseManager(VortexKingdom plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            File folder = plugin.getDataFolder();

            if (!folder.exists() && !folder.mkdirs()) {
                throw new IllegalStateException(
                        "Cannot create plugin data folder."
                );
            }

            String databaseFile = plugin.getConfig().getString(
                    "database.file",
                    "kingdoms.db"
            );

            File database = new File(folder, databaseFile);

            if (!database.getCanonicalFile().getParentFile()
                    .equals(folder.getCanonicalFile())) {
                throw new IllegalArgumentException(
                        "Invalid database file path."
                );
            }

            Class.forName("org.sqlite.JDBC");

            connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + database.getAbsolutePath()
            );

            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA busy_timeout = 5000");
                statement.execute("PRAGMA foreign_keys = ON");
                statement.execute("PRAGMA journal_mode = WAL");
                statement.execute("PRAGMA synchronous = NORMAL");
            }

            createTables();

            plugin.getLogger().info(
                    "SQLite database connected successfully!"
            );

        } catch (Exception exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Failed to connect to SQLite database!",
                    exception
            );

            throw new IllegalStateException(
                    "Database initialization failed.",
                    exception
            );
        }
    }

    private void createTables() throws SQLException {

        try (Statement statement = connection.createStatement()) {

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS kingdoms (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL COLLATE NOCASE UNIQUE,
                    owner_uuid TEXT NOT NULL,
                    level INTEGER NOT NULL DEFAULT 1,
                    experience INTEGER NOT NULL DEFAULT 0,
                    total_experience INTEGER NOT NULL DEFAULT 0,
                    total_kills INTEGER NOT NULL DEFAULT 0,
                    description TEXT DEFAULT '',
                    pvp_enabled INTEGER NOT NULL DEFAULT 0,
                    home_world TEXT,
                    home_x DOUBLE,
                    home_y DOUBLE,
                    home_z DOUBLE,
                    home_yaw FLOAT,
                    home_pitch FLOAT,
                    created_at INTEGER NOT NULL
                )
                """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS kingdom_members (
                    kingdom_id INTEGER NOT NULL,
                    player_uuid TEXT NOT NULL UNIQUE,
                    player_name TEXT NOT NULL,
                    role TEXT NOT NULL DEFAULT 'MEMBER',
                    joined_at INTEGER NOT NULL,

                    PRIMARY KEY (kingdom_id, player_uuid),

                    FOREIGN KEY (kingdom_id)
                        REFERENCES kingdoms(id)
                        ON DELETE CASCADE
                )
                """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS kingdom_invitations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    kingdom_id INTEGER NOT NULL,
                    inviter_uuid TEXT NOT NULL,
                    target_uuid TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL,

                    UNIQUE (kingdom_id, target_uuid),

                    FOREIGN KEY (kingdom_id)
                        REFERENCES kingdoms(id)
                        ON DELETE CASCADE
                )
                """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS kingdom_daily_exp (
                    player_uuid TEXT NOT NULL,
                    date TEXT NOT NULL,
                    experience INTEGER NOT NULL DEFAULT 0,

                    PRIMARY KEY (player_uuid, date)
                )
                """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS kingdom_exp_cooldowns (
                    player_uuid TEXT PRIMARY KEY,
                    last_reward_at INTEGER NOT NULL
                )
                """);

            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_kingdom_level
                ON kingdoms(level DESC, total_experience DESC)
                """);

            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_kingdom_exp
                ON kingdoms(total_experience DESC)
                """);

            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_kingdom_members
                ON kingdom_members(kingdom_id)
                """);

            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_kingdom_owner
                ON kingdoms(owner_uuid)
                """);
        }
    }

    public synchronized Connection getConnection() {
        if (!isConnected()) {
            throw new IllegalStateException(
                    "Database is not connected!"
            );
        }

        return connection;
    }

    public synchronized boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException exception) {
            return false;
        }
    }

    public synchronized void disconnect() {
        if (connection == null) {
            return;
        }

        try {
            if (!connection.isClosed()) {
                connection.close();
            }

            plugin.getLogger().info(
                    "SQLite database disconnected."
            );

        } catch (SQLException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Failed to disconnect database.",
                    exception
            );
        } finally {
            connection = null;
        }
    }
}
