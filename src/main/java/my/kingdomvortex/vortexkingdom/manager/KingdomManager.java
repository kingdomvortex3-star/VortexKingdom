
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

    // ==========================================
    // CREATE KINGDOM
    // ==========================================

    public synchronized boolean createKingdom(
            UUID owner,
            String ownerName,
            String kingdomName
    ) {
        if (owner == null || ownerName == null ||
                !isValidName(kingdomName)) {
            return false;
        }

        if (getMember(owner).isPresent()) {
            return false;
        }

        Connection connection = database.getConnection();

        try {
            connection.setAutoCommit(false);

            try {
                try (PreparedStatement check =
                             connection.prepareStatement(
                                     "SELECT id FROM kingdoms WHERE name = ?"
                             )) {

                    check.setString(1, kingdomName.trim());

                    try (ResultSet result = check.executeQuery()) {
                        if (result.next()) {
                            connection.rollback();
                            return false;
                        }
                    }
                }

                long now = System.currentTimeMillis();
                int kingdomId;

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     """
                                     INSERT INTO kingdoms
                                     (name, owner_uuid, level, experience,
                                      total_experience, created_at, updated_at)
                                     VALUES (?, ?, 1, 0, 0, ?, ?)
                                     """,
                                     Statement.RETURN_GENERATED_KEYS
                             )) {

                    statement.setString(1, kingdomName.trim());
                    statement.setString(2, owner.toString());
                    statement.setLong(3, now);
                    statement.setLong(4, now);

                    statement.executeUpdate();

                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException(
                                    "Failed to generate kingdom ID."
                            );
                        }

                        kingdomId = keys.getInt(1);
                    }
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     """
                                     INSERT INTO kingdom_members
                                     (kingdom_id, player_uuid, player_name,
                                      role, joined_at)
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
                connection.rollback();
                throw exception;
            }

        } catch (Exception exception) {
            logError("createKingdom", exception);
            return false;

        } finally {
            restoreAutoCommit(connection);
        }
    }

    // ==========================================
    // KINGDOM LOOKUP
    // ==========================================

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
        if (name == null) {
            return Optional.empty();
        }

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(
                             "SELECT * FROM kingdoms WHERE name = ?"
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

        return Optional.empty();
    }

    public synchronized Optional<Kingdom> getKingdomById(int id) {
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

    // ==========================================
    // MEMBER LOOKUP
    // ==========================================

    public synchronized Optional<KingdomMember> getMember(
            UUID player
    ) {
        if (player == null) {
            return Optional.empty();
        }

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(
                             """
                             SELECT * FROM kingdom_members
                             WHERE player_uuid = ?
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
                return result.next() ? result.getInt(1) : 0;
            }

        } catch (SQLException exception) {
            logError("getMemberCount", exception);
            return 0;
        }
    }

    public synchronized int getMaxMembers(int level) {
        int starting = plugin.getConfig().getInt(
                "kingdom.starting-capacity", 5
        );

        int perLevel = plugin.getConfig().getInt(
                "kingdom.capacity-per-level", 1
        );

        return starting + Math.max(0, level - 1) * perLevel;
    }

    // ==========================================
    // INVITATION
    // ==========================================

    public synchronized boolean invitePlayer(
            UUID inviter,
            UUID target,
            String targetName
    ) {
        if (inviter == null || target == null ||
                inviter.equals(target)) {
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

        if (getMember(target).isPresent()) {
            return false;
        }

        if (getMemberCount(kingdom.getId()) >=
                getMaxMembers(kingdom.getLevel())) {
            return false;
        }

        int maxPending = plugin.getConfig().getInt(
                "invitation.maximum-pending-invitations", 10
        );

        Connection connection = database.getConnection();

        try {
            // Bersihkan undangan kedaluwarsa milik target.
            try (PreparedStatement cleanup =
                         connection.prepareStatement(
                                 """
                                 DELETE FROM kingdom_invitations
                                 WHERE target_uuid = ?
                                 AND expires_at <= ?
                                 """
                         )) {

                cleanup.setString(1, target.toString());
                cleanup.setLong(2, System.currentTimeMillis());
                cleanup.executeUpdate();
            }

            // Cegah target menerima undangan dari beberapa kingdom
            // sekaligus jika sistem hanya mengizinkan satu undangan aktif.
            try (PreparedStatement existing =
                         connection.prepareStatement(
                                 """
                                 SELECT id FROM kingdom_invitations
                                 WHERE target_uuid = ?
                                 AND expires_at > ?
                                 AND kingdom_id != ?
                                 LIMIT 1
                                 """
                         )) {

                existing.setString(1, target.toString());
                existing.setLong(2, System.currentTimeMillis());
                existing.setInt(3, kingdom.getId());

                try (ResultSet result = existing.executeQuery()) {
                    if (result.next()) {
                        return false;
                    }
                }
            }

            try (PreparedStatement count =
                         connection.prepareStatement(
                                 """
                                 SELECT COUNT(*) FROM kingdom_invitations
                                 WHERE kingdom_id = ?
                                 AND inviter_uuid = ?
                                 AND expires_at > ?
                                 """
                         )) {

                count.setInt(1, kingdom.getId());
                count.setString(2, inviter.toString());
                count.setLong(3, System.currentTimeMillis());

                try (ResultSet result = count.executeQuery()) {
                    if (result.next() &&
                            result.getInt(1) >= maxPending) {
                        return false;
                    }
                }
            }

            int expiration = plugin.getConfig().getInt(
                    "invitation.expiration-seconds", 120
            );

            long now = System.currentTimeMillis();

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 """
                                 INSERT INTO kingdom_invitations
                                 (kingdom_id, inviter_uuid, target_uuid,
                                  created_at, expires_at)
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
                statement.setLong(5, now + expiration * 1000L);

                return statement.executeUpdate() > 0;
            }

        } catch (SQLException exception) {
            logError("invitePlayer", exception);
            return false;
        }
    }

    public synchronized List<Invitation> getInvitations(UUID player) {
        List<Invitation> invitations = new ArrayList<>();

        if (player == null) {
            return invitations;
        }

        String sql = """
                SELECT i.*, k.name AS kingdom_name
                FROM kingdom_invitations i
                JOIN kingdoms k ON i.kingdom_id = k.id
                WHERE i.target_uuid = ?
                AND i.expires_at > ?
                ORDER BY i.created_at DESC
                """;

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(sql)) {

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

        return invitations;
    }

    // Terima undangan berdasarkan ID tertentu.
    public synchronized boolean acceptInvitation(
            UUID player,
            int invitationId
    ) {
        if (player == null || invitationId <= 0) {
            return false;
        }

        Connection connection = database.getConnection();

        try {
            connection.setAutoCommit(false);

            try {
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
                                     SELECT i.kingdom_id,
                                            i.expires_at,
                                            k.level
                                     FROM kingdom_invitations i
                                     JOIN kingdoms k ON k.id = i.kingdom_id
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

                // Hapus hanya undangan yang sudah kedaluwarsa.
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
                        .getOfflinePlayer(player).getName();

                if (playerName == null || playerName.isBlank()) {
                    playerName = "Unknown";
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
                    statement.setString(3, playerName);
                    statement.setLong(4, System.currentTimeMillis());

                    statement.executeUpdate();
                }

                // Setelah berhasil bergabung, hapus seluruh undangan
                // lain karena pemain hanya boleh memiliki satu Kingdom.
                deleteInvitations(connection, player);

                connection.commit();
                return true;

            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }

        } catch (Exception exception) {
            logError("acceptInvitation", exception);
            return false;

        } finally {
            restoreAutoCommit(connection);
        }
    }

    // Kompatibilitas untuk pemanggil lama.
    // Mengambil undangan aktif terbaru.
    public synchronized boolean acceptInvitation(UUID player) {
        List<Invitation> invitations = getInvitations(player);

        if (invitations.isEmpty()) {
            return false;
        }

        return acceptInvitation(player, invitations.get(0).id());
    }

    // Tolak hanya undangan berdasarkan ID.
    public synchronized boolean denyInvitation(
            UUID player,
            int invitationId
    ) {
        if (player == null || invitationId <= 0) {
            return false;
        }

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(
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

    // Kompatibilitas untuk pemanggil lama.
    // Menolak undangan aktif terbaru saja.
    public synchronized boolean denyInvitation(UUID player) {
        List<Invitation> invitations = getInvitations(player);

        if (invitations.isEmpty()) {
            return false;
        }

        return denyInvitation(player, invitations.get(0).id());
    }

    // ==========================================
    // KICK MEMBER
    // ==========================================

    public synchronized boolean kickMember(
            UUID actor,
            UUID target
    ) {
        if (actor == null || target == null ||
                actor.equals(target)) {
            return false;
        }

        Optional<Kingdom> kingdom = getKingdomByPlayer(actor);

        if (kingdom.isEmpty()) {
            return false;
        }

        Optional<KingdomMember> actorMember = getMember(actor);
        Optional<KingdomMember> targetMember = getMember(target);

        if (actorMember.isEmpty() || targetMember.isEmpty()) {
            return false;
        }

        KingdomMember actorData = actorMember.get();
        KingdomMember targetData = targetMember.get();

        if (actorData.getKingdomId() != targetData.getKingdomId()) {
            return false;
        }

        if (!canManageRole(actorData.getRole(), targetData.getRole())) {
            return false;
        }

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(
                             """
                             DELETE FROM kingdom_members
                             WHERE kingdom_id = ?
                             AND player_uuid = ?
                             """
                     )) {

            statement.setInt(1, kingdom.get().getId());
            statement.setString(2, target.toString());

            return statement.executeUpdate() > 0;

        } catch (SQLException exception) {
            logError("kickMember", exception);
            return false;
        }
    }

    // ==========================================
    // PROMOTE / DEMOTE
    // ==========================================

    public synchronized boolean setRole(
            UUID actor,
            UUID target,
            Role newRole
    ) {
        if (actor == null || target == null ||
                actor.equals(target) ||
                newRole == null ||
                newRole == Role.OWNER) {
            return false;
        }

        Optional<KingdomMember> actorMember = getMember(actor);
        Optional<KingdomMember> targetMember = getMember(target);

        if (actorMember.isEmpty() || targetMember.isEmpty()) {
            return false;
        }

        KingdomMember actorData = actorMember.get();
        KingdomMember targetData = targetMember.get();

        if (actorData.getKingdomId() != targetData.getKingdomId()) {
            return false;
        }

        Role actorRole = actorData.getRole();
        Role targetRole = targetData.getRole();

        if (actorRole != Role.OWNER &&
                actorRole != Role.LEADER) {
            return false;
        }

        if (targetRole == Role.OWNER ||
                targetRole == Role.LEADER) {
            return false;
        }

        // Leader hanya boleh mengatur MEMBER menjadi OFFICER.
        // Hanya OWNER yang dapat memberikan jabatan LEADER.
        if (actorRole == Role.LEADER) {
            if (targetRole != Role.MEMBER ||
                    newRole != Role.OFFICER) {
                return false;
            }
        }

        try (PreparedStatement statement =
                     database.getConnection().prepareStatement(
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

    public synchronized boolean promoteMember(
            UUID actor,
            UUID target
    ) {
        Optional<KingdomMember> member = getMember(target);

        if (member.isEmpty()) {
            return false;
        }

        Optional<KingdomMember> actorMember = getMember(actor);

        if (actorMember.isEmpty()) {
            return false;
        }

        Role next;

        if (actorMember.get().getRole() == Role.LEADER) {
            next = member.get().getRole() == Role.MEMBER
                    ? Role.OFFICER : null;
        } else {
            next = switch (member.get().getRole()) {
                case MEMBER -> Role.OFFICER;
                case OFFICER -> Role.LEADER;
                default -> null;
            };
        }

        return next != null && setRole(actor, target, next);
    }

    public synchronized boolean demoteMember(
            UUID actor,
            UUID target
    ) {
        Optional<KingdomMember> member = getMember(target);

        if (member.isEmpty()) {
            return false;
        }

        Role next = switch (member.get().getRole()) {
            case LEADER -> Role.OFFICER;
            case OFFICER -> Role.MEMBER;
            default -> null;
        };

        return next != null && setRole(actor, target, next);
    }

    // ==========================================
    // TRANSFER OWNERSHIP
    // ==========================================

    public synchronized boolean transferOwnership(
            UUID owner,
            UUID target
    ) {
        if (owner == null || target == null ||
                owner.equals(target)) {
            return false;
        }

        Optional<Kingdom> optional = getKingdomByPlayer(owner);

        if (optional.isEmpty()) {
            return false;
        }

        Kingdom kingdom = optional.get();

        if (!kingdom.getOwnerUuid().equals(owner.toString())) {
            return false;
        }

        Optional<KingdomMember> targetMember = getMember(target);

        if (targetMember.isEmpty() ||
                targetMember.get().getKingdomId() != kingdom.getId()) {
            return false;
        }

        Connection connection = database.getConnection();

        try {
            connection.setAutoCommit(false);

            try {
                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     """
                                     UPDATE kingdom_members
                                     SET role = 'LEADER'
                                     WHERE kingdom_id = ?
                                     AND player_uuid = ?
                                     """
                             )) {

                    statement.setInt(1, kingdom.getId());
                    statement.setString(2, owner.toString());

                    if (statement.executeUpdate() <= 0) {
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

                    if (statement.executeUpdate() <= 0) {
                        throw new SQLException(
                                "Target member could not become owner."
                        );
                    }
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     """
                                     UPDATE kingdoms
                                     SET owner_uuid = ?, updated_at = ?
                                     WHERE id = ?
                                     """
                             )) {

                    statement.setString(1, target.toString());
                    statement.setLong(2, System.currentTimeMillis());
                    statement.setInt(3, kingdom.getId());

                    if (statement.executeUpdate() <= 0) {
                        throw new SQLException(
                                "Kingdom owner could not be updated."
                        );
                    }
                }

                connection.commit();
                return true;

            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }

        } catch (Exception exception) {
            logError("transferOwnership", exception);
            return false;

        } finally {
            restoreAutoCommit(connection);
        }
    }

    // ==========================================
    // LEAVE / DELETE
    // ==========================================

    public synchronized boolean leaveKingdom(UUID player) {
        Optional<KingdomMember> optional = getMember(player);

        if (optional.isEmpty() || optional.get().isOwner()) {
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

    // ==========================================
    // ROLE PERMISSION
    // ==========================================

    private boolean canManageRole(Role actor, Role target) {
        return switch (actor) {
            case OWNER -> target != Role.OWNER;
            case LEADER ->
                    target == Role.OFFICER || target == Role.MEMBER;
            case OFFICER -> target == Role.MEMBER;
            default -> false;
        };
    }

    // ==========================================
    // INTERNAL HELPERS
    // ==========================================

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

    private void restoreAutoCommit(Connection connection) {
        try {
            if (connection != null && !connection.isClosed() &&
                    !connection.getAutoCommit()) {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            logError("restoreAutoCommit", exception);
        }
    }

    private void logError(String method, Exception exception) {
        plugin.getLogger().log(
                java.util.logging.Level.SEVERE,
                "KingdomManager." + method + " failed.",
                exception
        );
    }

    // ==========================================
    // INVITATION DATA
    // ==========================================

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
