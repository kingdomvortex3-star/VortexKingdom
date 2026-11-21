
package my.kingdomvortex.vortexkingdom.manager;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.database.DatabaseManager;
import my.kingdomvortex.vortexkingdom.kingdom.Kingdom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class KingdomFeatureManager {

    private final VortexKingdom plugin;
    private final DatabaseManager database;

    public KingdomFeatureManager(VortexKingdom plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabaseManager();

        createSettingsTable();
    }

    // ==================================================
    // DATABASE
    // ==================================================

    private void createSettingsTable() {
        Connection connection = database.getConnection();

        synchronized (connection) {
            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 """
                                 CREATE TABLE IF NOT EXISTS kingdom_settings (
                                     kingdom_id INTEGER PRIMARY KEY,
                                     chat_enabled INTEGER NOT NULL DEFAULT 1
                                 )
                                 """
                         )) {

                statement.executeUpdate();

            } catch (SQLException exception) {
                plugin.getLogger().severe(
                        "Failed to create kingdom_settings table: "
                                + exception.getMessage()
                );
            }
        }
    }

    // ==================================================
    // KINGDOM HOME
    // ==================================================

    public boolean setHome(UUID player, Location location) {
        if (player == null || location == null
                || location.getWorld() == null) {
            return false;
        }

        Optional<Kingdom> optional =
                plugin.getKingdomManager().getKingdomByPlayer(player);

        if (optional.isEmpty()) {
            return false;
        }

        Kingdom kingdom = optional.get();
        Connection connection = database.getConnection();

        String sql = """
                UPDATE kingdoms
                SET home_world = ?,
                    home_x = ?,
                    home_y = ?,
                    home_z = ?,
                    home_yaw = ?,
                    home_pitch = ?,
                    updated_at = ?
                WHERE id = ?
                """;

        synchronized (connection) {
            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(
                        1,
                        location.getWorld().getName()
                );
                statement.setDouble(2, location.getX());
                statement.setDouble(3, location.getY());
                statement.setDouble(4, location.getZ());
                statement.setFloat(5, location.getYaw());
                statement.setFloat(6, location.getPitch());
                statement.setLong(7, System.currentTimeMillis());
                statement.setInt(8, kingdom.getId());

                return statement.executeUpdate() > 0;

            } catch (SQLException exception) {
                logError("setHome", exception);
                return false;
            }
        }
    }

    public Optional<HomeData> getHome(int kingdomId) {
        if (kingdomId <= 0) {
            return Optional.empty();
        }

        Connection connection = database.getConnection();

        String sql = """
                SELECT home_world, home_x, home_y, home_z,
                       home_yaw, home_pitch
                FROM kingdoms
                WHERE id = ?
                LIMIT 1
                """;

        synchronized (connection) {
            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setInt(1, kingdomId);

                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return Optional.empty();
                    }

                    String world = result.getString("home_world");

                    if (world == null || world.isBlank()) {
                        return Optional.empty();
                    }

                    double x = result.getDouble("home_x");

                    if (result.wasNull()) {
                        return Optional.empty();
                    }

                    double y = result.getDouble("home_y");

                    if (result.wasNull()) {
                        return Optional.empty();
                    }

                    double z = result.getDouble("home_z");

                    if (result.wasNull()) {
                        return Optional.empty();
                    }

                    float yaw = result.getFloat("home_yaw");
                    float pitch = result.getFloat("home_pitch");

                    return Optional.of(
                            new HomeData(
                                    world,
                                    x,
                                    y,
                                    z,
                                    yaw,
                                    pitch
                            )
                    );
                }

            } catch (SQLException exception) {
                logError("getHome", exception);
                return Optional.empty();
            }
        }
    }

    public Optional<Location> getHomeLocation(int kingdomId) {
        Optional<HomeData> optional = getHome(kingdomId);

        if (optional.isEmpty()) {
            return Optional.empty();
        }

        HomeData home = optional.get();
        World world = Bukkit.getWorld(home.world());

        if (world == null) {
            return Optional.empty();
        }

        return Optional.of(
                new Location(
                        world,
                        home.x(),
                        home.y(),
                        home.z(),
                        home.yaw(),
                        home.pitch()
                )
        );
    }

    public boolean deleteHome(UUID player) {
        if (player == null) {
            return false;
        }

        Optional<Kingdom> optional =
                plugin.getKingdomManager().getKingdomByPlayer(player);

        if (optional.isEmpty()) {
            return false;
        }

        Connection connection = database.getConnection();

        String sql = """
                UPDATE kingdoms
                SET home_world = NULL,
                    home_x = NULL,
                    home_y = NULL,
                    home_z = NULL,
                    home_yaw = NULL,
                    home_pitch = NULL,
                    updated_at = ?
                WHERE id = ?
                """;

        synchronized (connection) {
            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setLong(1, System.currentTimeMillis());
                statement.setInt(2, optional.get().getId());

                return statement.executeUpdate() > 0;

            } catch (SQLException exception) {
                logError("deleteHome", exception);
                return false;
            }
        }
    }

    // ==================================================
    // KINGDOM DESCRIPTION
    // ==================================================

    public boolean setDescription(UUID player, String description) {
        if (player == null || description == null) {
            return false;
        }

        String value = description.trim();

        if (value.length() > 200) {
            return false;
        }

        Optional<Kingdom> optional =
                plugin.getKingdomManager().getKingdomByPlayer(player);

        if (optional.isEmpty()) {
            return false;
        }

        Connection connection = database.getConnection();

        String sql = """
                UPDATE kingdoms
                SET description = ?,
                    updated_at = ?
                WHERE id = ?
                """;

        synchronized (connection) {
            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(1, value);
                statement.setLong(2, System.currentTimeMillis());
                statement.setInt(3, optional.get().getId());

                return statement.executeUpdate() > 0;

            } catch (SQLException exception) {
                logError("setDescription", exception);
                return false;
            }
        }
    }

    // ==================================================
    // KINGDOM PVP
    // ==================================================

    public boolean setPvp(UUID player, boolean enabled) {
        if (player == null) {
            return false;
        }

        Optional<Kingdom> optional =
                plugin.getKingdomManager().getKingdomByPlayer(player);

        if (optional.isEmpty()) {
            return false;
        }

        Connection connection = database.getConnection();

        String sql = """
                UPDATE kingdoms
                SET pvp_enabled = ?,
                    updated_at = ?
                WHERE id = ?
                """;

        synchronized (connection) {
            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setInt(1, enabled ? 1 : 0);
                statement.setLong(2, System.currentTimeMillis());
                statement.setInt(3, optional.get().getId());

                return statement.executeUpdate() > 0;

            } catch (SQLException exception) {
                logError("setPvp", exception);
                return false;
            }
        }
    }

    public boolean isPvpEnabled(int kingdomId) {
        if (kingdomId <= 0) {
            return false;
        }

        Connection connection = database.getConnection();

        synchronized (connection) {
            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 """
                                 SELECT pvp_enabled
                                 FROM kingdoms
                                 WHERE id = ?
                                 LIMIT 1
                                 """
                         )) {

                statement.setInt(1, kingdomId);

                try (ResultSet result = statement.executeQuery()) {
                    return result.next()
                            && result.getInt("pvp_enabled") == 1;
                }

            } catch (SQLException exception) {
                logError("isPvpEnabled", exception);
                return false;
            }
        }
    }

    // ==================================================
    // KINGDOM CHAT
    // ==================================================

    public boolean setChatEnabled(UUID player, boolean enabled) {
        if (player == null) {
            return false;
        }

        Optional<Kingdom> optional =
                plugin.getKingdomManager().getKingdomByPlayer(player);

        if (optional.isEmpty()) {
            return false;
        }

        int kingdomId = optional.get().getId();
        Connection connection = database.getConnection();

        synchronized (connection) {
            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 """
                                 INSERT INTO kingdom_settings
                                     (kingdom_id, chat_enabled)
                                 VALUES (?, ?)
                                 ON CONFLICT(kingdom_id)
                                 DO UPDATE SET
                                     chat_enabled = excluded.chat_enabled
                                 """
                         )) {

                statement.setInt(1, kingdomId);
                statement.setInt(2, enabled ? 1 : 0);

                return statement.executeUpdate() > 0;

            } catch (SQLException exception) {
                logError("setChatEnabled", exception);
                return false;
            }
        }
    }

    public boolean isChatEnabled(int kingdomId) {
        if (kingdomId <= 0) {
            return true;
        }

        Connection connection = database.getConnection();

        synchronized (connection) {
            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 """
                                 SELECT chat_enabled
                                 FROM kingdom_settings
                                 WHERE kingdom_id = ?
                                 LIMIT 1
                                 """
                         )) {

                statement.setInt(1, kingdomId);

                try (ResultSet result = statement.executeQuery()) {
                    // Default: Kingdom Chat aktif.
                    return !result.next()
                            || result.getInt("chat_enabled") == 1;
                }

            } catch (SQLException exception) {
                logError("isChatEnabled", exception);
                return true;
            }
        }
    }

    // ==================================================
    // DATA
    // ==================================================

    public record HomeData(
            String world,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
    }

    // ==================================================
    // LOGGING
    // ==================================================

    private void logError(String method, Exception exception) {
        plugin.getLogger().severe(
                "KingdomFeatureManager." + method
                        + ": " + exception.getMessage()
        );
    }
}
