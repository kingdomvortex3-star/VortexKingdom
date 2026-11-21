
package my.kingdomvortex.vortexkingdom.manager;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.database.DatabaseManager;
import my.kingdomvortex.vortexkingdom.kingdom.Kingdom;
import my.kingdomvortex.vortexkingdom.kingdom.KingdomMember;
import my.kingdomvortex.vortexkingdom.kingdom.KingdomMember.Role;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class KingdomManager {

    private final VortexKingdom plugin;
    private final DatabaseManager database;

    public KingdomManager(
            VortexKingdom plugin,
            DatabaseManager database
    ) {
        this.plugin = plugin;
        this.database = database;
    }

    public synchronized boolean createKingdom(
            UUID owner,
            String ownerName,
            String kingdomName
    ) {
        if (!isValidName(kingdomName)) {
            return false;
        }

        if (getKingdomByPlayer(owner).isPresent()) {
            return false;
        }

        String name = kingdomName.trim();

        try {
            Connection connection = database.getConnection();

            connection.setAutoCommit(false);

            try {
                try (PreparedStatement check = connection.prepareStatement(
                        "SELECT id FROM kingdoms WHERE name = ?"
                )) {
                    check.setString(1, name);

                    try (ResultSet result = check.executeQuery()) {
                        if (result.next()) {
                            connection.rollback();
                            return false;
                        }
                    }
                }

                long now = System.currentTimeMillis();

                int kingdomId;

                try (PreparedStatement insert = connection.prepareStatement(
                        """
                        INSERT INTO kingdoms
                        (name, owner_uuid, level, experience,
                         total_experience, created_at)
                        VALUES (?, ?, 1, 0, 0, ?)
                        """,
                        Statement.RETURN_GENERATED_KEYS
                )) {
                    insert.setString(1, name);
                    insert.setString(2, owner.toString());
                    insert.setLong(3, now);
                    insert.executeUpdate();

                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException(
                                    "Failed to generate kingdom ID."
                            );
                        }

                        kingdomId = keys.getInt(1);
                    }
                }

                try (PreparedStatement member = connection.prepareStatement(
                        """
                        INSERT INTO kingdom_members
                        (kingdom_id, player_uuid, player_name, role, joined_at)
                        VALUES (?, ?, ?, 'OWNER', ?)
                        """
                )) {
                    member.setInt(1, kingdomId);
                    member.setString(2, owner.toString());
                    member.setString(3, ownerName);
                    member.setLong(4, now);
                    member.executeUpdate();
                }

                connection.commit();
                return true;

            } catch (Exception exception) {
                connection.rollback();
                throw exception;

            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Failed to create kingdom: " + exception.getMessage()
            );
            return false;
        }
    }

    public synchronized Optional<Kingdom> getKingdomByPlayer(
            UUID player
    ) {
        String sql = """
                SELECT k.*
                FROM kingdoms k
                INNER JOIN kingdom_members m
                ON k.id = m.kingdom_id
                WHERE m.player_uuid = ?
                """;

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

            statement.setString(1, player.toString());

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapKingdom(result));
                }
            }

        } catch (SQLException exception) {
            logError("getKingdomByPlayer", exception);
        }

        return Optional.empty();
    }

    public synchronized Optional<Kingdom> getKingdomByName(
            String name
    ) {
        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(
                             "SELECT * FROM kingdoms WHERE name = ?"
                     )) {

            statement.setString(1, name);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapKingdom(result));
                }
            }

        } catch (SQLException exception) {
            logError("getKingdomByName", exception);
        }

        return Optional.empty();
    }

    public synchronized Optional<Kingdom> getKingdomById(
            int id
    ) {
        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(
                             "SELECT * FROM kingdoms WHERE id = ?"
                     )) {

            statement.setInt(1, id);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapKingdom(result));
                }
            }

        } catch (SQLException exception) {
            logError("getKingdomById", exception);
        }

        return Optional.empty();
    }

    public synchronized Optional<KingdomMember> getMember(
            UUID player
    ) {
        String sql = """
                SELECT * FROM kingdom_members
                WHERE player_uuid = ?
                """;

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

            statement.setString(1, player.toString());

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapMember(result));
                }
            }

        } catch (SQLException exception) {
            logError("getMember", exception);
        }

        return Optional.empty();
    }

    public synchronized List<KingdomMember> getMembers(
            int kingdomId
    ) {
        List<KingdomMember> members = new ArrayList<>();

        String sql = """
                SELECT * FROM kingdom_members
                WHERE kingdom_id = ?
                ORDER BY
                    CASE role
                        WHEN 'OWNER' THEN 1
                        WHEN 'LEADER' THEN 2
                        WHEN 'OFFICER' THEN 3
                        ELSE 4
                    END,
                    joined_at ASC
                """;

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

            statement.setInt(1, kingdomId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    members.add(mapMember(result));
                }
            }

        } catch (SQLException exception) {
            logError("getMembers", exception);
        }

        return members;
    }

    public synchronized int getMemberCount(int kingdomId) {
        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(
                             """
                             SELECT COUNT(*) FROM kingdom_members
                             WHERE kingdom_id = ?
                             """
                     )) {

            statement.setInt(1, kingdomId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt(1);
                }
            }

        } catch (SQLException exception) {
            logError("getMemberCount", exception);
        }

        return 0;
    }

    public synchronized int getMaxMembers(int level) {
        int startingCapacity = plugin.getConfig().getInt(
                "kingdom.starting-capacity", 5
        );

        int perLevel = plugin.getConfig().getInt(
                "kingdom.capacity-per-level", 1
        );

        return startingCapacity +
                Math.max(0, level - 1) * perLevel;
    }

    public synchronized boolean invitePlayer(
            UUID inviter,
            UUID target,
            String targetName
    ) {
        if (inviter.equals(target)) {
            return false;
        }

        Optional<Kingdom> optional = getKingdomByPlayer(inviter);

        if (optional.isEmpty()) {
            return false;
        }

        Kingdom kingdom = optional.get();

        Optional<KingdomMember> inviterMember = getMember(inviter);

        if (inviterMember.isEmpty()) {
            return false;
        }

        Role role = inviterMember.get().getRole();

        if (role != Role.OWNER &&
                role != Role.LEADER &&
                role != Role.OFFICER) {
            return false;
        }

        if (getKingdomByPlayer(target).isPresent()) {
            return false;
        }

        if (getMemberCount(kingdom.getId()) >=
                getMaxMembers(kingdom.getLevel())) {
            return false;
        }

        int expiration = plugin.getConfig().getInt(
                "invitation.expiration-seconds", 120
        );

        long now = System.currentTimeMillis();
        long expiresAt = now + expiration * 1000L;

        String sql = """
                INSERT INTO kingdom_invitations
                (kingdom_id, inviter_uuid, target_uuid,
                 created_at, expires_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(kingdom_id, target_uuid)
                DO UPDATE SET
                    inviter_uuid = excluded.inviter_uuid,
                    created_at = excluded.created_at,
                    expires_at = excluded.expires_at
                """;

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

            statement.setInt(1, kingdom.getId());
            statement.setString(2, inviter.toString());
            statement.setString(3, target.toString());
            statement.setLong(4, now);
            statement.setLong(5, expiresAt);

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            logError("invitePlayer", exception);
            return false;
        }
    }

    public synchronized boolean acceptInvitation(UUID player) {
        String sql = """
                SELECT i.*, k.level
                FROM kingdom_invitations i
                JOIN kingdoms k ON i.kingdom_id = k.id
                WHERE i.target_uuid = ?
                ORDER BY i.created_at DESC
                LIMIT 1
                """;

        try {
            Connection connection = database.getConnection();

            connection.setAutoCommit(false);

            try {
                if (getKingdomByPlayer(player).isPresent()) {
                    connection.rollback();
                    return false;
                }

                int kingdomId;
                long expiresAt;
                int level;

                try (PreparedStatement statement =
                             connection.prepareStatement(sql)) {

                    statement.setString(1, player.toString());

                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            connection.rollback();
                            return false;
                        }

                        kingdomId = result.getInt("kingdom_id");
                        expiresAt = result.getLong("expires_at");
                        level = result.getInt("level");
                    }
                }

                if (System.currentTimeMillis() > expiresAt) {
                    deleteInvitation(connection, player);
                    connection.commit();
                    return false;
                }

                if (getMemberCount(kingdomId) >= getMaxMembers(level)) {
                    connection.rollback();
                    return false;
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     """
                                     INSERT INTO kingdom_members
                                     (kingdom_id, player_uuid, player_name,
                                      role, joined_at)
                                     VALUES (?, ?, ?, 'MEMBER', ?)
                                     """
                             )) {

                    statement.setInt(1, kingdomId);
                    statement.setString(2, player.toString());

                    String name = plugin.getServer()
                            .getOfflinePlayer(player)
                            .getName();

                    statement.setString(
                            3,
                            name == null ? "Unknown" : name
                    );

                    statement.setLong(
                            4,
                            System.currentTimeMillis()
                    );

                    statement.executeUpdate();
                }

                deleteInvitation(connection, player);

                connection.commit();
                return true;

            } catch (Exception exception) {
                connection.rollback();
                throw exception;

            } finally {
                connection.setAutoCommit(true);
            }

        } catch (Exception exception) {
            logError("acceptInvitation", exception);
            return false;
        }
    }

    public synchronized boolean denyInvitation(UUID player) {
        String sql = """
                DELETE FROM kingdom_invitations
                WHERE target_uuid = ?
                """;

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

            statement.setString(1, player.toString());

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            logError("denyInvitation", exception);
            return false;
        }
    }

    public synchronized boolean leaveKingdom(UUID player) {
        Optional<KingdomMember> optional = getMember(player);

        if (optional.isEmpty()) {
            return false;
        }

        KingdomMember member = optional.get();

        if (member.isOwner()) {
            return false;
        }

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(
                             """
                             DELETE FROM kingdom_members
                             WHERE player_uuid = ?
                             """
                     )) {

            statement.setString(1, player.toString());

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            logError("leaveKingdom", exception);
            return false;
        }
    }

    public synchronized boolean deleteKingdom(UUID owner) {
        Optional<Kingdom> optional = getKingdomByPlayer(owner);

        if (optional.isEmpty() ||
                !optional.get().getOwnerUuid().equals(owner.toString())) {
            return false;
        }

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(
                             "DELETE FROM kingdoms WHERE id = ?"
                     )) {

            statement.setInt(1, optional.get().getId());

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            logError("deleteKingdom", exception);
            return false;
        }
    }

    private void deleteInvitation(
            Connection connection,
            UUID player
    ) throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(
                             """
                             DELETE FROM kingdom_invitations
                             WHERE target_uuid = ?
                             """
                     )) {

            statement.setString(1, player.toString());
            statement.executeUpdate();
        }
    }

    private boolean isValidName(String name) {
        if (name == null) {
            return false;
        }

        int min = plugin.getConfig().getInt(
                "kingdom.minimum-name-length", 3
        );

        int max = plugin.getConfig().getInt(
                "kingdom.maximum-name-length", 16
        );

        String value = name.trim();

        return value.length() >= min &&
                value.length() <= max &&
                value.matches("[a-zA-Z0-9_]+");
    }

    private Kingdom mapKingdom(ResultSet result)
            throws SQLException {

        return new Kingdom(
                result.getInt("id"),
                result.getString("name"),
                result.getString("owner_uuid"),
                result.getInt("level"),
                result.getLong("experience"),
                result.getLong("total_experience"),
                result.getLong("total_kills"),
                result.getString("description"),
                result.getInt("pvp_enabled") == 1
        );
    }

    private KingdomMember mapMember(ResultSet result)
            throws SQLException {

        return new KingdomMember(
                result.getInt("kingdom_id"),
                result.getString("player_uuid"),
                result.getString("player_name"),
                Role.fromString(result.getString("role")),
                result.getLong("joined_at")
        );
    }

    private void logError(String method, Exception exception) {
        plugin.getLogger().severe(
                "KingdomManager." + method + ": " +
                        exception.getMessage()
        );
    }
}
