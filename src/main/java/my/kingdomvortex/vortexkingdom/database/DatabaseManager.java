
package my.kingdomvortex.vortexkingdom.database;

import my.kingdomvortex.vortexkingdom.VortexKingdom;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public final class DatabaseManager {

    private final VortexKingdom plugin;

    private Connection connection;

    public DatabaseManager(VortexKingdom plugin) {
        this.plugin = plugin;
    }

    // ==========================================
    // DATABASE CONNECTION
    // ==========================================

    public synchronized void connect() {
        if (isConnected()) {
            return;
        }

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

            if (databaseFile == null || databaseFile.isBlank()) {
                throw new IllegalArgumentException(
                        "Database filename cannot be empty."
                );
            }

            File database = new File(folder, databaseFile)
                    .getCanonicalFile();

            if (!folder.getCanonicalFile().equals(
                    database.getParentFile())) {
                throw new IllegalArgumentException(
                        "Invalid database file path."
                );
            }

            Class.forName("org.sqlite.JDBC");

            connection = DriverManager.getConnection(
                    "jdbc:sqlite:" + database.getAbsolutePath()
            );

            configureConnection();

            createTables();

            createIndexes();

            runMigrations();

            plugin.getLogger().info(
                    "SQLite database connected successfully!"
            );

        } catch (Exception exception) {

            disconnect();

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Failed to initialize SQLite database!",
                    exception
            );

            throw new IllegalStateException(
                    "Database initialization failed.",
                    exception
            );
        }
    }

    private void configureConnection() throws SQLException {

        try (Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA busy_timeout = 5000");

            statement.execute("PRAGMA foreign_keys = ON");

            statement.execute("PRAGMA journal_mode = WAL");

            statement.execute("PRAGMA synchronous = NORMAL");

            statement.execute("PRAGMA temp_store = MEMORY");

            statement.execute("PRAGMA cache_size = -8000");

        }
    }

    // ==========================================
    // TABLES
    // ==========================================

    private void createTables() throws SQLException {

        try (Statement statement = connection.createStatement()) {

            // KINGDOMS

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS kingdoms (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,

                    name TEXT NOT NULL COLLATE NOCASE UNIQUE,

                    owner_uuid TEXT NOT NULL,

                    level INTEGER NOT NULL DEFAULT 1,

                    experience INTEGER NOT NULL DEFAULT 0,

                    total_experience INTEGER NOT NULL DEFAULT 0,

                    total_kills INTEGER NOT NULL DEFAULT 0,

                    description TEXT NOT NULL DEFAULT '',

                    pvp_enabled INTEGER NOT NULL DEFAULT 0,

                    home_world TEXT,

                    home_x DOUBLE,

                    home_y DOUBLE,

                    home_z DOUBLE,

                    home_yaw FLOAT,

                    home_pitch FLOAT,

                    created_at INTEGER NOT NULL,

                    updated_at INTEGER NOT NULL DEFAULT 0
                )
                """);

            // KINGDOM MEMBERS

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

            // INVITATIONS

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

            // DAILY EXP

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS kingdom_daily_exp (
                    player_uuid TEXT NOT NULL,

                    date TEXT NOT NULL,

                    experience INTEGER NOT NULL DEFAULT 0,

                    PRIMARY KEY (player_uuid, date)
                )
                """);

            // LEGACY EXP COOLDOWN

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS kingdom_exp_cooldowns (
                    player_uuid TEXT PRIMARY KEY,

                    last_reward_at INTEGER NOT NULL
                )
                """);

            // PER-SOURCE COOLDOWN

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS kingdom_exp_source_cooldowns (
                    player_uuid TEXT NOT NULL,

                    source TEXT NOT NULL,

                    last_reward_at INTEGER NOT NULL,

                    PRIMARY KEY (player_uuid, source)
                )
                """);

            // ANTI-FARMING ACTIVITY

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS kingdom_exp_activity (
                    player_uuid TEXT NOT NULL,

                    source TEXT NOT NULL,

                    action_key TEXT NOT NULL,

                    action_count INTEGER NOT NULL DEFAULT 0,

                    window_started INTEGER NOT NULL,

                    last_reward_at INTEGER NOT NULL DEFAULT 0,

                    PRIMARY KEY (
                        player_uuid,
                        source,
                        action_key
                    )
                )
                """);

            // KINGDOM STATISTICS

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS kingdom_statistics (
                    kingdom_id INTEGER PRIMARY KEY,

                    total_mob_kills INTEGER NOT NULL DEFAULT 0,

                    total_boss_kills INTEGER NOT NULL DEFAULT 0,

                    total_blocks_mined INTEGER NOT NULL DEFAULT 0,

                    total_blocks_placed INTEGER NOT NULL DEFAULT 0,

                    total_quests_completed INTEGER NOT NULL DEFAULT 0,

                    FOREIGN KEY (kingdom_id)
                        REFERENCES kingdoms(id)
                        ON DELETE CASCADE
                )
                """);

            // KINGDOM AUDIT LOG

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS kingdom_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,

                    kingdom_id INTEGER,

                    player_uuid TEXT,

                    action TEXT NOT NULL,

                    details TEXT DEFAULT '',

                    created_at INTEGER NOT NULL
                )
                """);

            // DATABASE VERSION

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS kingdom_database_version (
                    id INTEGER PRIMARY KEY CHECK (id = 1),

                    version INTEGER NOT NULL
                )
                """);

            statement.executeUpdate("""
                INSERT OR IGNORE INTO kingdom_database_version
                (id, version)
                VALUES (1, 1)
                """);
        }
    }

    // ==========================================
    // INDEXES
    // ==========================================

    private void createIndexes() throws SQLException {

        try (Statement statement = connection.createStatement()) {

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

            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_kingdom_member_name
                ON kingdom_members(player_name)
                """);

            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_kingdom_invitation_target
                ON kingdom_invitations(target_uuid, expires_at)
                """);

            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_kingdom_daily_exp
                ON kingdom_daily_exp(date, player_uuid)
                """);

            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_kingdom_activity
                ON kingdom_exp_activity(player_uuid, source)
                """);

            statement.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_kingdom_logs
                ON kingdom_logs(kingdom_id, created_at DESC)
                """);
        }
    }

    // ==========================================
    // DATABASE MIGRATIONS
    // ==========================================

    private void runMigrations() throws SQLException {

        int version = getDatabaseVersion();

        if (version < 2) {

            addColumnIfMissing(
                    "kingdoms",
                    "updated_at",
                    "INTEGER NOT NULL DEFAULT 0"
            );

            setDatabaseVersion(2);
        }

        if (version < 3) {

            addColumnIfMissing(
                    "kingdoms",
                    "description",
                    "TEXT NOT NULL DEFAULT ''"
            );

            addColumnIfMissing(
                    "kingdoms",
                    "pvp_enabled",
                    "INTEGER NOT NULL DEFAULT 0"
            );

            setDatabaseVersion(3);
        }

        plugin.getLogger().info(
                "Database migration completed. Version: " +
                        getDatabaseVersion()
        );
    }

    private void addColumnIfMissing(
            String table,
            String column,
            String definition
    ) throws SQLException {

        boolean exists = false;

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "PRAGMA table_info(" + table + ")"
             )) {

            while (result.next()) {

                if (column.equalsIgnoreCase(
                        result.getString("name"))) {

                    exists = true;
                    break;
                }
            }
        }

        if (!exists) {

            try (Statement statement = connection.createStatement()) {

                statement.executeUpdate(
                        "ALTER TABLE " + table +
                                " ADD COLUMN " + column +
                                " " + definition
                );
            }
        }
    }

    private int getDatabaseVersion() throws SQLException {

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     """
                     SELECT version
                     FROM kingdom_database_version
                     WHERE id = 1
                     """
             )) {

            if (result.next()) {
                return result.getInt("version");
            }
        }

        return 1;
    }

    private void setDatabaseVersion(int version)
            throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             """
                             UPDATE kingdom_database_version
                             SET version = ?
                             WHERE id = 1
                             """
                     )) {

            statement.setInt(1, version);
            statement.executeUpdate();
        }
    }

    // ==========================================
    // CONNECTION ACCESS
    // ==========================================

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

            return connection != null &&
                    !connection.isClosed();

        } catch (SQLException exception) {

            return false;
        }
    }

    // ==========================================
    // TRANSACTION SUPPORT
    // ==========================================

    public synchronized void beginTransaction()
            throws SQLException {

        if (!isConnected()) {
            throw new IllegalStateException(
                    "Database is not connected!"
            );
        }

        if (!connection.getAutoCommit()) {
            throw new SQLException(
                    "A transaction is already active."
            );
        }

        connection.setAutoCommit(false);
    }

    public synchronized void commit()
            throws SQLException {

        if (isConnected() && !connection.getAutoCommit()) {

            connection.commit();

            connection.setAutoCommit(true);
        }
    }

    public synchronized void rollback() {

        try {

            if (isConnected() && !connection.getAutoCommit()) {

                connection.rollback();

                connection.setAutoCommit(true);
            }

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Failed to rollback transaction.",
                    exception
            );
        }
    }

    // ==========================================
    // DATABASE MAINTENANCE
    // ==========================================

    public synchronized void optimize() {

        if (!isConnected()) {
            return;
        }

        try (Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA optimize");

            plugin.getLogger().info(
                    "Database optimization completed."
            );

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.WARNING,
                    "Database optimization failed.",
                    exception
            );
        }
    }

    public synchronized void checkpoint() {

        if (!isConnected()) {
            return;
        }

        try (Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA wal_checkpoint(PASSIVE)");

        } catch (SQLException exception) {

            plugin.getLogger().log(
                    Level.WARNING,
                    "Database checkpoint failed.",
                    exception
            );
        }
    }

    // ==========================================
    // DISCONNECT
    // ==========================================

    public synchronized void disconnect() {

        if (connection == null) {
            return;
        }

        try {

            if (!connection.isClosed()) {

                if (!connection.getAutoCommit()) {

                    connection.rollback();

                    connection.setAutoCommit(true);
                }

                try (Statement statement =
                             connection.createStatement()) {

                    statement.execute(
                            "PRAGMA wal_checkpoint(TRUNCATE)"
                    );
                }

                connection.close();
            }

            plugin.getLogger().info(
                    "SQLite database disconnected successfully."
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
