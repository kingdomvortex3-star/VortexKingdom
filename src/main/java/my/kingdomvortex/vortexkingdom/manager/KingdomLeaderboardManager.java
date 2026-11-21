
package my.kingdomvortex.vortexkingdom.manager;

import my.kingdomvortex.vortexkingdom.VortexKingdom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class KingdomLeaderboardManager {

    private final VortexKingdom plugin;

    public KingdomLeaderboardManager(VortexKingdom plugin) {
        this.plugin = plugin;
    }

    public enum LeaderboardType {

        LEVEL("level"),
        EXPERIENCE("total_experience"),
        MEMBERS("member_count"),
        KILLS("total_kills");

        private final String column;

        LeaderboardType(String column) {
            this.column = column;
        }

        public String getColumn() {
            return column;
        }

        public static LeaderboardType fromString(String value) {
            if (value == null) {
                return LEVEL;
            }

            try {
                return valueOf(value.toUpperCase());
            } catch (IllegalArgumentException exception) {
                return LEVEL;
            }
        }
    }

    public record LeaderboardEntry(
            int kingdomId,
            String kingdomName,
            String ownerUuid,
            int level,
            long totalExperience,
            long totalKills,
            int memberCount,
            long score,
            int position
    ) {
    }

    /**
     * Mengambil leaderboard berdasarkan kategori.
     */
    public synchronized List<LeaderboardEntry> getTop(
            LeaderboardType type,
            int limit
    ) {
        if (type == null) {
            type = LeaderboardType.LEVEL;
        }

        limit = Math.max(1, Math.min(limit, 100));

        String sql = """
                SELECT
                    k.id,
                    k.name,
                    k.owner_uuid,
                    k.level,
                    k.total_experience,
                    k.total_kills,
                    (
                        SELECT COUNT(*)
                        FROM kingdom_members km
                        WHERE km.kingdom_id = k.id
                    ) AS member_count
                FROM kingdoms k
                ORDER BY %s DESC, k.name ASC
                LIMIT ?
                """.formatted(type.getColumn());

        List<LeaderboardEntry> entries = new ArrayList<>();

        try {
            Connection connection =
                    plugin.getDatabaseManager().getConnection();

            synchronized (connection) {
                try (PreparedStatement statement =
                             connection.prepareStatement(sql)) {

                    statement.setInt(1, limit);

                    try (ResultSet result = statement.executeQuery()) {
                        int position = 1;

                        while (result.next()) {
                            entries.add(mapEntry(
                                    result,
                                    type,
                                    position++
                            ));
                        }
                    }
                }
            }

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Failed to load Kingdom leaderboard: "
                            + exception.getMessage()
            );
        }

        return entries;
    }

    /**
     * Mengambil leaderboard Level.
     */
    public List<LeaderboardEntry> getTopLevel(int limit) {
        return getTop(LeaderboardType.LEVEL, limit);
    }

    /**
     * Mengambil leaderboard Total EXP.
     */
    public List<LeaderboardEntry> getTopExperience(int limit) {
        return getTop(LeaderboardType.EXPERIENCE, limit);
    }

    /**
     * Mengambil leaderboard Jumlah Member.
     */
    public List<LeaderboardEntry> getTopMembers(int limit) {
        return getTop(LeaderboardType.MEMBERS, limit);
    }

    /**
     * Mengambil leaderboard Total Kills.
     */
    public List<LeaderboardEntry> getTopKills(int limit) {
        return getTop(LeaderboardType.KILLS, limit);
    }

    /**
     * Mendapatkan posisi Kingdom pada kategori tertentu.
     * Mengembalikan 0 jika Kingdom tidak ditemukan.
     */
    public synchronized int getRank(
            int kingdomId,
            LeaderboardType type
    ) {
        if (type == null) {
            type = LeaderboardType.LEVEL;
        }

        String sql = """
                SELECT
                    current.id,
                    (
                        SELECT COUNT(*) + 1
                        FROM kingdoms other
                        WHERE
                            other.%s > current.%s
                            OR (
                                other.%s = current.%s
                                AND other.name < current.name
                            )
                    ) AS kingdom_rank
                FROM kingdoms current
                WHERE current.id = ?
                """.formatted(
                type.getColumn(),
                type.getColumn(),
                type.getColumn(),
                type.getColumn()
        );

        try {
            Connection connection =
                    plugin.getDatabaseManager().getConnection();

            synchronized (connection) {
                try (PreparedStatement statement =
                             connection.prepareStatement(sql)) {

                    statement.setInt(1, kingdomId);

                    try (ResultSet result = statement.executeQuery()) {
                        return result.next()
                                ? result.getInt("kingdom_rank")
                                : 0;
                    }
                }
            }

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Failed to get Kingdom rank: "
                            + exception.getMessage()
            );
            return 0;
        }
    }

    /**
     * Mendapatkan data Kingdom pada leaderboard.
     */
    public synchronized LeaderboardEntry getKingdomEntry(
            int kingdomId,
            LeaderboardType type
    ) {
        if (type == null) {
            type = LeaderboardType.LEVEL;
        }

        String sql = """
                SELECT
                    k.id,
                    k.name,
                    k.owner_uuid,
                    k.level,
                    k.total_experience,
                    k.total_kills,
                    (
                        SELECT COUNT(*)
                        FROM kingdom_members km
                        WHERE km.kingdom_id = k.id
                    ) AS member_count
                FROM kingdoms k
                WHERE k.id = ?
                """;

        try {
            Connection connection =
                    plugin.getDatabaseManager().getConnection();

            synchronized (connection) {
                try (PreparedStatement statement =
                             connection.prepareStatement(sql)) {

                    statement.setInt(1, kingdomId);

                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            return null;
                        }

                        return mapEntry(
                                result,
                                type,
                                getRank(kingdomId, type)
                        );
                    }
                }
            }

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Failed to get Kingdom leaderboard entry: "
                            + exception.getMessage()
            );
            return null;
        }
    }

    private LeaderboardEntry mapEntry(
            ResultSet result,
            LeaderboardType type,
            int position
    ) throws SQLException {

        int id = result.getInt("id");
        String name = result.getString("name");
        String ownerUuid = result.getString("owner_uuid");

        int level = result.getInt("level");
        long totalExperience = result.getLong("total_experience");
        long totalKills = result.getLong("total_kills");
        int memberCount = result.getInt("member_count");

        long score = switch (type) {
            case LEVEL -> level;
            case EXPERIENCE -> totalExperience;
            case MEMBERS -> memberCount;
            case KILLS -> totalKills;
        };

        return new LeaderboardEntry(
                id,
                name,
                ownerUuid,
                level,
                totalExperience,
                totalKills,
                memberCount,
                score,
                position
        );
    }
}
