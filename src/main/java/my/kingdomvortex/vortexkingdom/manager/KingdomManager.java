
package my.kingdomvortex.vortexkingdom.manager;

import my.kingdomvortex.vortexkingdom.VortexKingdom;
import my.kingdomvortex.vortexkingdom.database.DatabaseManager;
import my.kingdomvortex.vortexkingdom.kingdom.Kingdom;
import my.kingdomvortex.vortexkingdom.kingdom.KingdomMember;
import my.kingdomvortex.vortexkingdom.kingdom.KingdomMember.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

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

    // ==================================================
    // CREATE KINGDOM
    // ==================================================

    public synchronized boolean createKingdom(
            UUID owner,
            String ownerName,
            String kingdomName
    ) {
        if (owner == null
                || ownerName == null
                || ownerName.isBlank()
                || !isValidName(kingdomName)) {
            return false;
        }

        Connection connection = database.getConnection();

        synchronized (connection) {
            boolean previousAutoCommit = true;

            try {
                previousAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);

                if (getMember(owner).isPresent()) {
                    connection.rollback();
                    return false;
                }

                String name = kingdomName.trim();
                long now = System.currentTimeMillis();

                int startingLevel = Math.max(
                        1,
                        plugin.getConfig().getInt(
                                "kingdom.starting-level", 1
                        )
                );

                long startingExp = Math.max(
                        0,
                        plugin.getConfig().getLong(
                                "kingdom.starting-exp", 0
                        )
                );

                int kingdomId;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     """
                                     INSERT INTO kingdoms
                                     (
                                         name,
                                         owner_uuid,
                                         level,
                                         experience,
                                         total_experience,
                                         total_kills,
                                         description,
                                         pvp_enabled,
                                         created_at,
                                         updated_at
                                     )
                                     VALUES (?, ?, ?, ?, 0, 0, '', 0, ?, ?)
                                     """,
                                     Statement.RETURN_GENERATED_KEYS
                             )) {

                    statement.setString(1, name);
                    statement.setString(2, owner.toString());
                    statement.setInt(3, startingLevel);
                    statement.setLong(4, startingExp);
                    statement.setLong(5, now);
                    statement.setLong(6, now);

                    statement.executeUpdate();

                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException(
                                    "Kingdom ID generation failed."
                            );
                        }

                        kingdomId = keys.getInt(1);
                    }
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     """
                                     INSERT INTO kingdom_members
                                     (
                                         kingdom_id,
                                         player_uuid,
                                         player_name,
                                         role,
                                         joined_at
                                     )
                                     VALUES (?, ?, ?, 'OWNER', ?)
                                     """
                             )) {

                    statement.setInt(1, kingdomId);
                    statement.setString(2, owner.toString());
                    statement.setString(3, ownerName);
                    statement.setLong(4, now);
                    statement.executeUpdate();
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     """
                                     INSERT INTO kingdom_statistics
                                     (kingdom_id)
                                     VALUES (?)
                                     """
                             )) {

                    statement.setInt(1, kingdomId);
                    statement.executeUpdate();
                }

                connection.commit();
                return true;

            } catch (Exception exception) {
                rollback(connection);
                logError("createKingdom", exception);
                return false;

            } finally {
                restoreAutoCommit(connection, previousAutoCommit);
            }
        }
    }

    // ==================================================
    // KINGDOM LOOKUP
    // ==================================================

    public synchronized Optional<Kingdom> getKingdomByPlayer(
            UUID player
    ) {
        if (player == null) {
            return Optional.empty();
        }

        String sql = """
                SELECT k.*
                FROM kingdoms k
                INNER JOIN kingdom_members m
                    ON k.id = m.kingdom_id
                WHERE m.player_uuid = ?
                LIMIT 1
                """;

        Connection connection = database.getConnection();

        synchronized (connection) {
            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(1, player.toString());

                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        return Optional.of(mapKingdom(result));
                    }
                }

            } catch (SQLException exception) {
                logError("getKingdomByPlayer", exception);
            }
        }

        return Optional.empty();
    }

    public synchronized Optional<Kingdom> getKingdomByName(
            String name
    ) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        Connection connection = database.getConnection();

        synchronized (connection) {
            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 """
                                 SELECT *
                                 FROM kingdoms
                                 WHERE name = ? COLLATE NOCASE
                                 LIMIT 1
                                 """
                         )) {

                statement.setString(1, name.trim());

                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        return Optional.of(mapKingdom(result));
                    }
                }

            } catch (SQLException exception) {
                logError("getKingdomByName", exception);
            }
        }

        return Optional.empty();
    }

    public synchronized Optional<Kingdom> getKingdomById(int id) {
        if (id <= 0) {
            return Optional.empty();
        }

        Connection connection = database.getConnection();

        synchronized (connection) {
            try (PreparedStatement statement =
                         connection.prepareStatement(
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
        }

        return Optional.empty();
    }

    // ==================================================
    // MEMBER LOOKUP
    // ==================================================

    public synchronized Optional<KingdomMember> getMember(UUID player) {
        if (player == null) {
            return Optional.empty();
        }

        Connection connection = database.getConnection();

        synchronized (connection) {
            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 """
                                 SELECT *
                                 FROM kingdom_members
                                 WHERE player_uuid = ?
                                 LIMIT 1
                                 """
                         )) {

                statement.setString(1, player.toString());

                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        return Optional.of(mapMember(result));
                    }
                }

            } catch (SQLException exception) {
                logError("getMember", exception);
            }
        }

        return Optional.empty();
    }

    public synchronized List<KingdomMember> getMembers(int kingdomId) {
        List<KingdomMember> members = new ArrayList<>();

        if (kingdomId <= 0) {
            return members;
        }

        String sql = """
                SELECT *
                FROM kingdom_members
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

        Connection connection = database.getConnection();

        synchronized (connection) {
            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setInt(1, kingdomId);

                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        members.add(mapMember(result));
                    }
                }

            } catch (SQLException exception) {
                logError("getMembers", exception);
            }
        }

        return members;
    }

    public synchronized int getMemberCount(int kingdomId) {
        if (kingdomId <= 0) {
            return 0;
        }

        Connection connection = database.getConnection();

        synchronized (connection) {
            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 """
                                 SELECT COUNT(*)
                                 FROM kingdom_members
                                 WHERE kingdom_id = ?
                                 """
                         )) {

                statement.setInt(1, kingdomId);

                try (ResultSet result = statement.executeQuery()) {
                    return result.next() ? result.getInt(1) : 0;
                }

            } catch (SQLException exception) {
                logError("getMemberCount", exception);
                return 0;
            }
        }
    }

    /**
     * Capacity formula:
     * Starting capacity + (level - 1) * capacity per level.
     */
    public int getMaxMembers(int level) {
        int starting = Math.max(
                1,
                plugin.getConfig().getInt(
                        "kingdom.starting-capacity", 5
                )
        );

        int perLevel = Math.max(
                0,
                plugin.getConfig().getInt(
                        "kingdom.capacity-per-level", 1
                )
        );

        long capacity = starting
                + (long) Math.max(0, level - 1) * perLevel;

        return (int) Math.min(Integer.MAX_VALUE, capacity);
    }

    // ==================================================
    // INVITATIONS
    // ==================================================

    public synchronized boolean invitePlayer(
            UUID inviter,
            UUID target,
            String targetName
    ) {
        if (inviter == null
                || target == null
                || inviter.equals(target)
                || targetName == null
                || targetName.isBlank()) {
            return false;
        }

        Connection connection = database.getConnection();

        synchronized (connection) {
            boolean previousAutoCommit = true;

            try {
                Optional<Kingdom> optional =
                        getKingdomByPlayer(inviter);

                if (optional.isEmpty()) {
                    return false;
                }

                Kingdom kingdom = optional.get();

                Optional<KingdomMember> inviterMember =
                        getMember(inviter);

                if (inviterMember.isEmpty()
                        || !canInvite(inviterMember.get().getRole())) {
                    return false;
                }

                if (getMember(target).isPresent()) {
                    return false;
                }

                previousAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);

                long now = System.currentTimeMillis();

                try (PreparedStatement cleanup =
                             connection.prepareStatement(
                                     """
                                     DELETE FROM kingdom_invitations
                                     WHERE expires_at <= ?
                                     """
                             )) {

                    cleanup.setLong(1, now);
                    cleanup.executeUpdate();
                }

                if (getMemberCount(kingdom.getId())
                        >= getMaxMembers(kingdom.getLevel())) {
                    connection.rollback();
                    return false;
                }

                int maxPending = Math.max(
                        1,
                        plugin.getConfig().getInt(
                                "invitation.maximum-pending-invitations",
                                10
                        )
                );

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     """
                                     SELECT COUNT(*)
                                     FROM kingdom_invitations
                                     WHERE kingdom_id = ?
                                       AND inviter_uuid = ?
                                       AND expires_at > ?
                                     """
                             )) {

                    statement.setInt(1, kingdom.getId());
                    statement.setString(2, inviter.toString());
                    statement.setLong(3, now);

                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()
                                && result.getInt(1) >= maxPending) {
                            connection.rollback();
                            return false;
                        }
                    }
                }

                // A player may have only one active invitation at a time.
                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     """
                                     SELECT id
                                     FROM kingdom_invitations
                                     WHERE target_uuid = ?
                                       AND expires_at > ?
                                       AND kingdom_id != ?
                                     LIMIT 1
                                     """
                             )) {

                    statement.setString(1, target.toString());
                    statement.setLong(2, now);
                    statement.setInt(3, kingdom.getId());

                    try (ResultSet result = statement.executeQuery()) {
                        if (result.next()) {
                            connection.rollback();
                            return false;
                        }
                    }
                }

                int expiration = Math.max(
                        1,
                        plugin.getConfig().getInt(
                                "invitation.expiration-seconds", 120
                        )
                );

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     """
                                     INSERT INTO kingdom_invitations
                                     (
                                         kingdom_id,
                                         inviter_uuid,
                                         target_uuid,
                                         created_at,
                                         expires_at
                                     )
                                     VALUES (?, ?, ?, ?, ?)
                                     ON CONFLICT(kingdom_id, target_uuid)
                                     DO UPDATE SET
                                         inviter_uuid = excluded.inviter_uuid,
                                         created_at = excluded.created_at,
                                         expires_at = excluded.expires_at
                                     """
                             )) {

                    statement.setInt(1, kingdom.getId());
                    statement.setString(2, inviter.toString());
                    statement.setString(3, target.toString());
                    statement.setLong(4, now);
                    statement.setLong(
                            5,
                            now + expiration * 1000L
                    );

                    statement.executeUpdate();
                }

                connection.commit();
                return true;

            } catch (Exception exception) {
                rollback(connection);
                logError("invitePlayer", exception);
                return false;

            } finally {
                restoreAutoCommit(connection, previousAutoCommit);
            }
        }
    }

    public synchronized List<Invitation> getInvitations(UUID player) {
        List<Invitation> invitations = new ArrayList<>();

        if (player == null) {
            return invitations;
        }

        String sql = """
                SELECT
                    i.*,
                    k.name AS kingdom_name
                FROM kingdom_invitations i
                INNER JOIN kingdoms k
                    ON i.kingdom_id = k.id
                WHERE i.target_uuid = ?
                  AND i.expires_at > ?
                ORDER BY i.created_at DESC
                """;

        Connection connection = database.getConnection();

        synchronized (connection) {
            try (PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(1, player.toString());
                statement.setLong(2, System.currentTimeMillis());

                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        invitations.add(new Invitation(
                                result.getInt("id"),
                                result.getInt("kingdom_id"),
                                result.getString("kingdom_name"),
                                result.getString("inviter_uuid"),
                                result.getLong("created_at"),
                                result.getLong("expires_at")
                        ));
                    }
                }

            } catch (SQLException exception) {
                logError("getInvitations", exception);
            }
        }

        return invitations;
    }

    public synchronized boolean acceptInvitation(
            UUID player,
            int invitationId
    ) {
        if (player == null || invitationId <= 0) {
            return false;
        }

        Connection connection = database.getConnection();

        synchronized (connection) {
            boolean previousAutoCommit = true;

            try {
                previousAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);

                if (getMember(player).isPresent()) {
                    connection.rollback();
                    return false;
                }

                int kingdomId;
                int level;
                long expiresAt;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     """
                                     SELECT
                                         i.kingdom_id,
                                         i.expires_at,
                                         k.level
                                     FROM kingdom_invitations i
                                     INNER JOIN kingdoms k
                                         ON k.id = i.kingdom_id
                                     WHERE i.id = ?
                                       AND i.target_uuid = ?
                                     """
                             )) {

                    statement.setInt(1, invitationId);
                    statement.setString(2, player.toString());

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

                if (System.currentTimeMillis() >= expiresAt) {
                    deleteInvitation(connection, invitationId, player);
                    connection.commit();
                    return false;
                }

                if (getMemberCount(kingdomId) >= getMaxMembers(level)) {
                    connection.rollback();
                    return false;
                }

                String playerName = plugin.getServer()
                        .getOfflinePlayer(player)
                        .getName();

                if (playerName == null || playerName.isBlank()) {
                    playerName = "Unknown";
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     """
                                     INSERT INTO kingdom_members
                                     (
                                         kingdom_id,
                                         player_uuid,
                                         player_name,
                                         role,
                                         joined_at
                                     )
                                     VALUES (?, ?, ?, 'MEMBER', ?)
                                     """
                             )) {

                    statement.setInt(1, kingdomId);
                    statement.setString(2, player.toString());
                    statement.setString(3, playerName);
                    statement.setLong(
                            4,
                            System.currentTimeMillis()
                    );

                    statement.executeUpdate();
                }

                deleteInvitations(connection, player);

                connection.commit();
                return true;

            } catch (Exception exception) {
                rollback(connection);
                logError("acceptInvitation", exception);
                return false;

            } finally {
                restoreAutoCommit(connection, previousAutoCommit);
            }
        }
    }

    public synchronized boolean acceptInvitation(UUID player) {
        List<Invitation> invitations = getInvitations(player);

        return !invitations.isEmpty()
                && acceptInvitation(player, invitations.get(0).id());
    }

    public synchronized boolean denyInvitation(
            UUID player,
            int invitationId
    ) {
        if (player == null || invitationId <= 0) {
            return false;
        }

        Connection connection = database.getConnection();

        synchronized (connection) {
            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 """
                                 DELETE FROM kingdom_invitations
                                 WHERE id = ?
                                   AND target_uuid = ?
                                 """
                         )) {

                statement.setInt(1, invitationId);
                statement.setString(2, player.toString());

                return statement.executeUpdate() > 0;

            } catch (SQLException exception) {
                logError("denyInvitation", exception);
                return false;
            }
        }
    }

    public synchronized boolean denyInvitation(UUID player) {
        List<Invitation> invitations = getInvitations(player);

        return !invitations.isEmpty()
                && denyInvitation(player, invitations.get(0).id());
    }

    // ==================================================
    // KICK MEMBER
    // ==================================================

    public synchronized boolean kickMember(
            UUID actor,
            UUID target
    ) {
        if (actor == null
                || target == null
                || actor.equals(target)) {
            return false;
        }

        Connection connection = database.getConnection();

        synchronized (connection) {
            Optional<KingdomMember> actorMember = getMember(actor);
            Optional<KingdomMember> targetMember = getMember(target);

            if (actorMember.isEmpty() || targetMember.isEmpty()) {
                return false;
            }

            KingdomMember actorData = actorMember.get();
            KingdomMember targetData = targetMember.get();

            if (actorData.getKingdomId()
                    != targetData.getKingdomId()) {
                return false;
            }

            if (!canManageRole(
                    actorData.getRole(),
                    targetData.getRole()
            )) {
                return false;
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 """
                                 DELETE FROM kingdom_members
                                 WHERE kingdom_id = ?
                                   AND player_uuid = ?
                                 """
                         )) {

                statement.setInt(1, actorData.getKingdomId());
                statement.setString(2, target.toString());

                return statement.executeUpdate() > 0;

            } catch (SQLException exception) {
                logError("kickMember", exception);
                return false;
            }
        }
    }

    // ==================================================
    // ROLE MANAGEMENT
    // ==================================================

    public synchronized boolean setRole(
            UUID actor,
            UUID target,
            Role newRole
    ) {
        if (actor == null
                || target == null
                || actor.equals(target)
                || newRole == null
                || newRole == Role.OWNER) {
            return false;
        }

        Connection connection = database.getConnection();

        synchronized (connection) {
            Optional<KingdomMember> actorMember = getMember(actor);
            Optional<KingdomMember> targetMember = getMember(target);

            if (actorMember.isEmpty() || targetMember.isEmpty()) {
                return false;
            }

            KingdomMember actorData = actorMember.get();
            KingdomMember targetData = targetMember.get();

            if (actorData.getKingdomId()
                    != targetData.getKingdomId()) {
                return false;
            }

            Role actorRole = actorData.getRole();
            Role targetRole = targetData.getRole();

            if (!canChangeRole(actorRole, targetRole, newRole)) {
                return false;
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 """
                                 UPDATE kingdom_members
                                 SET role = ?
                                 WHERE kingdom_id = ?
                                   AND player_uuid = ?
                                 """
                         )) {

                statement.setString(1, newRole.name());
                statement.setInt(2, actorData.getKingdomId());
                statement.setString(3, target.toString());

                return statement.executeUpdate() > 0;

            } catch (SQLException exception) {
                logError("setRole", exception);
                return false;
            }
        }
    }

    public synchronized boolean promoteMember(
            UUID actor,
            UUID target
    ) {
        if (actor == null || target == null) {
            return false;
        }

        Optional<KingdomMember> actorMember = getMember(actor);
        Optional<KingdomMember> targetMember = getMember(target);

        if (actorMember.isEmpty() || targetMember.isEmpty()) {
            return false;
        }

        Role nextRole;

        if (actorMember.get().getRole() == Role.LEADER) {
            nextRole = targetMember.get().getRole() == Role.MEMBER
                    ? Role.OFFICER
                    : null;
        } else if (actorMember.get().getRole() == Role.OWNER) {
            nextRole = switch (targetMember.get().getRole()) {
                case MEMBER -> Role.OFFICER;
                case OFFICER -> Role.LEADER;
                default -> null;
            };
        } else {
            return false;
        }

        return nextRole != null
                && setRole(actor, target, nextRole);
    }

    public synchronized boolean demoteMember(
            UUID actor,
            UUID target
    ) {
        if (actor == null || target == null) {
            return false;
        }

        Optional<KingdomMember> targetMember = getMember(target);

        if (targetMember.isEmpty()) {
            return false;
        }

        Role nextRole = switch (targetMember.get().getRole()) {
            case LEADER -> Role.OFFICER;
            case OFFICER -> Role.MEMBER;
            default -> null;
        };

        return nextRole != null
                && setRole(actor, target, nextRole);
    }

    // ==================================================
    // TRANSFER OWNERSHIP
    // ==================================================

    public synchronized boolean transferOwnership(
            UUID owner,
            UUID target
    ) {
        if (owner == null
                || target == null
                || owner.equals(target)) {
            return false;
        }

        Connection connection = database.getConnection();

        synchronized (connection) {
            boolean previousAutoCommit = true;

            try {
                previousAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);

                Optional<Kingdom> optional =
                        getKingdomByPlayer(owner);

                if (optional.isEmpty()) {
                    connection.rollback();
                    return false;
                }

                Kingdom kingdom = optional.get();

                if (!kingdom.getOwnerUuid().equals(owner.toString())) {
                    connection.rollback();
                    return false;
                }

                Optional<KingdomMember> targetMember = getMember(target);

                if (targetMember.isEmpty()
                        || targetMember.get().getKingdomId()
                        != kingdom.getId()) {
                    connection.rollback();
                    return false;
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     """
                                     UPDATE kingdom_members
                                     SET role = 'LEADER'
                                     WHERE kingdom_id = ?
                                       AND player_uuid = ?
                                       AND role = 'OWNER'
                                     """
                             )) {

                    statement.setInt(1, kingdom.getId());
                    statement.setString(2, owner.toString());

                    if (statement.executeUpdate() != 1) {
                        throw new SQLException(
                                "Current owner member not found."
                        );
                    }
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     """
                                     UPDATE kingdom_members
                                     SET role = 'OWNER'
                                     WHERE kingdom_id = ?
                                       AND player_uuid = ?
                                     """
                             )) {

                    statement.setInt(1, kingdom.getId());
                    statement.setString(2, target.toString());

                    if (statement.executeUpdate() != 1) {
                        throw new SQLException(
                                "Target member not found."
                        );
                    }
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     """
                                     UPDATE kingdoms
                                     SET owner_uuid = ?,
                                         updated_at = ?
                                     WHERE id = ?
                                     """
                             )) {

                    statement.setString(1, target.toString());
                    statement.setLong(
                            2,
                            System.currentTimeMillis()
                    );
                    statement.setInt(3, kingdom.getId());

                    if (statement.executeUpdate() != 1) {
                        throw new SQLException(
                                "Kingdom owner update failed."
                        );
                    }
                }

                connection.commit();
                return true;

            } catch (Exception exception) {
                rollback(connection);
                logError("transferOwnership", exception);
                return false;

            } finally {
                restoreAutoCommit(connection, previousAutoCommit);
            }
        }
    }

    // ==================================================
    // LEAVE KINGDOM
    // ==================================================

    public synchronized boolean leaveKingdom(UUID player) {
        if (player == null) {
            return false;
        }

        Connection connection = database.getConnection();

        synchronized (connection) {
            Optional<KingdomMember> member = getMember(player);

            if (member.isEmpty() || member.get().isOwner()) {
                return false;
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 """
                                 DELETE FROM kingdom_members
                                 WHERE player_uuid = ?
                                   AND role != 'OWNER'
                                 """
                         )) {

                statement.setString(1, player.toString());

                return statement.executeUpdate() > 0;

            } catch (SQLException exception) {
                logError("leaveKingdom", exception);
                return false;
            }
        }
    }

    // ==================================================
    // DELETE KINGDOM
    // ==================================================

    public synchronized boolean deleteKingdom(UUID owner) {
        if (owner == null) {
            return false;
        }

        Connection connection = database.getConnection();

        synchronized (connection) {
            Optional<Kingdom> optional =
                    getKingdomByPlayer(owner);

            if (optional.isEmpty()
                    || !optional.get().getOwnerUuid()
                    .equals(owner.toString())) {
                return false;
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 """
                                 DELETE FROM kingdoms
                                 WHERE id = ?
                                   AND owner_uuid = ?
                                 """
                         )) {

                statement.setInt(1, optional.get().getId());
                statement.setString(2, owner.toString());

                return statement.executeUpdate() > 0;

            } catch (SQLException exception) {
                logError("deleteKingdom", exception);
                return false;
            }
        }
    }

    // ==================================================
    // ROLE PERMISSIONS
    // ==================================================

    private boolean canInvite(Role role) {
        return role == Role.OWNER
                || role == Role.LEADER
                || role == Role.OFFICER;
    }

    private boolean canManageRole(
            Role actor,
            Role target
    ) {
        return switch (actor) {
            case OWNER -> target != Role.OWNER;
            case LEADER -> target == Role.OFFICER
                    || target == Role.MEMBER;
            case OFFICER -> target == Role.MEMBER;
            default -> false;
        };
    }

    private boolean canChangeRole(
            Role actor,
            Role target,
            Role newRole
    ) {
        if (target == Role.OWNER) {
            return false;
        }

        if (actor == Role.OWNER) {
            return switch (target) {
                case MEMBER -> newRole == Role.OFFICER;
                case OFFICER -> newRole == Role.MEMBER
                        || newRole == Role.LEADER;
                case LEADER -> newRole == Role.OFFICER;
                default -> false;
            };
        }

        if (actor == Role.LEADER) {
            return target == Role.MEMBER
                    && newRole == Role.OFFICER;
        }

        return false;
    }

    // ==================================================
    // INTERNAL HELPERS
    // ==================================================

    private void deleteInvitation(
            Connection connection,
            int invitationId,
            UUID player
    ) throws SQLException {
        try (PreparedStatement statement =
                     connection.prepareStatement(
                             """
                             DELETE FROM kingdom_invitations
                             WHERE id = ?
                               AND target_uuid = ?
                             """
                     )) {

            statement.setInt(1, invitationId);
            statement.setString(2, player.toString());
            statement.executeUpdate();
        }
    }

    private void deleteInvitations(
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

        int min = Math.max(
                1,
                plugin.getConfig().getInt(
                        "kingdom.minimum-name-length", 3
                )
        );

        int max = Math.max(
                min,
                plugin.getConfig().getInt(
                        "kingdom.maximum-name-length", 16
                )
        );

        String value = name.trim();

        return value.length() >= min
                && value.length() <= max
                && value.matches("[a-zA-Z0-9_]+");
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

    private void rollback(Connection connection) {
        try {
            if (connection != null
                    && !connection.isClosed()
                    && !connection.getAutoCommit()) {
                connection.rollback();
            }
        } catch (SQLException exception) {
            logError("rollback", exception);
        }
    }

    private void restoreAutoCommit(
            Connection connection,
            boolean previousAutoCommit
    ) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException exception) {
            logError("restoreAutoCommit", exception);
        }
    }

    private void logError(
            String method,
            Exception exception
    ) {
        plugin.getLogger().log(
                Level.SEVERE,
                "KingdomManager." + method + " failed.",
                exception
        );
    }

    // ==================================================
    // INVITATION DATA
    // ==================================================

    public record Invitation(
            int id,
            int kingdomId,
            String kingdomName,
            String inviterUuid,
            long createdAt,
            long expiresAt
    ) {
    }
}
