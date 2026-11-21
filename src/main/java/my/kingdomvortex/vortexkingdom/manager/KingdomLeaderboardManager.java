
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
                return valueOf(value.trim().toUpperCase());
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

    public List<LeaderboardEntry> getTop(
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
                    COUNT(km.player_uuid) AS member_count
                FROM kingdoms k
                LEFT JOIN kingdom_members km
                    ON km.kingdom_id = k.id
                GROUP BY
                    k.id,
                    k.name,
                    k.owner_uuid,
                    k.level,
                    k.total_experience,
                    k.total_kills
                ORDER BY %s DESC,
                         k.name COLLATE NOCASE ASC,
                         k.id ASC
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

    public List<LeaderboardEntry> getTopLevel(int limit) {
        return getTop(LeaderboardType.LEVEL, limit);
    }

    public List<LeaderboardEntry> getTopExperience(int limit) {
        return getTop(LeaderboardType.EXPERIENCE, limit);
    }

    public List<LeaderboardEntry> getTopMembers(int limit) {
        return getTop(LeaderboardType.MEMBERS, limit);
    }

    public List<LeaderboardEntry> getTopKills(int limit) {
        return getTop(LeaderboardType.KILLS, limit);
    }

    public int getRank(
            int kingdomId,
            LeaderboardType type
    ) {
        if (kingdomId <= 0) {
            return 0;
        }

        if (type == null) {
            type = LeaderboardType.LEVEL;
        }

        String scoreExpression = switch (type) {
            case LEVEL -> "current.level";
            case EXPERIENCE -> "current.total_experience";
            case MEMBERS -> """
                    (
                        SELECT COUNT(*)
                        FROM kingdom_members current_members
                        WHERE current_members.kingdom_id = current.id
                    )
                    """;
            case KILLS -> "current.total_kills";
        };

        String otherScoreExpression = switch (type) {
            case LEVEL -> "other.level";
            case EXPERIENCE -> "other.total_experience";
            case MEMBERS -> """
                    (
                        SELECT COUNT(*)
                        FROM kingdom_members other_members
                        WHERE other_members.kingdom_id = other.id
                    )
                    """;
            case KILLS -> "other.total_kills";
        };

        String sql = """
                SELECT
                    (
                        SELECT COUNT(*) + 1
                        FROM kingdoms other
                        WHERE
                            %s > %s
                            OR (
                                %s = %s
                                AND (
                                    other.name COLLATE NOCASE
                                        < current.name COLLATE NOCASE
                                    OR (
                                        other.name COLLATE NOCASE
                                            = current.name COLLATE NOCASE
                                        AND other.id < current.id
                                    )
                                )
                            )
                    ) AS kingdom_rank
                FROM kingdoms current
                WHERE current.id = ?
                """.formatted(
                otherScoreExpression,
                scoreExpression,
                otherScoreExpression,
                scoreExpression
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

    public LeaderboardEntry getKingdomEntry(
            int kingdomId,
            LeaderboardType type
    ) {
        if (kingdomId <= 0) {
            return null;
        }

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
                    COUNT(km.player_uuid) AS member_count
                FROM kingdoms k
                LEFT JOIN kingdom_members km
                    ON km.kingdom_id = k.id
                WHERE k.id = ?
                GROUP BY
                    k.id,
                    k.name,
                    k.owner_uuid,
                    k.level,
                    k.total_experience,
                    k.total_kills
                """;

        try {
            Connection connection =
                    plugin.getDatabaseManager().getConnection();

            synchronized (connection) {
                LeaderboardEntry entry;

                try (PreparedStatement statement =
                             connection.prepareStatement(sql)) {

                    statement.setInt(1, kingdomId);

                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            return null;
                        }

                        entry = mapEntry(result, type, 0);
                    }
                }

                int rank = getRank(kingdomId, type);

                return new LeaderboardEntry(
                        entry.kingdomId(),
                        entry.kingdomName(),
                        entry.ownerUuid(),
                        entry.level(),
                        entry.totalExperience(),
                        entry.totalKills(),
                        entry.memberCount(),
                        entry.score(),
                        rank
                );
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
