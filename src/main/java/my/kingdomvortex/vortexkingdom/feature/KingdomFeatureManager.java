package my.kingdomvortex.vortexkingdom.feature;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.database.DatabaseManager;
import my.kingdomvortex.vortexkingdom.kingdom.Kingdom;
import my.kingdomvortex.vortexkingdom.kingdom.KingdomMember;
import my.kingdomvortex.vortexkingdom.kingdom.KingdomMember.Role;
import my.kingdomvortex.vortexkingdom.manager.KingdomManager;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class KingdomFeatureManager {

    private final VortexKingdom plugin;
    private final DatabaseManager database;
    private final KingdomManager kingdoms;

    public KingdomFeatureManager(VortexKingdom plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabaseManager();
        this.kingdoms = plugin.getKingdomManager();

        createSettingsTable();
    }

    private void createSettingsTable() {
        Connection connection = database.getConnection();

        synchronized (connection) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS kingdom_settings (
                        kingdom_id INTEGER PRIMARY KEY,
                        chat_enabled INTEGER NOT NULL DEFAULT 1
                    )
                    """)) {
                statement.executeUpdate();
            } catch (SQLException exception) {
                throw new IllegalStateException(
                        "Failed to create kingdom_settings table.",
                        exception
                );
            }
        }
    }

    // ==================================================
    // PERMISSIONS
    // ==================================================

    public boolean canManage(UUID uuid) {
        Optional<KingdomMember> member = kingdoms.getMember(uuid);

        if (member.isEmpty()) {
            return false;
        }

        Role role = member.get().getRole();

        return role == Role.OWNER
                || role == Role.LEADER
                || role == Role.OFFICER;
    }

    // ==================================================
    // HOME
    // ==================================================

    public boolean setHome(Player player) {
        if (player == null || !canManage(player.getUniqueId())) {
            return false;
        }

        Optional<Kingdom> kingdom =
                kingdoms.getKingdomByPlayer(player.getUniqueId());

        if (kingdom.isEmpty()) {
            return false;
        }

        Location location = player.getLocation();

        if (location.getWorld() == null) {
            return false;
        }

        Connection connection = database.getConnection();

        synchronized (connection) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE kingdoms
                    SET home_world = ?,
                        home_x = ?,
                        home_y = ?,
                        home_z = ?,
                        home_yaw = ?,
                        home_pitch = ?,
                        updated_at = ?
                    WHERE id = ?
                    """)) {

                statement.setString(1, location.getWorld().getName());
                statement.setDouble(2, location.getX());
                statement.setDouble(3, location.getY());
                statement.setDouble(4, location.getZ());
                statement.setFloat(5, location.getYaw());
                statement.setFloat(6, location.getPitch());
                statement.setLong(7, System.currentTimeMillis());
                statement.setInt(8, kingdom.get().getId());

                return statement.executeUpdate() > 0;

            } catch (SQLException exception) {
                logError("setHome", exception);
                return false;
            }
        }
    }

    public Optional<Location> getHome(UUID playerUuid) {
        Optional<Kingdom> kingdom =
                kingdoms.getKingdomByPlayer(playerUuid);

        if (kingdom.isEmpty()) {
            return Optional.empty();
        }

        return getHome(kingdom.get().getId());
    }

    public Optional<Location> getHome(int kingdomId) {
        Connection connection = database.getConnection();

        synchronized (connection) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT home_world, home_x, home_y, home_z,
                           home_yaw, home_pitch
                    FROM kingdoms
                    WHERE id = ?
                    LIMIT 1
                    """)) {

                statement.setInt(1, kingdomId);

                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        return Optional.empty();
                    }

                    String worldName = result.getString("home_world");

                    if (worldName == null || worldName.isBlank()) {
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

                    World world = plugin.getServer().getWorld(worldName);

                    if (world == null) {
                        return Optional.empty();
                    }

                    return Optional.of(new Location(
                            world,
                            x,
                            y,
                            z,
                            result.getFloat("home_yaw"),
                            result.getFloat("home_pitch")
                    ));
                }

            } catch (SQLException exception) {
                logError("getHome", exception);
                return Optional.empty();
            }
        }
    }

    public boolean deleteHome(Player player) {
        if (player == null || !canManage(player.getUniqueId())) {
            return false;
        }

        Optional<Kingdom> kingdom =
                kingdoms.getKingdomByPlayer(player.getUniqueId());

        if (kingdom.isEmpty()) {
            return false;
        }

        Connection connection = database.getConnection();

        synchronized (connection) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE kingdoms
                    SET home_world = NULL,
                        home_x = NULL,
                        home_y = NULL,
                        home_z = NULL,
                        home_yaw = NULL,
                        home_pitch = NULL,
                        updated_at = ?
                    WHERE id = ?
                    """)) {

                statement.setLong(1, System.currentTimeMillis());
                statement.setInt(2, kingdom.get().getId());

                return statement.executeUpdate() > 0;

            } catch (SQLException exception) {
                logError("deleteHome", exception);
                return false;
            }
        }
    }

    // ==================================================
    // PVP
    // ==================================================

    public boolean setPvp(Player player, boolean enabled) {
        if (player == null || !canManage(player.getUniqueId())) {
            return false;
        }

        Optional<Kingdom> kingdom =
                kingdoms.getKingdomByPlayer(player.getUniqueId());

        if (kingdom.isEmpty()) {
            return false;
        }

        Connection connection = database.getConnection();

        synchronized (connection) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE kingdoms
                    SET pvp_enabled = ?, updated_at = ?
                    WHERE id = ?
                    """)) {

                statement.setInt(1, enabled ? 1 : 0);
                statement.setLong(2, System.currentTimeMillis());
                statement.setInt(3, kingdom.get().getId());

                return statement.executeUpdate() > 0;

            } catch (SQLException exception) {
                logError("setPvp", exception);
                return false;
            }
        }
    }

    public boolean isPvpEnabled(int kingdomId) {
        Connection connection = database.getConnection();

        synchronized (connection) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT pvp_enabled
                    FROM kingdoms
                    WHERE id = ?
                    LIMIT 1
                    """)) {

                statement.setInt(1, kingdomId);

                try (ResultSet result = statement.executeQuery()) {
                    return result.next() && result.getInt(1) == 1;
                }

            } catch (SQLException exception) {
                logError("isPvpEnabled", exception);
                return false;
            }
        }
    }

    // ==================================================
    // DESCRIPTION
    // ==================================================

    public boolean setDescription(Player player, String description) {
        if (player == null || description == null
                || !canManage(player.getUniqueId())) {
            return false;
        }

        String clean = description.trim();

        int max = Math.max(
                16,
                plugin.getConfig().getInt(
                        "kingdom.maximum-description-length",
                        120
                )
        );

        if (clean.length() > max) {
            return false;
        }

        Optional<Kingdom> kingdom =
                kingdoms.getKingdomByPlayer(player.getUniqueId());

        if (kingdom.isEmpty()) {
            return false;
        }

        Connection connection = database.getConnection();

        synchronized (connection) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE kingdoms
                    SET description = ?, updated_at = ?
                    WHERE id = ?
                    """)) {

                statement.setString(1, clean);
                statement.setLong(2, System.currentTimeMillis());
                statement.setInt(3, kingdom.get().getId());

                return statement.executeUpdate() > 0;

            } catch (SQLException exception) {
                logError("setDescription", exception);
                return false;
            }
        }
    }

    // ==================================================
    // KINGDOM CHAT
    // ==================================================

    public boolean setChatEnabled(Player player, boolean enabled) {
        if (player == null || !canManage(player.getUniqueId())) {
            return false;
        }

        Optional<Kingdom> kingdom =
                kingdoms.getKingdomByPlayer(player.getUniqueId());

        if (kingdom.isEmpty()) {
            return false;
        }

        Connection connection = database.getConnection();

        synchronized (connection) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO kingdom_settings (kingdom_id, chat_enabled)
                    VALUES (?, ?)
                    ON CONFLICT(kingdom_id)
                    DO UPDATE SET chat_enabled = excluded.chat_enabled
                    """)) {

                statement.setInt(1, kingdom.get().getId());
                statement.setInt(2, enabled ? 1 : 0);

                return statement.executeUpdate() > 0;

            } catch (SQLException exception) {
                logError("setChatEnabled", exception);
                return false;
            }
        }
    }

    public boolean isChatEnabled(int kingdomId) {
        Connection connection = database.getConnection();

        synchronized (connection) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT chat_enabled
                    FROM kingdom_settings
                    WHERE kingdom_id = ?
                    LIMIT 1
                    """)) {

                statement.setInt(1, kingdomId);

                try (ResultSet result = statement.executeQuery()) {
                    return !result.next() || result.getInt(1) == 1;
                }

            } catch (SQLException exception) {
                logError("isChatEnabled", exception);
                return true;
            }
        }
    }

    private void logError(String method, SQLException exception) {
        plugin.getLogger().severe(
                "KingdomFeatureManager." + method
                        + ": " + exception.getMessage()
        );
    }
                        }
